package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.TaskGroup
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JsonTaskRepository(
    private val filePath: Path = defaultFilePath(),
) : TaskRepository {

    private val json = Json { prettyPrint = true }
    private val mutex = Mutex()

    override suspend fun loadGroups(): List<TaskGroup> = load().groups

    override suspend fun loadTasks(): List<DailyTask> = load().tasks

    override suspend fun saveGroups(groups: List<TaskGroup>) {
        mutex.withLock {
            val current = readFile()
            writeFile(current.copy(groups = groups))
        }
    }

    override suspend fun saveTasks(tasks: List<DailyTask>) {
        mutex.withLock {
            val current = readFile()
            writeFile(current.copy(tasks = tasks))
        }
    }

    private suspend fun load(): NikkaData {
        mutex.withLock { return readFile() }
    }

    private fun readFile(): NikkaData {
        if (!Files.exists(filePath)) return NikkaData()
        val text = Files.readString(filePath)
        return if (text.isBlank()) NikkaData() else json.decodeFromString<NikkaData>(text)
    }

    private fun writeFile(data: NikkaData) {
        Files.createDirectories(filePath.parent)
        Files.writeString(filePath, json.encodeToString(NikkaData.serializer(), data))
    }

    companion object {
        fun defaultFilePath(): Path {
            val appDir = Paths.get(System.getProperty("user.home"), ".nikka")
            return appDir.resolve("data.json")
        }
    }
}
