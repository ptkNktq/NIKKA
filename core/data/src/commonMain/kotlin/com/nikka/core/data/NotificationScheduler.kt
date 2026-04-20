package com.nikka.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * 指定時刻に日課の未達成を Discord Webhook で通知するスケジューラ。
 *
 * - アプリ起動中のみ動作する。
 * - 1 日 1 回まで通知する (当日の通知済みフラグは Repository に永続化)。
 * - 未達成タスクが 0 件の場合は送信せず、通知済みフラグだけ立てる (当日再度増えても再送しない)。
 */
class NotificationScheduler(
    private val repository: TaskRepository,
    private val webhookClient: DiscordWebhookClient,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var loopJob: Job? = null

    fun start() {
        scope.launch { restart() }
    }

    fun stop() {
        scope.cancel()
    }

    /** 設定変更時に呼び出して再スケジュールする。 */
    fun onSettingsChanged() {
        scope.launch { restart() }
    }

    private suspend fun restart() {
        mutex.withLock {
            loopJob?.cancel()
            loopJob = scope.launch { runLoop() }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runLoop() {
        while (scope.isActive) {
            val settings = repository.loadNotificationSettings()
            if (!settings.enabled || settings.webhookUrl.isBlank()) return
            val waitMs = computeWaitMillis(settings.hour)
            if (waitMs > 0) delay(waitMs)
            try {
                fireIfNeeded(settings.webhookUrl, settings.message)
            } catch (_: Exception) {
                // 通知の失敗はログ抑制。次サイクルで再試行される。
            }
            // 翌日まで少し進めてから再計算 (同じ時刻にヒットし続けるのを防ぐ)
            delay(POST_FIRE_COOLDOWN_MS)
        }
    }

    private fun computeWaitMillis(hour: Int): Long {
        val nowInstant = clock.now()
        val now = nowInstant.toLocalDateTime(timeZone)
        val todayTarget = LocalDateTime(now.date, LocalTime(hour, 0)).toInstant(timeZone)
        val target = if (todayTarget > nowInstant) {
            todayTarget
        } else {
            LocalDateTime(now.date.plus(1, DateTimeUnit.DAY), LocalTime(hour, 0))
                .toInstant(timeZone)
        }
        return (target - nowInstant).inWholeMilliseconds.coerceAtLeast(0)
    }

    private suspend fun fireIfNeeded(webhookUrl: String, message: String) {
        val today = clock.now().toLocalDateTime(timeZone).date
        if (repository.loadLastNotifiedDate() == today) return
        val tasks = repository.loadTasks()
        val hasUncompleted = tasks.any { !it.isCompleted }
        if (!hasUncompleted) {
            repository.saveLastNotifiedDate(today)
            return
        }
        val result = webhookClient.send(webhookUrl, message)
        if (result.isSuccess) {
            repository.saveLastNotifiedDate(today)
        }
    }

    companion object {
        private const val POST_FIRE_COOLDOWN_MS = 60_000L
    }
}
