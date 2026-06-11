package com.nikka.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class TaskGroup(
    val id: String,
    val name: String,
    val resetHour: Int? = null,
    val lastResetDate: LocalDate? = null,
    /** 週課のリセット曜日 (ISO 8601: 1=月曜 .. 7=日曜)。null で週次リセット無効 */
    val resetDayOfWeek: Int? = null,
    val lastWeeklyResetDate: LocalDate? = null,
) {
    init {
        require(resetHour == null || resetHour in 0..MAX_HOUR) {
            "resetHour must be null or in 0..23, but was $resetHour"
        }
        require(resetDayOfWeek == null || resetDayOfWeek in MIN_ISO_DAY..MAX_ISO_DAY) {
            "resetDayOfWeek must be null or in 1..7, but was $resetDayOfWeek"
        }
    }

    companion object {
        private const val MAX_HOUR = 23
        private const val MIN_ISO_DAY = 1
        private const val MAX_ISO_DAY = 7
    }
}
