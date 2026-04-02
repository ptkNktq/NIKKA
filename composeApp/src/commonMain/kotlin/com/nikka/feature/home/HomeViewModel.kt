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
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalUuidApi::class)
class HomeViewModel(
    private val repository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val groups = repository.loadGroups()
            val tasks = repository.loadTasks()
            _uiState.update { it.copy(groups = groups, tasks = tasks, isLoading = false) }
        }
    }

    fun addGroup(name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val newGroup = TaskGroup(
                id = Uuid.random().toString(),
                name = name,
                colorIndex = state.groups.size % COLOR_PALETTE_SIZE,
            )
            state.copy(
                groups = state.groups + newGroup,
                isAddGroupDialogVisible = false,
            )
        }
        persistGroups()
    }

    fun removeGroup(groupId: String) {
        _uiState.update { state ->
            state.copy(
                groups = state.groups.filter { it.id != groupId },
                tasks = state.tasks.filter { it.groupId != groupId },
            )
        }
        persistGroups()
        persistTasks()
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
        persistTasks()
    }

    fun removeTask(taskId: String) {
        _uiState.update { state ->
            state.copy(tasks = state.tasks.filter { it.id != taskId })
        }
        persistTasks()
    }

    fun toggleTask(taskId: String) {
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
                },
            )
        }
        persistTasks()
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
            )
        }
        persistTasks()
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
        removeGroup(groupId)
        _uiState.update { it.copy(deleteGroupConfirmId = null) }
    }

    fun dismissDeleteGroupConfirm() {
        _uiState.update { it.copy(deleteGroupConfirmId = null) }
    }

    fun dismissAddTaskDialog() {
        _uiState.update { it.copy(isAddTaskDialogVisible = false, addTaskTargetGroupId = null) }
    }

    private fun persistGroups() {
        viewModelScope.launch { repository.saveGroups(_uiState.value.groups) }
    }

    private fun persistTasks() {
        viewModelScope.launch { repository.saveTasks(_uiState.value.tasks) }
    }

    companion object {
        const val COLOR_PALETTE_SIZE = 7
    }
}
