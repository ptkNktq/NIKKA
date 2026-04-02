package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup

interface TaskRepository {
    suspend fun loadGroups(): List<TaskGroup>
    suspend fun loadTasks(): List<DailyTask>
    suspend fun saveGroups(groups: List<TaskGroup>)
    suspend fun saveTasks(tasks: List<DailyTask>)
}
