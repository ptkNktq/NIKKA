package com.nikka.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikka.core.data.TaskRepository
import com.nikka.core.model.AppSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    private val repository: TaskRepository,
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = repository.appSettings

    fun setCollapseOnDailyCompleted(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAppSettings(
                repository.appSettings.value.copy(collapseOnDailyCompleted = enabled),
            )
        }
    }
}
