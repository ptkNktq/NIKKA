package com.nikka.core.data

import com.nikka.core.model.NotificationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
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
 *
 * start / stop / onSettingsChanged は [Channel] で順序を保証してシリアライズする。
 */
class NotificationScheduler(
    private val repository: TaskRepository,
    private val webhookClient: DiscordWebhookClient,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : AutoCloseable {

    private sealed interface Command {
        data object Start : Command
        data object Stop : Command
        data object Restart : Command
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private var loopJob: Job? = null
    private val logger: Logger = Logger.getLogger(NotificationScheduler::class.java.name)

    init {
        scope.launch { processCommands() }
    }

    fun start() = send(Command.Start)

    /** ループのみ停止する。scope 自体は破棄しないため、start / onSettingsChanged で再開可能。 */
    fun stop() = send(Command.Stop)

    /** 設定変更時に呼び出して再スケジュールする。 */
    fun onSettingsChanged() = send(Command.Restart)

    private fun send(command: Command) {
        if (commands.trySend(command).isFailure) {
            // close() 後の呼び出しは設計ミス。本来存在しないが将来の事故防止にログだけ残す
            logger.warning("NotificationScheduler command dropped after close(): $command")
        }
    }

    /** scope / channel ごと完全に解放する。以降 start/stop/onSettingsChanged は無視される。 */
    override fun close() {
        commands.close()
        scope.cancel()
    }

    private suspend fun processCommands() {
        for (cmd in commands) {
            when (cmd) {
                Command.Start, Command.Restart -> {
                    loopJob?.cancel()
                    loopJob = scope.launch { runLoop() }
                }
                Command.Stop -> {
                    loopJob?.cancel()
                    loopJob = null
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runLoop() {
        var failureCount = 0
        while (currentCoroutineContext().isActive) {
            val settings = repository.notificationSettings.value
            if (!settings.enabled || settings.webhookUrl.isBlank()) return
            val waitMs = computeWaitMillis(settings.hour)
            if (waitMs > 0) delay(waitMs)
            val fired = try {
                fireIfNeeded(
                    webhookUrl = settings.webhookUrl,
                    message = settings.message ?: NotificationSettings.DEFAULT_MESSAGE,
                )
            } catch (e: Exception) {
                // 通知失敗は次サイクルで再試行。原因切り分けのためログ出力のみ行う。
                logger.log(Level.WARNING, "Failed to send scheduled notification", e)
                false
            }
            failureCount = if (fired) 0 else failureCount + 1
            if (failureCount >= MAX_FAILURE_RETRIES) {
                // これ以上連続失敗する URL なら当日は諦めて翌日の通知時刻を待つ
                val today = clock.now().toLocalDateTime(timeZone).date
                repository.saveLastNotifiedDate(today)
                failureCount = 0
            }
            delay(failureBackoffMs(failureCount))
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

    private suspend fun fireIfNeeded(webhookUrl: String, message: String): Boolean {
        val now = clock.now().toLocalDateTime(timeZone)
        val today = now.date
        if (repository.loadLastNotifiedDate() == today) return true
        val sent = if (hasUncompletedTasks(now.hour, today)) {
            webhookClient.send(webhookUrl, message).isSuccess
        } else {
            // 未達成 0 件の日は送らない。当日再度増えても再通知しない仕様なのでフラグだけ立てる
            true
        }
        if (sent) repository.saveLastNotifiedDate(today)
        return sent
    }

    /**
     * HomeViewModel が動いていない間に resetHour が到達した場合、[com.nikka.core.model.DailyTask.isCompleted]
     * は前日のまま = true のことがある。そのようなグループのタスクは「未完了扱い」で判定する。
     */
    private suspend fun hasUncompletedTasks(currentHour: Int, today: LocalDate): Boolean {
        val groups = repository.loadGroups()
        val tasks = repository.loadTasks()
        val pendingResetGroupIds = groups.filter { group ->
            val hour = group.resetHour ?: return@filter false
            currentHour >= hour && group.lastResetDate != today
        }.map { it.id }.toSet()
        return tasks.any { task -> !task.isCompleted || task.groupId in pendingResetGroupIds }
    }

    // failureCount は「直前の送信が失敗していた回数」。0 = 直前成功 (初回発火含む)
    private fun failureBackoffMs(failureCount: Int): Long = when (failureCount) {
        0 -> POST_FIRE_COOLDOWN_MS
        1 -> FAILURE_BACKOFF_SHORT_MS
        else -> FAILURE_BACKOFF_LONG_MS
    }

    companion object {
        // 待機間隔 (ms)
        private const val POST_FIRE_COOLDOWN_MS = 60_000L
        private const val FAILURE_BACKOFF_SHORT_MS = 5 * 60_000L
        private const val FAILURE_BACKOFF_LONG_MS = 15 * 60_000L

        // しきい値
        private const val MAX_HOUR = 23

        // 失敗 1→2→3→4 の累計で 5+15+15+15=50 分粘ってから当日を諦めて翌日の通知時刻を待つ
        private const val MAX_FAILURE_RETRIES = 4
    }
}
