package com.nikka.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikka.core.data.DiscordWebhookClient
import com.nikka.core.data.NotificationScheduler
import com.nikka.core.data.TaskRepository
import com.nikka.core.model.NotificationSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TestSendStatus { Idle, Sending, Success, Failure }

data class NotificationSettingsUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val isHourDialogVisible: Boolean = false,
    val testSendStatus: TestSendStatus = TestSendStatus.Idle,
    val testSendError: String? = null,
)

class NotificationSettingsViewModel(
    private val repository: TaskRepository,
    private val scheduler: NotificationScheduler,
    private val webhookClient: DiscordWebhookClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NotificationSettingsUiState(settings = repository.notificationSettings.value),
    )
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    private var pendingSaveJob: Job? = null

    // 直近で永続化した値。debounce 保存時に scheduler 再起動要否を判定するために保持
    private var lastPersisted: NotificationSettings = repository.notificationSettings.value

    init {
        // Repository の Flow を購読して、外部からの変更にも追随する
        viewModelScope.launch {
            repository.notificationSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                lastPersisted = settings
            }
        }
    }

    fun setEnabled(enabled: Boolean) = updateAndPersist { it.copy(enabled = enabled) }

    fun setWebhookUrl(url: String) = updateAndPersist { it.copy(webhookUrl = url) }

    fun setMessage(message: String) =
        updateAndPersist { it.copy(message = message.ifBlank { null }) }

    fun setHour(hour: Int) = updateAndPersist { it.copy(hour = hour) }

    fun showHourDialog() {
        _uiState.update { it.copy(isHourDialogVisible = true) }
    }

    fun dismissHourDialog() {
        _uiState.update { it.copy(isHourDialogVisible = false) }
    }

    fun sendTestMessage() {
        val current = _uiState.value.settings
        if (current.webhookUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    testSendStatus = TestSendStatus.Failure,
                    testSendError = "Webhook URL が未入力です",
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(testSendStatus = TestSendStatus.Sending, testSendError = null)
            }
            val content = "[NIKKA テスト通知]\n${current.message ?: NotificationSettings.DEFAULT_MESSAGE}"
            val result = webhookClient.send(current.webhookUrl, content)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(testSendStatus = TestSendStatus.Success, testSendError = null)
                } else {
                    it.copy(
                        testSendStatus = TestSendStatus.Failure,
                        testSendError = result.exceptionOrNull()?.message ?: "送信に失敗しました",
                    )
                }
            }
        }
    }

    private fun updateAndPersist(block: (NotificationSettings) -> NotificationSettings) {
        val next = block(_uiState.value.settings)
        _uiState.update { it.copy(settings = next) }
        pendingSaveJob?.cancel()
        pendingSaveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            val prev = lastPersisted
            repository.saveNotificationSettings(next)
            lastPersisted = next
            if (shouldReschedule(prev, next)) scheduler.onSettingsChanged()
        }
    }

    /** scheduler の再計算が必要なフィールドのみを監視対象とする。 */
    private fun shouldReschedule(prev: NotificationSettings, next: NotificationSettings): Boolean =
        prev.enabled != next.enabled ||
            prev.hour != next.hour ||
            prev.webhookUrl != next.webhookUrl

    companion object {
        private const val SAVE_DEBOUNCE_MS = 300L
    }
}
