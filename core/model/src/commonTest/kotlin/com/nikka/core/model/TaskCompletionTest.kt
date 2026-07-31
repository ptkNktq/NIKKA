package com.nikka.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskCompletionTest {

    private fun task(
        title: String,
        isCompleted: Boolean,
        type: TaskType = TaskType.DAILY,
    ) = Task(id = title, groupId = "g1", title = title, isCompleted = isCompleted, type = type)

    @Test
    fun `optional task is excluded from completion judgement`() {
        val tasks = listOf(
            task("daily", isCompleted = true),
            task("optional", isCompleted = false, type = TaskType.OPTIONAL),
        )
        assertTrue(tasks.allTasksCompleted(dailyOnly = false))
    }

    @Test
    fun `uncompleted non-optional task blocks completion`() {
        val tasks = listOf(
            task("daily", isCompleted = false),
            task("optional", isCompleted = true, type = TaskType.OPTIONAL),
        )
        assertFalse(tasks.allTasksCompleted(dailyOnly = false))
    }

    @Test
    fun `all-optional group falls back to judging optional tasks themselves`() {
        val incomplete = listOf(task("optional", isCompleted = false, type = TaskType.OPTIONAL))
        assertFalse(incomplete.allTasksCompleted(dailyOnly = false))

        val complete = listOf(task("optional", isCompleted = true, type = TaskType.OPTIONAL))
        assertTrue(complete.allTasksCompleted(dailyOnly = false))
    }

    @Test
    fun `dailyOnly ignores weekly and optional tasks when daily tasks exist`() {
        val tasks = listOf(
            task("daily", isCompleted = true),
            task("weekly", isCompleted = false, type = TaskType.WEEKLY),
            task("optional", isCompleted = false, type = TaskType.OPTIONAL),
        )
        assertTrue(tasks.allTasksCompleted(dailyOnly = true))
    }

    @Test
    fun `dailyOnly falls back to weekly tasks when no daily task exists`() {
        val tasks = listOf(
            task("weekly", isCompleted = false, type = TaskType.WEEKLY),
            task("optional", isCompleted = true, type = TaskType.OPTIONAL),
        )
        assertFalse(tasks.allTasksCompleted(dailyOnly = true))
    }

    @Test
    fun `empty list is never completed`() {
        assertFalse(emptyList<Task>().allTasksCompleted(dailyOnly = false))
    }
}
