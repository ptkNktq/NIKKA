package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup

class FakeTaskRepository : TaskRepository {
    private var groups = mutableListOf<TaskGroup>()
    private var tasks = mutableListOf<DailyTask>()

    override suspend fun loadGroups(): List<TaskGroup> = groups.toList()
    override suspend fun loadTasks(): List<DailyTask> = tasks.toList()
    override suspend fun saveAll(groups: List<TaskGroup>, tasks: List<DailyTask>) {
        this.groups = groups.toMutableList()
        this.tasks = tasks.toMutableList()
    }
}
