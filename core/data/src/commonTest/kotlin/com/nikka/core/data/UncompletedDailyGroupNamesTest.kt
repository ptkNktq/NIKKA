package com.nikka.core.data

import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UncompletedDailyGroupNamesTest {

    private val today = LocalDate(2026, 6, 12)

    private fun group(id: String, name: String) =
        TaskGroup(id = id, name = name, lastResetDate = today)

    private fun task(
        groupId: String,
        title: String,
        isCompleted: Boolean,
        type: TaskType = TaskType.DAILY,
    ) = Task(id = "$groupId-$title", groupId = groupId, title = title, isCompleted = isCompleted, type = type)

    @Test
    fun `groups with uncompleted daily tasks are listed in display order`() {
        val names = uncompletedDailyGroupNames(
            groups = listOf(group("g1", "原神"), group("g2", "スターレイル"), group("g3", "ゼンゼロ")),
            tasks = listOf(
                task("g1", "デイリー任務", isCompleted = false),
                task("g1", "樹脂消費", isCompleted = false),
                task("g2", "デイリー", isCompleted = true),
                task("g3", "デイリー", isCompleted = false),
            ),
            currentHour = 21,
            today = today,
        )
        // 未達成が複数あってもグループ名は 1 回だけ列挙される
        assertEquals(listOf("原神", "ゼンゼロ"), names)
    }

    @Test
    fun `group with only uncompleted weekly tasks is not listed`() {
        val names = uncompletedDailyGroupNames(
            groups = listOf(group("g1", "原神")),
            tasks = listOf(
                task("g1", "daily", isCompleted = true),
                task("g1", "weekly", isCompleted = false, type = TaskType.WEEKLY),
            ),
            currentHour = 21,
            today = today,
        )
        assertTrue(names.isEmpty())
    }

    @Test
    fun `pending-reset group with completed daily task is listed`() {
        // resetHour 到達済みかつ当日未リセット → 前日の完了フラグは信用しない
        val pendingGroup = TaskGroup(
            id = "g1",
            name = "原神",
            resetHour = 5,
            lastResetDate = today.minusOneDay(),
        )
        val names = uncompletedDailyGroupNames(
            groups = listOf(pendingGroup),
            tasks = listOf(task("g1", "daily", isCompleted = true)),
            currentHour = 21,
            today = today,
        )
        assertEquals(listOf("原神"), names)
    }

    @Test
    fun `disabled group is not listed even with uncompleted daily tasks`() {
        val disabledGroup = group("g1", "原神").copy(isEnabled = false)
        val names = uncompletedDailyGroupNames(
            groups = listOf(disabledGroup),
            tasks = listOf(task("g1", "daily", isCompleted = false)),
            currentHour = 21,
            today = today,
        )
        assertTrue(names.isEmpty())
    }

    @Test
    fun `all tasks completed yields empty list`() {
        val names = uncompletedDailyGroupNames(
            groups = listOf(group("g1", "原神")),
            tasks = listOf(
                task("g1", "daily", isCompleted = true),
                task("g1", "weekly", isCompleted = true, type = TaskType.WEEKLY),
            ),
            currentHour = 21,
            today = today,
        )
        assertTrue(names.isEmpty())
    }

    private fun LocalDate.minusOneDay(): LocalDate = LocalDate.fromEpochDays(toEpochDays() - 1)
}
