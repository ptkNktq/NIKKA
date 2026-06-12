package com.nikka.core.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

private const val DAYS_IN_WEEK = 7

/** 直近に到来した日次リセット予定日を返す (当日のリセット時刻前なら前日) */
fun TaskGroup.latestDailyResetDate(today: LocalDate, currentHour: Int): LocalDate =
    if (currentHour >= resetHour) today else today.minus(1, DateTimeUnit.DAY)

/** 日次リセットが到来しているのにまだ実施されていなければ true */
fun TaskGroup.isDailyResetPending(today: LocalDate, currentHour: Int): Boolean =
    currentHour >= resetHour && lastResetDate != today

/** 週課の実効リセット時刻 (「日課と同じ時刻」設定なら日課リセット時刻) */
fun TaskGroup.effectiveWeeklyResetHour(): Int = weeklyResetHour ?: resetHour

/** 直近に到来した週次リセット予定日を返す */
fun TaskGroup.latestWeeklyResetDate(today: LocalDate, currentHour: Int): LocalDate {
    val daysSinceResetDay = (today.dayOfWeek.isoDayNumber - resetDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val candidate = today.minus(daysSinceResetDay, DateTimeUnit.DAY)
    // リセット曜日当日でまだ時刻前なら、1 週間前が直近のリセット予定日
    return if (daysSinceResetDay == 0 && currentHour < effectiveWeeklyResetHour()) {
        candidate.minus(DAYS_IN_WEEK, DateTimeUnit.DAY)
    } else {
        candidate
    }
}

/** 週次リセットが到来しているのにまだ実施されていなければ、その予定日を返す */
fun TaskGroup.pendingWeeklyResetDate(today: LocalDate, currentHour: Int): LocalDate? {
    val latest = latestWeeklyResetDate(today, currentHour)
    val last = lastWeeklyResetDate
    return if (last == null || last < latest) latest else null
}

/** 直近の日次リセット予定日を実施済み扱いにする。実施済みの日付は巻き戻さない */
fun TaskGroup.withDailyResetBaseline(today: LocalDate, currentHour: Int): TaskGroup {
    val baseline = latestDailyResetDate(today, currentHour)
    return copy(lastResetDate = maxOf(lastResetDate ?: baseline, baseline))
}

/** 直近の週次リセット予定日を実施済み扱いにする。実施済みの日付は巻き戻さない */
fun TaskGroup.withWeeklyResetBaseline(today: LocalDate, currentHour: Int): TaskGroup {
    val baseline = latestWeeklyResetDate(today, currentHour)
    return copy(lastWeeklyResetDate = maxOf(lastWeeklyResetDate ?: baseline, baseline))
}
