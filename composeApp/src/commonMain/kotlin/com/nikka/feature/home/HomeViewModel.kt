package com.nikka.feature.home

import androidx.lifecycle.ViewModel
import com.nikka.core.model.DailyTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val tasks: List<DailyTask> = emptyList(),
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun toggleTask(taskId: String) {
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
                },
            )
        }
    }

    fun addTask(title: String) {
        if (title.isBlank()) return
        _uiState.update { state ->
            val newTask = DailyTask(
                id = (state.tasks.size + 1).toString(),
                title = title,
            )
            state.copy(tasks = state.tasks + newTask)
        }
    }
}
