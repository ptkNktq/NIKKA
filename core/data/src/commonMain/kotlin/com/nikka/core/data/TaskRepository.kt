package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.NotificationSettings
import com.nikka.core.model.TaskGroup
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface TaskRepository {
    val notificationSettings: StateFlow<NotificationSettings>

    suspend fun loadGroups(): List<TaskGroup>
    suspend fun loadTasks(): List<DailyTask>
    suspend fun saveAll(groups: List<TaskGroup>, tasks: List<DailyTask>)

    suspend fun saveNotificationSettings(settings: NotificationSettings)

    suspend fun loadLastNotifiedDate(): LocalDate?
    suspend fun saveLastNotifiedDate(date: LocalDate)
}
