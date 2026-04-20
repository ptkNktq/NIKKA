package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.NotificationSettings
import com.nikka.core.model.TaskGroup
import kotlinx.datetime.LocalDate

interface TaskRepository {
    suspend fun loadGroups(): List<TaskGroup>
    suspend fun loadTasks(): List<DailyTask>
    suspend fun saveAll(groups: List<TaskGroup>, tasks: List<DailyTask>)

    suspend fun loadNotificationSettings(): NotificationSettings
    suspend fun saveNotificationSettings(settings: NotificationSettings)

    suspend fun loadLastNotifiedDate(): LocalDate?
    suspend fun saveLastNotifiedDate(date: LocalDate)
}
