package com.nikka.core.data

import com.nikka.core.model.NotificationSettings
import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface TaskRepository {
    val notificationSettings: StateFlow<NotificationSettings>

    suspend fun loadGroups(): List<TaskGroup>
    suspend fun loadTasks(): List<Task>
    suspend fun saveAll(groups: List<TaskGroup>, tasks: List<Task>)

    suspend fun saveNotificationSettings(settings: NotificationSettings)

    suspend fun loadLastNotifiedDate(): LocalDate?
    suspend fun saveLastNotifiedDate(date: LocalDate)

    suspend fun loadLastWeeklyNotifiedDate(): LocalDate?
    suspend fun saveLastWeeklyNotifiedDate(date: LocalDate)
}
