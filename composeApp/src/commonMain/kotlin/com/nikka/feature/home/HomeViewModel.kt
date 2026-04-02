package com.nikka.feature.home

import androidx.lifecycle.ViewModel
import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class HomeUiState(
    val groups: List<TaskGroup> = emptyList(),
    val tasks: List<DailyTask> = emptyList(),
    val isAddGroupDialogVisible: Boolean = false,
    val isAddTaskDialogVisible: Boolean = false,
    val addTaskTargetGroupId: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
    }

    fun removeGroup(groupId: String) {
        _uiState.update { state ->
            state.copy(
                groups = state.groups.filter { it.id != groupId },
                tasks = state.tasks.filter { it.groupId != groupId },
            )
        }
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
    }

    fun removeTask(taskId: String) {
        _uiState.update { state ->
            state.copy(tasks = state.tasks.filter { it.id != taskId })
        }
    }

    fun toggleTask(taskId: String) {
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
                },
            )
        }
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

    fun dismissAddTaskDialog() {
        _uiState.update { it.copy(isAddTaskDialogVisible = false, addTaskTargetGroupId = null) }
    }

    companion object {
        const val COLOR_PALETTE_SIZE = 7
    }
}
