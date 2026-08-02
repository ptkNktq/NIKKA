package com.nikka.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class TaskGroup(
    val id: String,
    val name: String,
    /** 日課のリセット時刻 (0-23)。週課のリセット時刻にも流用される */
    val resetHour: Int = DEFAULT_RESET_HOUR,
    val lastResetDate: LocalDate? = null,
    /** 週課のリセット曜日 (ISO 8601: 1=月曜 .. 7=日曜) */
    val resetDayOfWeek: Int = DEFAULT_RESET_DAY_OF_WEEK,
    /**
     * 週課のリセット時刻 (0-23)。null は「日課と同じ時刻」を意味し、[resetHour] を参照する。
     * 値をコピーせず null で表すことで、日課リセット時刻の変更に自動で追従する。
     */
    val weeklyResetHour: Int? = null,
    val lastWeeklyResetDate: LocalDate? = null,
    /** false の間は休止中。日課・週課の追加/更新を禁止し、通知対象からも除外する */
    val isEnabled: Boolean = true,
) {
    init {
        require(resetHour in 0..MAX_HOUR) {
            "resetHour must be in 0..23, but was $resetHour"
        }
        require(resetDayOfWeek in MIN_ISO_DAY..MAX_ISO_DAY) {
            "resetDayOfWeek must be in 1..7, but was $resetDayOfWeek"
        }
        require(weeklyResetHour == null || weeklyResetHour in 0..MAX_HOUR) {
            "weeklyResetHour must be null or in 0..23, but was $weeklyResetHour"
        }
    }

    companion object {
        const val DEFAULT_RESET_HOUR = 5

        /** 月曜 */
        const val DEFAULT_RESET_DAY_OF_WEEK = 1

        private const val MAX_HOUR = 23
        private const val MIN_ISO_DAY = 1
        private const val MAX_ISO_DAY = 7
    }
}
