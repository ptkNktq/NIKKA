package com.nikka.core.data

import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UncompletedWeeklyTaskTitlesTest {

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
        title: String,
        isCompleted: Boolean,
        type: TaskType = TaskType.WEEKLY,
    ) = Task(id = title, groupId = "g1", title = title, isCompleted = isCompleted, type = type)

    @Test
    fun `uncompleted weekly tasks are listed on the day before reset day`() {
        val titles = uncompletedWeeklyTaskTitles(
            groups = listOf(group),
            tasks = listOf(
                task("週ボス", isCompleted = false),
                task("紀行", isCompleted = true),
                task("聖遺物周回", isCompleted = false),
            ),
            currentHour = 21,
            today = sunday,
        )
        assertEquals(listOf("週ボス", "聖遺物周回"), titles)
    }

    @Test
    fun `completed weekly task is not listed`() {
        val titles = uncompletedWeeklyTaskTitles(
            groups = listOf(group),
            tasks = listOf(task("週ボス", isCompleted = true)),
            currentHour = 21,
            today = sunday,
        )
        assertTrue(titles.isEmpty())
    }

    @Test
    fun `uncompleted weekly task is not listed when tomorrow is not reset day`() {
        val titles = uncompletedWeeklyTaskTitles(
            groups = listOf(group),
            tasks = listOf(task("週ボス", isCompleted = false)),
            currentHour = 21,
            today = saturday,
        )
        assertTrue(titles.isEmpty())
    }

    @Test
    fun `uncompleted daily task is not listed in weekly reminder`() {
        val titles = uncompletedWeeklyTaskTitles(
            groups = listOf(group),
            tasks = listOf(task("デイリー", isCompleted = false, type = TaskType.DAILY)),
            currentHour = 21,
            today = sunday,
        )
        assertTrue(titles.isEmpty())
    }

    @Test
    fun `completed weekly task in pending-reset group is listed`() {
        // 直近のリセット予定日 (3/30) が未実施 → 完了フラグは前週のものなので信用しない
        val pendingGroup = group.copy(lastWeeklyResetDate = LocalDate(2026, 3, 23))
        val titles = uncompletedWeeklyTaskTitles(
            groups = listOf(pendingGroup),
            tasks = listOf(task("週ボス", isCompleted = true)),
            currentHour = 21,
            today = sunday,
        )
        assertEquals(listOf("週ボス"), titles)
    }
}
