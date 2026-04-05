package com.nikka.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyTask(
    val id: String,
    val groupId: String,
    val title: String,
    val isCompleted: Boolean = false,
)
