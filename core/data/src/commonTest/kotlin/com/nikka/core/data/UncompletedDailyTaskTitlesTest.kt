package com.nikka.core.data

import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UncompletedDailyTaskTitlesTest {

    private val today = LocalDate(2026, 6, 12)
    private val group = TaskGroup(id = "g1", name = "原神", lastResetDate = today)

    private fun task(
        title: String,
        isCompleted: Boolean,
        type: TaskType = TaskType.DAILY,
    ) = Task(id = title, groupId = "g1", title = title, isCompleted = isCompleted, type = type)

    @Test
    fun `uncompleted daily tasks are listed in display order`() {
        val titles = uncompletedDailyTaskTitles(
            groups = listOf(group),
            tasks = listOf(
                task("デイリー任務", isCompleted = false),
                task("樹脂消費", isCompleted = true),
                task("探索派遣", isCompleted = false),
            ),
            currentHour = 21,
            today = today,
        )
        assertEquals(listOf("デイリー任務", "探索派遣"), titles)
    }

    @Test
    fun `uncompleted weekly task is not listed even when all dailies are completed`() {
        val titles = uncompletedDailyTaskTitles(
            groups = listOf(group),
            tasks = listOf(
                task("daily", isCompleted = true),
                task("weekly", isCompleted = false, type = TaskType.WEEKLY),
            ),
            currentHour = 21,
            today = today,
        )
        assertTrue(titles.isEmpty())
    }

    @Test
    fun `completed daily task in pending-reset group is listed`() {
        // resetHour 到達済みかつ当日未リセット → 前日の完了フラグは信用しない
        val pendingGroup = TaskGroup(
            id = "g1",
            name = "原神",
            resetHour = 5,
            lastResetDate = today.minusOneDay(),
        )
        val titles = uncompletedDailyTaskTitles(
            groups = listOf(pendingGroup),
            tasks = listOf(task("daily", isCompleted = true)),
            currentHour = 21,
            today = today,
        )
        assertEquals(listOf("daily"), titles)
    }

    @Test
    fun `all tasks completed yields empty list`() {
        val titles = uncompletedDailyTaskTitles(
            groups = listOf(group),
            tasks = listOf(
                task("daily", isCompleted = true),
                task("weekly", isCompleted = true, type = TaskType.WEEKLY),
            ),
            currentHour = 21,
            today = today,
        )
        assertTrue(titles.isEmpty())
    }

    private fun LocalDate.minusOneDay(): LocalDate = LocalDate.fromEpochDays(toEpochDays() - 1)
}
