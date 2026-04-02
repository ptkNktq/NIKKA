package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup

class FakeTaskRepository : TaskRepository {
    private var groups = mutableListOf<TaskGroup>()
    private var tasks = mutableListOf<DailyTask>()

    override suspend fun loadGroups(): List<TaskGroup> = groups.toList()
    override suspend fun loadTasks(): List<DailyTask> = tasks.toList()
    override suspend fun saveGroups(groups: List<TaskGroup>) { this.groups = groups.toMutableList() }
    override suspend fun saveTasks(tasks: List<DailyTask>) { this.tasks = tasks.toMutableList() }
}
