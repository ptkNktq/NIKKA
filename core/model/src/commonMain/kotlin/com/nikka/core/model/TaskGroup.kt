package com.nikka.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class TaskGroup(
    val id: String,
    val name: String,
    val resetHour: Int? = null,
    val lastResetDate: LocalDate? = null,
) {
    init {
        require(resetHour == null || resetHour in 0..MAX_HOUR) {
            "resetHour must be null or in 0..23, but was $resetHour"
        }
    }

    companion object {
        private const val MAX_HOUR = 23
    }
}
