package com.nikka.core.data

import com.nikka.core.model.NotificationSettings
import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

class FakeTaskRepository : TaskRepository {
    private var groups = mutableListOf<TaskGroup>()
    private var tasks = mutableListOf<Task>()
    private var lastNotifiedDate: LocalDate? = null
    private var lastWeeklyNotifiedDate: LocalDate? = null

    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    override val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

    override suspend fun loadGroups(): List<TaskGroup> = groups.toList()
    override suspend fun loadTasks(): List<Task> = tasks.toList()
    override suspend fun saveAll(groups: List<TaskGroup>, tasks: List<Task>) {
        this.groups = groups.toMutableList()
        this.tasks = tasks.toMutableList()
    }

    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        _notificationSettings.value = settings
    }

    override suspend fun loadLastNotifiedDate(): LocalDate? = lastNotifiedDate
    override suspend fun saveLastNotifiedDate(date: LocalDate) {
        lastNotifiedDate = date
    }

    override suspend fun loadLastWeeklyNotifiedDate(): LocalDate? = lastWeeklyNotifiedDate
    override suspend fun saveLastWeeklyNotifiedDate(date: LocalDate) {
        lastWeeklyNotifiedDate = date
    }
}
