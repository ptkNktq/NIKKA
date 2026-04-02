package com.nikka.core.model

data class DailyTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
)
