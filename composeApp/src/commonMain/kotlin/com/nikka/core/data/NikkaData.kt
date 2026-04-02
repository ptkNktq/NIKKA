package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup
import kotlinx.serialization.Serializable

@Serializable
data class NikkaData(
    val groups: List<TaskGroup> = emptyList(),
    val tasks: List<DailyTask> = emptyList(),
)
