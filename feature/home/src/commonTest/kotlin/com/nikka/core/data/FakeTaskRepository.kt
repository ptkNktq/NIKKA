package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.NotificationSettings
import com.nikka.core.model.TaskGroup
import kotlinx.datetime.LocalDate

class FakeTaskRepository : TaskRepository {
    private var groups = mutableListOf<TaskGroup>()
    private var tasks = mutableListOf<DailyTask>()
    private var notificationSettings = NotificationSettings()
    private var lastNotifiedDate: LocalDate? = null

    override suspend fun loadGroups(): List<TaskGroup> = groups.toList()
    override suspend fun loadTasks(): List<DailyTask> = tasks.toList()
    override suspend fun saveAll(groups: List<TaskGroup>, tasks: List<DailyTask>) {
        this.groups = groups.toMutableList()
        this.tasks = tasks.toMutableList()
    }

    override suspend fun loadNotificationSettings(): NotificationSettings = notificationSettings
    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        notificationSettings = settings
    }

    override suspend fun loadLastNotifiedDate(): LocalDate? = lastNotifiedDate
    override suspend fun saveLastNotifiedDate(date: LocalDate) {
        lastNotifiedDate = date
    }
}
