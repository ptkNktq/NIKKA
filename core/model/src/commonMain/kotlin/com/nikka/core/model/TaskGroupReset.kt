package com.nikka.core.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

private const val DAYS_IN_WEEK = 7

/** 日次リセットが到来しているのにまだ実施されていなければ true */
fun TaskGroup.isDailyResetPending(today: LocalDate, currentHour: Int): Boolean {
    val hour = resetHour ?: return false
    return currentHour >= hour && lastResetDate != today
}

/**
 * 直近に到来した週次リセット予定日を返す。リセット曜日が未設定なら null。
 * リセット時刻は日課リセット時刻 (resetHour) を流用し、未設定なら 0 時とする。
 */
fun TaskGroup.latestWeeklyResetDate(today: LocalDate, currentHour: Int): LocalDate? {
    val isoDay = resetDayOfWeek ?: return null
    val hour = resetHour ?: 0
    val daysSinceResetDay = (today.dayOfWeek.isoDayNumber - isoDay + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val candidate = today.minus(daysSinceResetDay, DateTimeUnit.DAY)
    // リセット曜日当日でまだ時刻前なら、1 週間前が直近のリセット予定日
    return if (daysSinceResetDay == 0 && currentHour < hour) {
        candidate.minus(DAYS_IN_WEEK, DateTimeUnit.DAY)
    } else {
        candidate
    }
}

/** 週次リセットが到来しているのにまだ実施されていなければ、その予定日を返す */
fun TaskGroup.pendingWeeklyResetDate(today: LocalDate, currentHour: Int): LocalDate? {
    val latest = latestWeeklyResetDate(today, currentHour) ?: return null
    val last = lastWeeklyResetDate
    return if (last == null || last < latest) latest else null
}
