package com.nikka.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikka.core.data.TaskRepository
import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import com.nikka.core.model.allTasksCompleted
import com.nikka.core.model.isDailyResetPending
import com.nikka.core.model.latestDailyResetDate
import com.nikka.core.model.pendingWeeklyResetDate
import com.nikka.core.model.withDailyResetBaseline
import com.nikka.core.model.withWeeklyResetBaseline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
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
    val weeklyResetTargetGroupId: String? = null,
    /** 週課が未達成でも日課完了でグループを完了扱い (折りたたみ・ステータス表示) にするか */
    val collapseOnDailyCompleted: Boolean = false,
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
        viewModelScope.launch {
            // 無関係な設定の保存で手動の折りたたみ状態が破棄されないよう、対象フィールドの変化だけ拾う
            repository.appSettings
                .map { it.collapseOnDailyCompleted }
                .distinctUntilChanged()
                .collect { collapseOnDailyCompleted ->
                    _uiState.update { state ->
                        state.copy(
                            collapseOnDailyCompleted = collapseOnDailyCompleted,
                            // 完了判定の基準が変わるため、自動折りたたみ状態も再評価する (ロード完了前は対象なし)
                            collapsedGroupIds = if (state.isLoading) {
                                state.collapsedGroupIds
                            } else {
                                autoCollapsedGroupIds(state.groups, state.tasks, collapseOnDailyCompleted)
                            },
                        )
                    }
                }
        }
    }

    private data class AutoResetResult(
        val groups: List<TaskGroup>,
        val tasks: List<Task>,
        /** タスクが実際に未完了へ戻ったグループ (折りたたみ解除の対象) */
        val resetGroupIds: Set<String>,
        /** グループのリセット実施日かタスクに変化があったか (永続化要否) */
        val hasChanges: Boolean,
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
        val completedGroupIds = autoCollapsedGroupIds(
            groups = result.groups,
            tasks = result.tasks,
            dailyOnly = repository.appSettings.value.collapseOnDailyCompleted,
        )
        _uiState.update {
            it.copy(
                groups = result.groups,
                tasks = result.tasks,
                collapsedGroupIds = completedGroupIds,
                isLoading = false,
            )
        }
        if (result.hasChanges) persistAll()
    }

    fun refreshAutoReset() {
        viewModelScope.launch {
            refreshMutex.withLock {
                val state = _uiState.value
                if (state.isLoading) return@withLock
                val result = applyAutoReset(state.groups, state.tasks)
                if (!result.hasChanges) return@withLock
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
        val dailyResetGroupIds = groups
            .filter { it.isDailyResetPending(today, currentHour) }
            .map { it.id }
            .toSet()
        // 週次リセット: 直近のリセット予定日が到来していて、まだその週の分を実施していないグループ
        val weeklyResetDates = groups.mapNotNull { group ->
            group.pendingWeeklyResetDate(today, currentHour)?.let { group.id to it }
        }.toMap()
        if (dailyResetGroupIds.isEmpty() && weeklyResetDates.isEmpty()) {
            return AutoResetResult(groups, tasks, emptySet(), hasChanges = false)
        }
        val newGroups = groups.map { group ->
            var updated = group
            if (group.id in dailyResetGroupIds) {
                // 当日のリセット時刻前に追い付いた場合に「当日実施済み」と誤記録しないよう、予定日を記録する
                updated = updated.copy(lastResetDate = updated.latestDailyResetDate(today, currentHour))
            }
            weeklyResetDates[group.id]?.let { updated = updated.copy(lastWeeklyResetDate = it) }
            updated
        }
        // 折りたたみ解除はタスクが実際に未完了へ戻ったグループだけを対象にする
        val changedGroupIds = mutableSetOf<String>()
        val newTasks = tasks.map { task ->
            val shouldReset = when (task.type) {
                TaskType.DAILY -> task.groupId in dailyResetGroupIds
                TaskType.WEEKLY -> task.groupId in weeklyResetDates
            }
            if (shouldReset && task.isCompleted) {
                changedGroupIds += task.groupId
                task.copy(isCompleted = false)
            } else {
                task
            }
        }
        return AutoResetResult(newGroups, newTasks, changedGroupIds, hasChanges = true)
    }

    fun addGroup(name: String) {
        if (name.isBlank()) return
        val now = clock.now().toLocalDateTime(timeZone)
        _uiState.update { state ->
            // 作成時点までのリセット予定は実施済み扱いにして、直後の自動リセットを防ぐ
            val newGroup = TaskGroup(
                id = Uuid.random().toString(),
                name = name,
            )
                .withDailyResetBaseline(now.date, now.hour)
                .withWeeklyResetBaseline(now.date, now.hour)
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
            // トグルしたタスクのグループが完了状態になったら自動で折りたたむ
            val collapseGroupId = state.tasks.find { it.id == taskId }?.groupId?.takeIf { groupId ->
                newTasks
                    .filter { it.groupId == groupId }
                    .allTasksCompleted(dailyOnly = repository.appSettings.value.collapseOnDailyCompleted)
            }
            state.copy(
                tasks = newTasks,
                collapsedGroupIds = if (collapseGroupId != null) {
                    state.collapsedGroupIds + collapseGroupId
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

    fun setResetHour(groupId: String, hour: Int) {
        val now = clock.now().toLocalDateTime(timeZone)
        _uiState.update { state ->
            // 変更前の設定で実施待ちのリセットを先に適用してから、新しい設定を反映する。
            // これをしないと、実施待ちだったリセットが「実施済み扱い」で握り潰される
            val caughtUp = applyAutoReset(state.groups, state.tasks)
            state.copy(
                groups = caughtUp.groups.map { group ->
                    if (group.id != groupId) {
                        group
                    } else {
                        // 変更時点までのリセット予定は実施済み扱いにして、当日の進捗を消さない。
                        // 週課が「日課と同じ時刻」設定の場合はリセット時刻が連動するため、週次側も同様に扱う
                        var configured = group.copy(resetHour = hour).withDailyResetBaseline(now.date, now.hour)
                        if (configured.weeklyResetHour == null) {
                            configured = configured.withWeeklyResetBaseline(now.date, now.hour)
                        }
                        configured
                    }
                },
                tasks = caughtUp.tasks,
                collapsedGroupIds = state.collapsedGroupIds - caughtUp.resetGroupIds,
                resetHourTargetGroupId = null,
            )
        }
        persistAll()
    }

    fun showWeeklyResetDialog(groupId: String) {
        _uiState.update { it.copy(weeklyResetTargetGroupId = groupId) }
    }

    fun dismissWeeklyResetDialog() {
        _uiState.update { it.copy(weeklyResetTargetGroupId = null) }
    }

    fun setWeeklyReset(groupId: String, isoDayOfWeek: Int, weeklyResetHour: Int?) {
        val now = clock.now().toLocalDateTime(timeZone)
        _uiState.update { state ->
            // 変更前の設定で実施待ちのリセットを先に適用してから、新しい設定を反映する
            val caughtUp = applyAutoReset(state.groups, state.tasks)
            state.copy(
                groups = caughtUp.groups.map { group ->
                    if (group.id != groupId) {
                        group
                    } else {
                        // 設定時点の週課の進捗を消さないよう、直近のリセット予定日を実施済み扱いにする
                        group.copy(
                            resetDayOfWeek = isoDayOfWeek,
                            weeklyResetHour = weeklyResetHour,
                        ).withWeeklyResetBaseline(now.date, now.hour)
                    }
                },
                tasks = caughtUp.tasks,
                collapsedGroupIds = state.collapsedGroupIds - caughtUp.resetGroupIds,
                weeklyResetTargetGroupId = null,
            )
        }
        persistAll()
    }

    /** 自動折りたたみ対象 (「日課完了で折りたたむ」設定に応じた完了済みグループ) を返す */
    private fun autoCollapsedGroupIds(
        groups: List<TaskGroup>,
        tasks: List<Task>,
        dailyOnly: Boolean,
    ): Set<String> = groups.map { it.id }.filter { groupId ->
        tasks.filter { it.groupId == groupId }.allTasksCompleted(dailyOnly = dailyOnly)
    }.toSet()

    private fun persistAll() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            repository.saveAll(snapshot.groups, snapshot.tasks)
        }
    }
}
