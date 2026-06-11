package com.nikka.core.data

import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasUncompletedDailyTasksTest {

    private val today = LocalDate(2026, 6, 12)
    private val group = TaskGroup(id = "g1", name = "原神", lastResetDate = today)

    private fun task(
        id: String,
        isCompleted: Boolean,
        type: TaskType = TaskType.DAILY,
    ) = Task(id = id, groupId = "g1", title = id, isCompleted = isCompleted, type = type)

    @Test
    fun `uncompleted daily task triggers reminder`() {
        val result = hasUncompletedDailyTasks(
            groups = listOf(group),
            tasks = listOf(task("daily", isCompleted = false)),
            currentHour = 21,
            today = today,
        )
        assertTrue(result)
    }

    @Test
    fun `uncompleted weekly task does not trigger reminder when all dailies are completed`() {
        val result = hasUncompletedDailyTasks(
            groups = listOf(group),
            tasks = listOf(
                task("daily", isCompleted = true),
                task("weekly", isCompleted = false, type = TaskType.WEEKLY),
            ),
            currentHour = 21,
            today = today,
        )
        assertFalse(result)
    }

    @Test
    fun `completed daily task in pending-reset group is treated as uncompleted`() {
        // resetHour 到達済みかつ当日未リセット → 前日の完了フラグは信用しない
        val pendingGroup = TaskGroup(
            id = "g1",
            name = "原神",
            resetHour = 5,
            lastResetDate = today.minusOneDay(),
        )
        val result = hasUncompletedDailyTasks(
            groups = listOf(pendingGroup),
            tasks = listOf(task("daily", isCompleted = true)),
            currentHour = 21,
            today = today,
        )
        assertTrue(result)
    }

    @Test
    fun `completed weekly task in pending-reset group does not trigger reminder`() {
        // 日次リセットは週課を未完了に戻さないため、リセット待機でも週課は未達成扱いにしない
        val pendingGroup = TaskGroup(
            id = "g1",
            name = "原神",
            resetHour = 5,
            lastResetDate = today.minusOneDay(),
        )
        val result = hasUncompletedDailyTasks(
            groups = listOf(pendingGroup),
            tasks = listOf(task("weekly", isCompleted = true, type = TaskType.WEEKLY)),
            currentHour = 21,
            today = today,
        )
        assertFalse(result)
    }

    @Test
    fun `all tasks completed does not trigger reminder`() {
        val result = hasUncompletedDailyTasks(
            groups = listOf(group),
            tasks = listOf(
                task("daily", isCompleted = true),
                task("weekly", isCompleted = true, type = TaskType.WEEKLY),
            ),
            currentHour = 21,
            today = today,
        )
        assertFalse(result)
    }

    private fun LocalDate.minusOneDay(): LocalDate = LocalDate.fromEpochDays(toEpochDays() - 1)
}
