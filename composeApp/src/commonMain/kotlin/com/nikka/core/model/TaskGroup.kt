package com.nikka.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TaskGroup(
    val id: String,
    val name: String,
    val colorIndex: Int = 0,
)
