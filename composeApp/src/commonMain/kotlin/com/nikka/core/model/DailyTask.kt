package com.nikka.core.model

data class DailyTask(
    val id: String,
    val groupId: String,
    val title: String,
    val isCompleted: Boolean = false,
)
