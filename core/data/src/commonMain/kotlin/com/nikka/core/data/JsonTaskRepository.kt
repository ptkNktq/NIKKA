package com.nikka.core.data

import com.nikka.core.model.DailyTask
import com.nikka.core.model.NotificationSettings
import com.nikka.core.model.TaskGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

// JVM API (java.nio.file) を使用。ターゲットは jvm("desktop") のみのため commonMain に配置。
// マルチプラットフォーム対応時は desktopMain に移動し、expect/actual で抽象化すること。
class JsonTaskRepository(
    private val filePath: Path = defaultFilePath(),
) : TaskRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val mutex = Mutex()

    private val _notificationSettings: MutableStateFlow<NotificationSettings> =
        // コンストラクタで初期値をロード完了させ、VM / Scheduler 初期化との race を排除する。
        MutableStateFlow(runBlocking { load().notificationSettings })
    override val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

    override suspend fun loadGroups(): List<TaskGroup> = load().groups

    override suspend fun loadTasks(): List<DailyTask> = load().tasks

    override suspend fun saveAll(groups: List<TaskGroup>, tasks: List<DailyTask>) {
        mutex.withLock {
            val current = readFile()
            writeFile(current.copy(groups = groups, tasks = tasks))
        }
    }

    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        mutex.withLock {
            val current = readFile()
            writeFile(current.copy(notificationSettings = settings))
            // ファイル書き込み成功と StateFlow 更新を原子的に扱う
            _notificationSettings.value = settings
        }
    }

    override suspend fun loadLastNotifiedDate(): LocalDate? = load().lastNotifiedDate

    override suspend fun saveLastNotifiedDate(date: LocalDate) {
        mutex.withLock {
            val current = readFile()
            writeFile(current.copy(lastNotifiedDate = date))
        }
    }

    private suspend fun load(): NikkaData {
        mutex.withLock { return readFile() }
    }

    private fun readFile(): NikkaData {
        val text = if (Files.exists(filePath)) Files.readString(filePath) else ""
        if (text.isBlank()) return NikkaData()
        return try {
            json.decodeFromString<NikkaData>(text)
        } catch (_: Exception) {
            migrateFromLegacy(text)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun migrateFromLegacy(text: String): NikkaData {
        return try {
            val root = Json.parseToJsonElement(text).jsonObject
            val groups = root["groups"]?.jsonArray?.map { elem ->
                val obj = elem.jsonObject
                val lastResetStr = obj["lastResetDate"]?.jsonPrimitive?.content
                TaskGroup(
                    id = obj["id"]!!.jsonPrimitive.content,
                    name = obj["name"]!!.jsonPrimitive.content,
                    resetHour = obj["resetHour"]?.jsonPrimitive?.content?.toIntOrNull(),
                    lastResetDate = lastResetStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                )
            } ?: emptyList()
            val tasks = root["tasks"]?.jsonArray?.map { elem ->
                json.decodeFromJsonElement(DailyTask.serializer(), elem)
            } ?: emptyList()
            NikkaData(groups = groups, tasks = tasks)
        } catch (_: Exception) {
            NikkaData()
        }
    }

    private fun writeFile(data: NikkaData) {
        Files.createDirectories(filePath.parent)
        val tmp = Files.createTempFile(filePath.parent, "nikka", ".tmp")
        Files.writeString(tmp, json.encodeToString(NikkaData.serializer(), data))
        try {
            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        fun defaultFilePath(): Path {
            val appDir = Paths.get(System.getProperty("user.home"), ".nikka")
            return appDir.resolve("data.json")
        }
    }
}
