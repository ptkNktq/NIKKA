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

/**
 * 直近に到来した週次リセット予定日を返す。
 * リセット時刻は日課リセット時刻 (resetHour) を流用する。
 */
fun TaskGroup.latestWeeklyResetDate(today: LocalDate, currentHour: Int): LocalDate {
    val daysSinceResetDay = (today.dayOfWeek.isoDayNumber - resetDayOfWeek + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val candidate = today.minus(daysSinceResetDay, DateTimeUnit.DAY)
    // リセット曜日当日でまだ時刻前なら、1 週間前が直近のリセット予定日
    return if (daysSinceResetDay == 0 && currentHour < resetHour) {
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
