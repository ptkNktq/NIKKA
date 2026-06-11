package com.nikka.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikka.core.data.TaskRepository
import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class HomeUiState(
    val groups: List<TaskGroup> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val collapsedGroupIds: Set<String> = emptySet(),
    val isAddGroupDialogVisible: Boolean = false,
    val isAddTaskDialogVisible: Boolean = false,
    val addTaskTargetGroupId: String? = null,
    val addTaskTargetType: TaskType = TaskType.DAILY,
    val deleteGroupConfirmId: String? = null,
    val resetHourTargetGroupId: String? = null,
    val resetDayOfWeekTargetGroupId: String? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalUuidApi::class)
class HomeViewModel(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 手動リフレッシュの二重起動を防ぐ
    private val refreshMutex = Mutex()

    init {
        loadData()
    }

    private data class AutoResetResult(
        val groups: List<TaskGroup>,
        val tasks: List<Task>,
        val resetGroupIds: Set<String>,
    )

    private fun loadData() {
        viewModelScope.launch {
            refreshMutex.withLock {
                loadDataLocked()
            }
        }
    }

    private suspend fun loadDataLocked() {
        val rawGroups = repository.loadGroups()
        val rawTasks = repository.loadTasks()
        val result = applyAutoReset(rawGroups, rawTasks)
        val completedGroupIds = result.groups.map { it.id }.filter { groupId ->
            val groupTasks = result.tasks.filter { it.groupId == groupId }
            groupTasks.isNotEmpty() && groupTasks.all { it.isCompleted }
        }.toSet()
        _uiState.update {
            it.copy(
                groups = result.groups,
                tasks = result.tasks,
                collapsedGroupIds = completedGroupIds,
                isLoading = false,
            )
        }
        if (result.resetGroupIds.isNotEmpty()) persistAll()
    }

    fun refreshAutoReset() {
        viewModelScope.launch {
            refreshMutex.withLock {
                val state = _uiState.value
                if (state.isLoading) return@withLock
                val result = applyAutoReset(state.groups, state.tasks)
                if (result.resetGroupIds.isEmpty()) return@withLock
                _uiState.update {
                    it.copy(
                        groups = result.groups,
                        tasks = result.tasks,
                        // リセットで全タスク未完了になったグループは折りたたみを解除する
                        collapsedGroupIds = it.collapsedGroupIds - result.resetGroupIds,
                    )
                }
                persistAll()
            }
        }
    }

    private fun applyAutoReset(
        groups: List<TaskGroup>,
        tasks: List<Task>,
    ): AutoResetResult {
        val now = clock.now().toLocalDateTime(timeZone)
        val today = now.date
        val currentHour = now.hour
        val dailyResetGroupIds = groups.filter { group ->
            val hour = group.resetHour ?: return@filter false
            currentHour >= hour && group.lastResetDate != today
        }.map { it.id }.toSet()
        // 週次リセット: 直近のリセット予定日が到来していて、まだその週の分を実施していないグループ
        val weeklyResetDates = groups.mapNotNull { group ->
            val resetDate = latestWeeklyResetDate(today, currentHour, group) ?: return@mapNotNull null
            val last = group.lastWeeklyResetDate
            if (last == null || last < resetDate) group.id to resetDate else null
        }.toMap()
        if (dailyResetGroupIds.isEmpty() && weeklyResetDates.isEmpty()) {
            return AutoResetResult(groups, tasks, emptySet())
        }
        val newGroups = groups.map { group ->
            var updated = group
            if (group.id in dailyResetGroupIds) updated = updated.copy(lastResetDate = today)
            weeklyResetDates[group.id]?.let { updated = updated.copy(lastWeeklyResetDate = it) }
            updated
        }
        val newTasks = tasks.map { task ->
            val shouldReset = when (task.type) {
                TaskType.DAILY -> task.groupId in dailyResetGroupIds
                TaskType.WEEKLY -> task.groupId in weeklyResetDates
            }
            if (shouldReset) task.copy(isCompleted = false) else task
        }
        return AutoResetResult(newGroups, newTasks, dailyResetGroupIds + weeklyResetDates.keys)
    }

    /**
     * 直近に到来した週次リセット予定日を返す。リセット曜日が未設定なら null。
     * リセット時刻は日課リセット時刻 (resetHour) を流用し、未設定なら 0 時とする。
     */
    private fun latestWeeklyResetDate(
        today: LocalDate,
        currentHour: Int,
        group: TaskGroup,
    ): LocalDate? {
        val isoDay = group.resetDayOfWeek ?: return null
        val resetHour = group.resetHour ?: 0
        val daysSinceResetDay = (today.dayOfWeek.isoDayNumber - isoDay + DAYS_IN_WEEK) % DAYS_IN_WEEK
        val candidate = today.minus(daysSinceResetDay, DateTimeUnit.DAY)
        // リセット曜日当日でまだ時刻前なら、1 週間前が直近のリセット予定日
        return if (daysSinceResetDay == 0 && currentHour < resetHour) {
            candidate.minus(DAYS_IN_WEEK, DateTimeUnit.DAY)
        } else {
            candidate
        }
    }

    fun addGroup(name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val newGroup = TaskGroup(
                id = Uuid.random().toString(),
                name = name,
            )
            state.copy(
                groups = state.groups + newGroup,
                isAddGroupDialogVisible = false,
            )
        }
        persistAll()
    }

    fun removeGroup(groupId: String) {
        _uiState.update { state ->
            state.copy(
                groups = state.groups.filter { it.id != groupId },
                tasks = state.tasks.filter { it.groupId != groupId },
            )
        }
        persistAll()
    }

    fun addTask(groupId: String, title: String, type: TaskType = TaskType.DAILY) {
        if (title.isBlank()) return
        _uiState.update { state ->
            val newTask = Task(
                id = Uuid.random().toString(),
                groupId = groupId,
                title = title,
                type = type,
            )
            state.copy(
                tasks = state.tasks + newTask,
                isAddTaskDialogVisible = false,
                addTaskTargetGroupId = null,
            )
        }
        persistAll()
    }

    fun removeTask(taskId: String) {
        _uiState.update { state ->
            state.copy(tasks = state.tasks.filter { it.id != taskId })
        }
        persistAll()
    }

    fun toggleTask(taskId: String) {
        _uiState.update { state ->
            val newTasks = state.tasks.map { task ->
                if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
            }
            val groupId = state.tasks.find { it.id == taskId }?.groupId
            val autoCollapse = if (groupId != null) {
                val groupTasks = newTasks.filter { it.groupId == groupId }
                groupTasks.isNotEmpty() && groupTasks.all { it.isCompleted }
            } else {
                false
            }
            state.copy(
                tasks = newTasks,
                collapsedGroupIds = if (autoCollapse && groupId != null) {
                    state.collapsedGroupIds + groupId
                } else {
                    state.collapsedGroupIds
                },
            )
        }
        persistAll()
    }

    fun moveGroup(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        _uiState.update { state ->
            state.copy(
                groups = state.groups.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                },
            )
        }
    }

    fun settleDrag() {
        persistAll()
    }

    fun moveTask(groupId: String, type: TaskType, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        _uiState.update { state ->
            // 並べ替えは同一グループ・同一種別のセクション内で完結する
            val sectionTasks = state.tasks
                .filter { it.groupId == groupId && it.type == type }
                .toMutableList()
            if (fromIndex !in sectionTasks.indices || toIndex !in sectionTasks.indices) return@update state
            sectionTasks.add(toIndex, sectionTasks.removeAt(fromIndex))
            val reorderedQueue = ArrayDeque(sectionTasks)
            state.copy(
                tasks = state.tasks.map {
                    if (it.groupId == groupId && it.type == type) reorderedQueue.removeFirst() else it
                },
            )
        }
        persistAll()
    }

    fun toggleGroupCollapse(groupId: String) {
        _uiState.update { state ->
            val newCollapsed = if (groupId in state.collapsedGroupIds) {
                state.collapsedGroupIds - groupId
            } else {
                state.collapsedGroupIds + groupId
            }
            state.copy(collapsedGroupIds = newCollapsed)
        }
    }

    fun resetGroupTasks(groupId: String) {
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.groupId == groupId) task.copy(isCompleted = false) else task
                },
                collapsedGroupIds = state.collapsedGroupIds - groupId,
            )
        }
        persistAll()
    }

    fun showAddGroupDialog() {
        _uiState.update { it.copy(isAddGroupDialogVisible = true) }
    }

    fun dismissAddGroupDialog() {
        _uiState.update { it.copy(isAddGroupDialogVisible = false) }
    }

    fun showAddTaskDialog(groupId: String, type: TaskType = TaskType.DAILY) {
        _uiState.update {
            it.copy(
                isAddTaskDialogVisible = true,
                addTaskTargetGroupId = groupId,
                addTaskTargetType = type,
            )
        }
    }

    fun showDeleteGroupConfirm(groupId: String) {
        _uiState.update { it.copy(deleteGroupConfirmId = groupId) }
    }

    fun confirmDeleteGroup() {
        val groupId = _uiState.value.deleteGroupConfirmId ?: return
        dismissDeleteGroupConfirm()
        removeGroup(groupId)
    }

    fun dismissDeleteGroupConfirm() {
        _uiState.update { it.copy(deleteGroupConfirmId = null) }
    }

    fun dismissAddTaskDialog() {
        _uiState.update { it.copy(isAddTaskDialogVisible = false, addTaskTargetGroupId = null) }
    }

    fun showResetHourDialog(groupId: String) {
        _uiState.update { it.copy(resetHourTargetGroupId = groupId) }
    }

    fun dismissResetHourDialog() {
        _uiState.update { it.copy(resetHourTargetGroupId = null) }
    }

    fun setResetHour(groupId: String, hour: Int?) {
        _uiState.update { state ->
            state.copy(
                groups = state.groups.map { group ->
                    if (group.id == groupId) group.copy(resetHour = hour) else group
                },
                resetHourTargetGroupId = null,
            )
        }
        persistAll()
    }

    fun showResetDayOfWeekDialog(groupId: String) {
        _uiState.update { it.copy(resetDayOfWeekTargetGroupId = groupId) }
    }

    fun dismissResetDayOfWeekDialog() {
        _uiState.update { it.copy(resetDayOfWeekTargetGroupId = null) }
    }

    fun setResetDayOfWeek(groupId: String, isoDayOfWeek: Int?) {
        val now = clock.now().toLocalDateTime(timeZone)
        _uiState.update { state ->
            state.copy(
                groups = state.groups.map { group ->
                    when {
                        group.id != groupId -> group
                        isoDayOfWeek == null -> group.copy(
                            resetDayOfWeek = null,
                            lastWeeklyResetDate = null,
                        )
                        else -> {
                            // 設定時点の週課の進捗を消さないよう、直近のリセット予定日を実施済み扱いにする
                            val configured = group.copy(resetDayOfWeek = isoDayOfWeek)
                            configured.copy(
                                lastWeeklyResetDate = latestWeeklyResetDate(now.date, now.hour, configured),
                            )
                        }
                    }
                },
                resetDayOfWeekTargetGroupId = null,
            )
        }
        persistAll()
    }

    private fun persistAll() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            repository.saveAll(snapshot.groups, snapshot.tasks)
        }
    }

    companion object {
        private const val DAYS_IN_WEEK = 7
    }
}
