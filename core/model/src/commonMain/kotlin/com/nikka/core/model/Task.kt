package com.nikka.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val groupId: String,
    val title: String,
    val isCompleted: Boolean = false,
)
