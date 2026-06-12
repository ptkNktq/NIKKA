package com.nikka.core.data

import com.nikka.core.model.NotificationSettings
import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import com.nikka.core.model.isDailyResetPending
import com.nikka.core.model.pendingWeeklyResetDate
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
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 指定時刻に日課・週課の未達成を Discord Webhook で通知するスケジューラ。
 *
 * - アプリ起動中のみ動作する。
 * - 日課: 未達成があれば毎日通知する。
 * - 週課: 翌日が週課リセット曜日のグループに未達成があれば、リセット前日の同時刻に通知する。
 * - それぞれ 1 日 1 回まで通知する (当日の通知済みフラグは Repository に個別に永続化)。
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
            if (!settings.enabled || settings.webhookUrl.isBlank()) {
                // Repository 変更は VM 経由の onSettingsChanged() で loopJob が cancel → 再起動される。
                // その到来まで長めに待機しつつ、万一の漏れも拾えるよう定期的に再チェックする。
                delay(IDLE_CHECK_MS)
                continue
            }
            val waitMs = computeWaitMillis(settings.hour)
            if (waitMs > 0) delay(waitMs)
            val fired = try {
                fireIfNeeded(settings)
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
                repository.saveLastWeeklyNotifiedDate(today)
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
            // 当日 hour:00 を過ぎていて日課・週課いずれかが未送信なら即時発火 (hour 超え後に ON にしたケース)
            repository.loadLastNotifiedDate() != now.date ||
                repository.loadLastWeeklyNotifiedDate() != now.date -> nowInstant
            else -> LocalDateTime(now.date.plus(1, DateTimeUnit.DAY), LocalTime(safeHour, 0))
                .toInstant(timeZone)
        }
        return (target - nowInstant).inWholeMilliseconds.coerceAtLeast(0)
    }

    private suspend fun fireIfNeeded(settings: NotificationSettings): Boolean {
        val now = clock.now().toLocalDateTime(timeZone)
        // 片方が失敗してももう片方は送信を試み、未完了の方だけ次サイクルで再試行する
        val dailySent = fireDailyIfNeeded(settings, now.hour, now.date)
        val weeklySent = fireWeeklyIfNeeded(settings, now.hour, now.date)
        return dailySent && weeklySent
    }

    private suspend fun fireDailyIfNeeded(
        settings: NotificationSettings,
        currentHour: Int,
        today: LocalDate,
    ): Boolean {
        if (repository.loadLastNotifiedDate() == today) return true
        val titles = uncompletedDailyTaskTitles(
            groups = repository.loadGroups(),
            tasks = repository.loadTasks(),
            currentHour = currentHour,
            today = today,
        )
        val sent = if (titles.isNotEmpty()) {
            val message = buildReminderMessage(settings.messagePrefix, DAILY_REMINDER_HEADER, titles)
            webhookClient.send(settings.webhookUrl, message).isSuccess
        } else {
            // 未達成 0 件の日は送らない。当日再度増えても再通知しない仕様なのでフラグだけ立てる
            true
        }
        if (sent) repository.saveLastNotifiedDate(today)
        return sent
    }

    private suspend fun fireWeeklyIfNeeded(
        settings: NotificationSettings,
        currentHour: Int,
        today: LocalDate,
    ): Boolean {
        if (repository.loadLastWeeklyNotifiedDate() == today) return true
        val titles = uncompletedWeeklyTaskTitles(
            groups = repository.loadGroups(),
            tasks = repository.loadTasks(),
            currentHour = currentHour,
            today = today,
        )
        val sent = if (titles.isNotEmpty()) {
            val message = buildReminderMessage(settings.messagePrefix, WEEKLY_REMINDER_HEADER, titles)
            webhookClient.send(settings.webhookUrl, message).isSuccess
        } else {
            // 通知対象 0 件の日は送らない。フラグだけ立てて当日の再評価を抑止する
            true
        }
        if (sent) repository.saveLastWeeklyNotifiedDate(today)
        return sent
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
        private const val IDLE_CHECK_MS = 30 * 60_000L

        // しきい値
        private const val MAX_HOUR = 23

        // 失敗 1→2→3→4 の累計で 5+15+15+15=50 分粘ってから当日を諦めて翌日の通知時刻を待つ
        private const val MAX_FAILURE_RETRIES = 4

        // 通知メッセージの固定テンプレート見出し
        private const val DAILY_REMINDER_HEADER = "以下の日課が未達成です。"
        private const val WEEKLY_REMINDER_HEADER = "以下の週課が未達成です。"
    }
}

/**
 * 通知メッセージを組み立てる。
 *
 * ```
 * {prefix}
 * {header}
 * - タスク1
 * - タスク2
 * ```
 *
 * prefix が null / 空白のみの場合は省略する。
 */
internal fun buildReminderMessage(prefix: String?, header: String, titles: List<String>): String =
    buildString {
        if (!prefix.isNullOrBlank()) appendLine(prefix)
        append(header)
        titles.forEach { append("\n- ").append(it) }
    }

/**
 * 日課リマインダー対象の未達成タスク名を表示順で返す。週課は対象外。空なら通知不要。
 *
 * HomeViewModel が動いていない間に resetHour が到達した場合、[Task.isCompleted]
 * は前日のまま = true のことがある。そのようなグループの日課は「未完了扱い」で判定する。
 */
internal fun uncompletedDailyTaskTitles(
    groups: List<TaskGroup>,
    tasks: List<Task>,
    currentHour: Int,
    today: LocalDate,
): List<String> {
    val pendingResetGroupIds = groups
        .filter { it.isDailyResetPending(today, currentHour) }
        .map { it.id }
        .toSet()
    return tasks
        .filter { task ->
            task.type == TaskType.DAILY && (!task.isCompleted || task.groupId in pendingResetGroupIds)
        }
        .map { it.title }
}

/**
 * 週課リマインダー対象の未達成タスク名を表示順で返す。空なら通知不要。
 * 対象は「翌日が週課リセット曜日」のグループの週課のみ (リセットで消える前日に知らせる)。
 * 週次リセットが未実施のグループは完了フラグが前週のままの可能性があるため「未完了扱い」で判定する。
 */
internal fun uncompletedWeeklyTaskTitles(
    groups: List<TaskGroup>,
    tasks: List<Task>,
    currentHour: Int,
    today: LocalDate,
): List<String> {
    val targetGroupIds = mutableSetOf<String>()
    val pendingResetGroupIds = mutableSetOf<String>()
    val tomorrowIsoDay = today.plus(1, DateTimeUnit.DAY).dayOfWeek.isoDayNumber
    groups.filter { it.resetDayOfWeek == tomorrowIsoDay }.forEach { group ->
        targetGroupIds += group.id
        if (group.pendingWeeklyResetDate(today, currentHour) != null) pendingResetGroupIds += group.id
    }
    return tasks
        .filter { task ->
            task.type == TaskType.WEEKLY &&
                task.groupId in targetGroupIds &&
                (!task.isCompleted || task.groupId in pendingResetGroupIds)
        }
        .map { it.title }
}
