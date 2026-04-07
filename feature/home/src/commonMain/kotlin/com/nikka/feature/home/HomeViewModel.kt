package com.nikka.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikka.core.data.TaskRepository
import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class HomeUiState(
    val groups: List<TaskGroup> = emptyList(),
    val tasks: List<DailyTask> = emptyList(),
    val collapsedGroupIds: Set<String> = emptySet(),
    val isAddGroupDialogVisible: Boolean = false,
    val isAddTaskDialogVisible: Boolean = false,
    val addTaskTargetGroupId: String? = null,
    val deleteGroupConfirmId: String? = null,
    val resetHourTargetGroupId: String? = null,
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

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val rawGroups = repository.loadGroups()
            val rawTasks = repository.loadTasks()
            val (groups, tasks, didReset) = applyAutoReset(rawGroups, rawTasks)
            val completedGroupIds = groups.map { it.id }.filter { groupId ->
                val groupTasks = tasks.filter { it.groupId == groupId }
                groupTasks.isNotEmpty() && groupTasks.all { it.isCompleted }
            }.toSet()
            _uiState.update {
                it.copy(
                    groups = groups,
                    tasks = tasks,
                    collapsedGroupIds = completedGroupIds,
                    isLoading = false,
                )
            }
            if (didReset) persistAll()
        }
    }

    fun refreshAutoReset() {
        val state = _uiState.value
        if (state.isLoading) return
        val (groups, tasks, didReset) = applyAutoReset(state.groups, state.tasks)
        if (!didReset) return
        // リセットで全タスク未完了になったグループは折りたたみを解除する
        val resetGroupIds = groups.asSequence()
            .filter { it.lastResetDate != state.groups.find { old -> old.id == it.id }?.lastResetDate }
            .map { it.id }
            .toSet()
        _uiState.update {
            it.copy(
                groups = groups,
                tasks = tasks,
                collapsedGroupIds = it.collapsedGroupIds - resetGroupIds,
            )
        }
        persistAll()
    }

    private fun applyAutoReset(
        groups: List<TaskGroup>,
        tasks: List<DailyTask>,
    ): Triple<List<TaskGroup>, List<DailyTask>, Boolean> {
        val now = clock.now().toLocalDateTime(timeZone)
        val today = now.date
        val currentHour = now.hour
        val resetGroupIds = groups.filter { group ->
            val hour = group.resetHour ?: return@filter false
            currentHour >= hour && group.lastResetDate != today
        }.map { it.id }.toSet()
        if (resetGroupIds.isEmpty()) return Triple(groups, tasks, false)
        val newGroups = groups.map { group ->
            if (group.id in resetGroupIds) group.copy(lastResetDate = today) else group
        }
        val newTasks = tasks.map { task ->
            if (task.groupId in resetGroupIds) task.copy(isCompleted = false) else task
        }
        return Triple(newGroups, newTasks, true)
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

    fun addTask(groupId: String, title: String) {
        if (title.isBlank()) return
        _uiState.update { state ->
            val newTask = DailyTask(
                id = Uuid.random().toString(),
                groupId = groupId,
                title = title,
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

    fun moveTask(groupId: String, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        _uiState.update { state ->
            val groupTasks = state.tasks.filter { it.groupId == groupId }.toMutableList()
            if (fromIndex !in groupTasks.indices || toIndex !in groupTasks.indices) return@update state
            groupTasks.add(toIndex, groupTasks.removeAt(fromIndex))
            val reorderedQueue = ArrayDeque(groupTasks)
            state.copy(
                tasks = state.tasks.map {
                    if (it.groupId == groupId) reorderedQueue.removeFirst() else it
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

    fun showAddTaskDialog(groupId: String) {
        _uiState.update { it.copy(isAddTaskDialogVisible = true, addTaskTargetGroupId = groupId) }
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

    private fun persistAll() {
        val snapshot = _uiState.value
        viewModelScope.launch {
            repository.saveAll(snapshot.groups, snapshot.tasks)
        }
    }
}
