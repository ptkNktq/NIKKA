package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonTaskRepositoryTest {

    private val tempDir = Files.createTempDirectory("nikka-test")
    private val filePath = tempDir.resolve("data.json")
    private val repository = JsonTaskRepository(filePath)

    @AfterTest
    fun cleanup() {
        Files.deleteIfExists(filePath)
        Files.deleteIfExists(tempDir)
    }

    @Test
    fun `load returns empty data when file does not exist`() = runTest {
        assertEquals(emptyList(), repository.loadGroups())
        assertEquals(emptyList(), repository.loadTasks())
    }

    @Test
    fun `saveAll and load round-trips data`() = runTest {
        val groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5))
        val tasks = listOf(DailyTask(id = "t1", groupId = "g1", title = "デイリー"))

        repository.saveAll(groups, tasks)

        assertEquals(groups, repository.loadGroups())
        assertEquals(tasks, repository.loadTasks())
    }

    @Test
    fun `saveAll overwrites previous data`() = runTest {
        repository.saveAll(
            listOf(TaskGroup(id = "g1", name = "原神")),
            listOf(DailyTask(id = "t1", groupId = "g1", title = "タスク1")),
        )
        repository.saveAll(
            listOf(TaskGroup(id = "g2", name = "スターレイル")),
            listOf(DailyTask(id = "t2", groupId = "g2", title = "タスク2")),
        )

        val groups = repository.loadGroups()
        assertEquals(1, groups.size)
        assertEquals("スターレイル", groups.first().name)
    }

    @Test
    fun `saveAll preserves lastResetDate as LocalDate`() = runTest {
        val date = LocalDate(2026, 4, 5)
        val groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5, lastResetDate = date))
        repository.saveAll(groups, emptyList())

        val loaded = repository.loadGroups().first()
        assertEquals(date, loaded.lastResetDate)
    }

    @Test
    fun `load returns empty data when file is blank`() = runTest {
        Files.writeString(filePath, "   ")

        assertEquals(emptyList(), repository.loadGroups())
        assertEquals(emptyList(), repository.loadTasks())
    }

    @Test
    fun `migration from legacy format with colorIndex`() = runTest {
        val legacyJson = """
            {
              "groups": [
                {"id": "g1", "name": "原神", "colorIndex": 2}
              ],
              "tasks": [
                {"id": "t1", "groupId": "g1", "title": "デイリー", "isCompleted": true}
              ]
            }
        """.trimIndent()
        Files.writeString(filePath, legacyJson)

        val groups = repository.loadGroups()
        val tasks = repository.loadTasks()
        assertEquals(1, groups.size)
        assertEquals("原神", groups.first().name)
        assertEquals(1, tasks.size)
        assertTrue(tasks.first().isCompleted)
    }

    @Test
    fun `migration from legacy format with string lastResetDate`() = runTest {
        val legacyJson = """
            {
              "groups": [
                {"id": "g1", "name": "原神", "colorIndex": 0, "resetHour": 5, "lastResetDate": "2026-04-05"}
              ],
              "tasks": []
            }
        """.trimIndent()
        Files.writeString(filePath, legacyJson)

        val groups = repository.loadGroups()
        assertEquals(1, groups.size)
        assertEquals(5, groups.first().resetHour)
        assertEquals(LocalDate(2026, 4, 5), groups.first().lastResetDate)
    }

    @Test
    fun `migration returns empty data for corrupted json`() = runTest {
        Files.writeString(filePath, "not valid json at all")

        assertEquals(emptyList(), repository.loadGroups())
        assertEquals(emptyList(), repository.loadTasks())
    }

    @Test
    fun `atomic write creates file via temp file`() = runTest {
        repository.saveAll(
            listOf(TaskGroup(id = "g1", name = "テスト")),
            emptyList(),
        )

        assertTrue(Files.exists(filePath))
        val content = Files.readString(filePath)
        assertTrue(content.contains("テスト"))
    }
}
