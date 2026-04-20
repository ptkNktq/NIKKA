package com.nikka.core.data

import com.nikka.core.model.NotificationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 指定時刻に日課の未達成を Discord Webhook で通知するスケジューラ。
 *
 * - アプリ起動中のみ動作する。
 * - 1 日 1 回まで通知する (当日の通知済みフラグは Repository に永続化)。
 * - 未達成タスクが 0 件の場合は送信せず、通知済みフラグだけ立てる (当日再度増えても再送しない)。
 * - 当日の通知時刻を過ぎてから設定を ON にした場合、今日分が未送信なら即時送信する。
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
    private val logger: Logger = Logger.getLogger(NotificationScheduler::class.java.name)

    fun start() {
        scope.launch { restart() }
    }

    /** ループのみ停止する。scope 自体は破棄しないため、start / onSettingsChanged で再開可能。 */
    fun stop() {
        scope.launch {
            mutex.withLock {
                loopJob?.cancel()
                loopJob = null
            }
        }
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
            val settings = repository.notificationSettings.value
            if (!settings.enabled || settings.webhookUrl.isBlank()) return
            val waitMs = computeWaitMillis(settings.hour)
            if (waitMs > 0) delay(waitMs)
            try {
                fireIfNeeded(
                    webhookUrl = settings.webhookUrl,
                    message = settings.message ?: NotificationSettings.DEFAULT_MESSAGE,
                )
            } catch (e: Exception) {
                // 通知失敗は次サイクルで再試行。原因切り分けのためログ出力のみ行う。
                logger.log(Level.WARNING, "Failed to send scheduled notification", e)
            }
            // 翌日まで少し進めてから再計算 (同じ時刻にヒットし続けるのを防ぐ)
            delay(POST_FIRE_COOLDOWN_MS)
        }
    }

    private suspend fun computeWaitMillis(hour: Int): Long {
        val safeHour = hour.coerceIn(0, MAX_HOUR)
        val nowInstant = clock.now()
        val now = nowInstant.toLocalDateTime(timeZone)
        val todayTarget = LocalDateTime(now.date, LocalTime(safeHour, 0)).toInstant(timeZone)
        val target = when {
            todayTarget > nowInstant -> todayTarget
            // 当日 hour:00 を過ぎていて未送信なら即時発火 (hour 超え後に ON にしたケース)
            repository.loadLastNotifiedDate() != now.date -> nowInstant
            else -> LocalDateTime(now.date.plus(1, DateTimeUnit.DAY), LocalTime(safeHour, 0))
                .toInstant(timeZone)
        }
        return (target - nowInstant).inWholeMilliseconds.coerceAtLeast(0)
    }

    private suspend fun fireIfNeeded(webhookUrl: String, message: String) {
        val now = clock.now().toLocalDateTime(timeZone)
        val today = now.date
        if (repository.loadLastNotifiedDate() == today) return
        if (!hasUncompletedTasks(now.hour, today)) {
            repository.saveLastNotifiedDate(today)
            return
        }
        val result = webhookClient.send(webhookUrl, message)
        if (result.isSuccess) {
            repository.saveLastNotifiedDate(today)
        }
    }

    /**
     * HomeViewModel が動いていない間に resetHour が到達した場合、[com.nikka.core.model.DailyTask.isCompleted]
     * は前日のまま = true のことがある。そのようなグループのタスクは「未完了扱い」で判定する。
     */
    private suspend fun hasUncompletedTasks(currentHour: Int, today: kotlinx.datetime.LocalDate): Boolean {
        val groups = repository.loadGroups()
        val tasks = repository.loadTasks()
        val pendingResetGroupIds = groups.filter { group ->
            val hour = group.resetHour ?: return@filter false
            currentHour >= hour && group.lastResetDate != today
        }.map { it.id }.toSet()
        return tasks.any { task -> !task.isCompleted || task.groupId in pendingResetGroupIds }
    }

    companion object {
        private const val POST_FIRE_COOLDOWN_MS = 60_000L
        private const val MAX_HOUR = 23
    }
}
