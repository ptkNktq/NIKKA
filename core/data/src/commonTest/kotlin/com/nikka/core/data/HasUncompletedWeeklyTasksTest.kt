package com.nikka.core.data

import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasUncompletedWeeklyTasksTest {

    // 2026-04-05 は日曜日。リセット曜日は月曜 (ISO: 1) で「前日」に当たる
    private val sunday = LocalDate(2026, 4, 5)
    private val saturday = LocalDate(2026, 4, 4)
    private val lastMonday = LocalDate(2026, 3, 30)

    private val group = TaskGroup(
        id = "g1",
        name = "原神",
        resetHour = 5,
        resetDayOfWeek = 1,
        lastWeeklyResetDate = lastMonday,
    )

    private fun task(
        id: String,
        isCompleted: Boolean,
        type: TaskType = TaskType.WEEKLY,
    ) = Task(id = id, groupId = "g1", title = id, isCompleted = isCompleted, type = type)

    @Test
    fun `uncompleted weekly task triggers on the day before reset day`() {
        val result = hasUncompletedWeeklyTasks(
            groups = listOf(group),
            tasks = listOf(task("weekly", isCompleted = false)),
            currentHour = 21,
            today = sunday,
        )
        assertTrue(result)
    }

    @Test
    fun `completed weekly task does not trigger`() {
        val result = hasUncompletedWeeklyTasks(
            groups = listOf(group),
            tasks = listOf(task("weekly", isCompleted = true)),
            currentHour = 21,
            today = sunday,
        )
        assertFalse(result)
    }

    @Test
    fun `uncompleted weekly task does not trigger when tomorrow is not reset day`() {
        val result = hasUncompletedWeeklyTasks(
            groups = listOf(group),
            tasks = listOf(task("weekly", isCompleted = false)),
            currentHour = 21,
            today = saturday,
        )
        assertFalse(result)
    }

    @Test
    fun `uncompleted daily task does not trigger weekly reminder`() {
        val result = hasUncompletedWeeklyTasks(
            groups = listOf(group),
            tasks = listOf(task("daily", isCompleted = false, type = TaskType.DAILY)),
            currentHour = 21,
            today = sunday,
        )
        assertFalse(result)
    }

    @Test
    fun `group without reset day never triggers`() {
        val noDayGroup = group.copy(resetDayOfWeek = null, lastWeeklyResetDate = null)
        val result = hasUncompletedWeeklyTasks(
            groups = listOf(noDayGroup),
            tasks = listOf(task("weekly", isCompleted = false)),
            currentHour = 21,
            today = sunday,
        )
        assertFalse(result)
    }

    @Test
    fun `completed weekly task in pending-reset group is treated as uncompleted`() {
        // 直近のリセット予定日 (3/30) が未実施 → 完了フラグは前週のものなので信用しない
        val pendingGroup = group.copy(lastWeeklyResetDate = LocalDate(2026, 3, 23))
        val result = hasUncompletedWeeklyTasks(
            groups = listOf(pendingGroup),
            tasks = listOf(task("weekly", isCompleted = true)),
            currentHour = 21,
            today = sunday,
        )
        assertTrue(result)
    }
}
