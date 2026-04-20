package com.nikka.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikka.core.data.NotificationScheduler
import com.nikka.core.data.TaskRepository
import com.nikka.core.model.NotificationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val isLoading: Boolean = true,
    val isHourDialogVisible: Boolean = false,
)

class SettingsViewModel(
    private val repository: TaskRepository,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repository.loadNotificationSettings()
            _uiState.update { it.copy(settings = loaded, isLoading = false) }
        }
    }

    fun setEnabled(enabled: Boolean) = updateAndPersist { it.copy(enabled = enabled) }

    fun setWebhookUrl(url: String) = updateAndPersist { it.copy(webhookUrl = url) }

    fun setMessage(message: String) = updateAndPersist { it.copy(message = message) }

    fun setHour(hour: Int) = updateAndPersist { it.copy(hour = hour) }

    fun showHourDialog() {
        _uiState.update { it.copy(isHourDialogVisible = true) }
    }

    fun dismissHourDialog() {
        _uiState.update { it.copy(isHourDialogVisible = false) }
    }

    private fun updateAndPersist(block: (NotificationSettings) -> NotificationSettings) {
        val next = block(_uiState.value.settings)
        _uiState.update { it.copy(settings = next) }
        viewModelScope.launch {
            repository.saveNotificationSettings(next)
            scheduler.onSettingsChanged()
        }
    }
}
