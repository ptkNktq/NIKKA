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
    val lastWeeklyResetDate: LocalDate? = null,
) {
    init {
        require(resetHour in 0..MAX_HOUR) {
            "resetHour must be in 0..23, but was $resetHour"
        }
        require(resetDayOfWeek in MIN_ISO_DAY..MAX_ISO_DAY) {
            "resetDayOfWeek must be in 1..7, but was $resetDayOfWeek"
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
