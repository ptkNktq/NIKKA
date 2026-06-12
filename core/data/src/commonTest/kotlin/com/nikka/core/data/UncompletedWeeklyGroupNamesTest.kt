package com.nikka.core.data

import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UncompletedWeeklyGroupNamesTest {

    // 2026-04-05 は日曜日。リセット曜日は月曜 (ISO: 1) で「前日」に当たる
    private val sunday = LocalDate(2026, 4, 5)
    private val saturday = LocalDate(2026, 4, 4)
    private val lastMonday = LocalDate(2026, 3, 30)

    private fun group(id: String, name: String, resetDayOfWeek: Int = 1) = TaskGroup(
        id = id,
        name = name,
        resetHour = 5,
        resetDayOfWeek = resetDayOfWeek,
        lastWeeklyResetDate = lastMonday,
    )

    private fun task(
        groupId: String,
        title: String,
        isCompleted: Boolean,
        type: TaskType = TaskType.WEEKLY,
    ) = Task(id = "$groupId-$title", groupId = groupId, title = title, isCompleted = isCompleted, type = type)

    @Test
    fun `groups with uncompleted weekly tasks are listed on the day before reset day`() {
        val names = uncompletedWeeklyGroupNames(
            groups = listOf(group("g1", "原神"), group("g2", "スターレイル")),
            tasks = listOf(
                task("g1", "週ボス", isCompleted = false),
                task("g1", "紀行", isCompleted = false),
                task("g2", "週ボス", isCompleted = true),
            ),
            currentHour = 21,
            today = sunday,
        )
        // 未達成が複数あってもグループ名は 1 回だけ列挙される
        assertEquals(listOf("原神"), names)
    }

    @Test
    fun `group is not listed when tomorrow is not its reset day`() {
        val names = uncompletedWeeklyGroupNames(
            groups = listOf(group("g1", "原神")),
            tasks = listOf(task("g1", "週ボス", isCompleted = false)),
            currentHour = 21,
            today = saturday,
        )
        assertTrue(names.isEmpty())
    }

    @Test
    fun `group with only uncompleted daily tasks is not listed`() {
        val names = uncompletedWeeklyGroupNames(
            groups = listOf(group("g1", "原神")),
            tasks = listOf(task("g1", "デイリー", isCompleted = false, type = TaskType.DAILY)),
            currentHour = 21,
            today = sunday,
        )
        assertTrue(names.isEmpty())
    }

    @Test
    fun `pending-reset group with completed weekly task is listed`() {
        // 直近のリセット予定日 (3/30) が未実施 → 完了フラグは前週のものなので信用しない
        val pendingGroup = group("g1", "原神").copy(lastWeeklyResetDate = LocalDate(2026, 3, 23))
        val names = uncompletedWeeklyGroupNames(
            groups = listOf(pendingGroup),
            tasks = listOf(task("g1", "週ボス", isCompleted = true)),
            currentHour = 21,
            today = sunday,
        )
        assertEquals(listOf("原神"), names)
    }
}
