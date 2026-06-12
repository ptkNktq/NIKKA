package com.nikka.feature.home

import com.nikka.core.data.FakeTaskRepository
import com.nikka.core.model.Task
import com.nikka.core.model.TaskGroup
import com.nikka.core.model.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTaskRepository
    private lateinit var viewModel: HomeViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTaskRepository()
        viewModel = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Group tests ---

    @Test
    fun `initial state has empty groups and tasks`() {
        val state = viewModel.uiState.value
        assertTrue(state.groups.isEmpty())
        assertTrue(state.tasks.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `addGroup adds a new group`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.groups.size)
        assertEquals("原神", state.groups.first().name)
    }

    @Test
    fun `addGroup with blank name does nothing`() = runTest {
        viewModel.addGroup("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.groups.isEmpty())
    }

    @Test
    fun `removeGroup removes group and its tasks`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "デイリー")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeGroup(groupId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.groups.isEmpty())
        assertTrue(state.tasks.isEmpty())
    }

    // --- Task tests ---

    @Test
    fun `addTask adds a task to specified group`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id

        viewModel.addTask(groupId, "デイリー任務")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("デイリー任務", state.tasks.first().title)
        assertEquals(groupId, state.tasks.first().groupId)
        assertFalse(state.tasks.first().isCompleted)
        assertEquals(TaskType.DAILY, state.tasks.first().type)
    }

    @Test
    fun `addTask with WEEKLY type creates a weekly task`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id

        viewModel.addTask(groupId, "週ボス討伐", TaskType.WEEKLY)
        testDispatcher.scheduler.advanceUntilIdle()

        val task = viewModel.uiState.value.tasks.first()
        assertEquals("週ボス討伐", task.title)
        assertEquals(TaskType.WEEKLY, task.type)
    }

    @Test
    fun `addTask with blank title does nothing`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id

        viewModel.addTask(groupId, "   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tasks.isEmpty())
    }

    @Test
    fun `removeTask removes only the specified task`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "タスク1")
        viewModel.addTask(groupId, "タスク2")
        testDispatcher.scheduler.advanceUntilIdle()

        val taskId = viewModel.uiState.value.tasks.first().id
        viewModel.removeTask(taskId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("タスク2", state.tasks.first().title)
    }

    @Test
    fun `toggleTask toggles completion status`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "デイリー")
        testDispatcher.scheduler.advanceUntilIdle()
        val taskId = viewModel.uiState.value.tasks.first().id

        viewModel.toggleTask(taskId)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.tasks.first().isCompleted)

        viewModel.toggleTask(taskId)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.tasks.first().isCompleted)
    }

    // --- Dialog state tests ---

    @Test
    fun `showAddGroupDialog and dismiss work correctly`() {
        viewModel.showAddGroupDialog()
        assertTrue(viewModel.uiState.value.isAddGroupDialogVisible)

        viewModel.dismissAddGroupDialog()
        assertFalse(viewModel.uiState.value.isAddGroupDialogVisible)
    }

    @Test
    fun `showAddTaskDialog and dismiss work correctly`() {
        viewModel.showAddTaskDialog("group-1")
        val state = viewModel.uiState.value
        assertTrue(state.isAddTaskDialogVisible)
        assertEquals("group-1", state.addTaskTargetGroupId)
        assertEquals(TaskType.DAILY, state.addTaskTargetType)

        viewModel.dismissAddTaskDialog()
        val dismissed = viewModel.uiState.value
        assertFalse(dismissed.isAddTaskDialogVisible)
        assertNull(dismissed.addTaskTargetGroupId)
    }

    @Test
    fun `showAddTaskDialog with WEEKLY stores target type`() {
        viewModel.showAddTaskDialog("group-1", TaskType.WEEKLY)
        val state = viewModel.uiState.value
        assertTrue(state.isAddTaskDialogVisible)
        assertEquals(TaskType.WEEKLY, state.addTaskTargetType)
    }

    @Test
    fun `addGroup dismisses dialog`() = runTest {
        viewModel.showAddGroupDialog()
        viewModel.addGroup("テスト")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAddGroupDialogVisible)
    }

    @Test
    fun `addTask dismisses dialog`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.showAddTaskDialog(groupId)

        viewModel.addTask(groupId, "デイリー")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAddTaskDialogVisible)
        assertNull(state.addTaskTargetGroupId)
    }

    // --- Persistence tests ---

    @Test
    fun `data is persisted via repository`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "デイリー")
        testDispatcher.scheduler.advanceUntilIdle()

        val savedGroups = repository.loadGroups()
        val savedTasks = repository.loadTasks()
        assertEquals(1, savedGroups.size)
        assertEquals(1, savedTasks.size)
        assertEquals("原神", savedGroups.first().name)
        assertEquals("デイリー", savedTasks.first().title)
    }

    // --- Move tests ---

    @Test
    fun `moveGroup reorders groups`() = runTest {
        viewModel.addGroup("原神")
        viewModel.addGroup("スターレイル")
        viewModel.addGroup("ゼンゼロ")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.moveGroup(0, 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val names = viewModel.uiState.value.groups.map { it.name }
        assertEquals(listOf("スターレイル", "ゼンゼロ", "原神"), names)
    }

    @Test
    fun `moveTask reorders tasks within group`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "タスク1")
        viewModel.addTask(groupId, "タスク2")
        viewModel.addTask(groupId, "タスク3")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.moveTask(groupId, TaskType.DAILY, 0, 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val titles = viewModel.uiState.value.tasks.map { it.title }
        assertEquals(listOf("タスク2", "タスク3", "タスク1"), titles)
    }

    @Test
    fun `moveTask reorders only within the same type section`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "日課1")
        viewModel.addTask(groupId, "週課1", TaskType.WEEKLY)
        viewModel.addTask(groupId, "日課2")
        viewModel.addTask(groupId, "週課2", TaskType.WEEKLY)
        testDispatcher.scheduler.advanceUntilIdle()

        // 週課セクション内 (週課1, 週課2) の並べ替え。日課の順序には影響しない
        viewModel.moveTask(groupId, TaskType.WEEKLY, 0, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val dailyTitles = state.tasks.filter { it.type == TaskType.DAILY }.map { it.title }
        val weeklyTitles = state.tasks.filter { it.type == TaskType.WEEKLY }.map { it.title }
        assertEquals(listOf("日課1", "日課2"), dailyTitles)
        assertEquals(listOf("週課2", "週課1"), weeklyTitles)
    }

    @Test
    fun `moveTask with same index does nothing`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "タスク1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.moveTask(groupId, TaskType.DAILY, 0, 0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("タスク1", viewModel.uiState.value.tasks.first().title)
    }

    // --- Auto collapse/expand tests ---

    @Test
    fun `toggleTask auto-collapses group when all tasks completed`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "タスク1")
        testDispatcher.scheduler.advanceUntilIdle()
        val taskId = viewModel.uiState.value.tasks.first().id

        viewModel.toggleTask(taskId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(groupId in viewModel.uiState.value.collapsedGroupIds)
    }

    @Test
    fun `resetGroupTasks expands collapsed group`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "タスク1")
        testDispatcher.scheduler.advanceUntilIdle()
        val taskId = viewModel.uiState.value.tasks.first().id

        viewModel.toggleTask(taskId)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(groupId in viewModel.uiState.value.collapsedGroupIds)

        viewModel.resetGroupTasks(groupId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(groupId in viewModel.uiState.value.collapsedGroupIds)
        assertFalse(viewModel.uiState.value.tasks.first().isCompleted)
    }

    // --- Reset hour tests ---

    @Test
    fun `setResetHour updates group and dismisses dialog`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id

        viewModel.showResetHourDialog(groupId)
        assertEquals(groupId, viewModel.uiState.value.resetHourTargetGroupId)

        viewModel.setResetHour(groupId, 5)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.groups.first().resetHour)
        assertNull(viewModel.uiState.value.resetHourTargetGroupId)
    }

    @Test
    fun `addGroup initializes reset baselines`() = runTest {
        // 水曜 10:00 に作成 → 日次は当日、週次は直近の月曜を実施済み扱いで初期化
        val vm = HomeViewModel(repository, fixedClock("2026-04-08T10:00:00Z"), utc)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()

        val group = vm.uiState.value.groups.first()
        assertEquals(TaskGroup.DEFAULT_RESET_HOUR, group.resetHour)
        assertEquals(TaskGroup.DEFAULT_RESET_DAY_OF_WEEK, group.resetDayOfWeek)
        assertEquals(LocalDate(2026, 4, 8), group.lastResetDate)
        assertEquals(LocalDate(2026, 4, 6), group.lastWeeklyResetDate)
    }

    // --- Delete confirm tests ---

    @Test
    fun `confirmDeleteGroup removes group via removeGroup`() = runTest {
        viewModel.addGroup("原神")
        testDispatcher.scheduler.advanceUntilIdle()
        val groupId = viewModel.uiState.value.groups.first().id

        viewModel.showDeleteGroupConfirm(groupId)
        viewModel.confirmDeleteGroup()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.groups.isEmpty())
        assertNull(viewModel.uiState.value.deleteGroupConfirmId)
    }

    // --- Auto reset tests ---

    // Auto reset tests use UTC timezone to avoid system timezone dependency

    private val utc = TimeZone.UTC

    @Test
    fun `auto reset triggers when current hour is past reset hour`() = runTest {
        // 2026-04-05T10:00 UTC, resetHour=5 → hour=10 >= 5 → reset
        val clock = fixedClock("2026-04-05T10:00:00Z")
        repository.saveAll(
            groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5)),
            tasks = listOf(Task(id = "t1", groupId = "g1", title = "デイリー", isCompleted = true)),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.tasks.first().isCompleted)
    }

    @Test
    fun `auto reset does not reset weekly tasks`() = runTest {
        // 日次リセットの条件を満たしても、週課 (週次は実施済み) は未完了に戻さない
        val clock = fixedClock("2026-04-05T10:00:00Z")
        repository.saveAll(
            groups = listOf(
                TaskGroup(
                    id = "g1",
                    name = "原神",
                    resetHour = 5,
                    // 直近の週次リセット (3/30 月曜) は実施済み
                    lastWeeklyResetDate = LocalDate(2026, 3, 30),
                ),
            ),
            tasks = listOf(
                Task(id = "t1", groupId = "g1", title = "デイリー", isCompleted = true),
                Task(id = "t2", groupId = "g1", title = "週ボス", isCompleted = true, type = TaskType.WEEKLY),
            ),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        val tasks = vm.uiState.value.tasks
        assertFalse(tasks.first { it.id == "t1" }.isCompleted)
        assertTrue(tasks.first { it.id == "t2" }.isCompleted)
    }

    @Test
    fun `auto reset does not trigger when current hour is before reset hour`() = runTest {
        // 2026-04-05T02:00 UTC, resetHour=5 → hour=2 < 5 → no reset
        val clock = fixedClock("2026-04-05T02:00:00Z")
        repository.saveAll(
            groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5)),
            tasks = listOf(Task(id = "t1", groupId = "g1", title = "デイリー", isCompleted = true)),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.tasks.first().isCompleted)
    }

    @Test
    fun `auto reset does not trigger twice on same day`() = runTest {
        // 2026-04-05T10:00 UTC, lastResetDate=2026-04-05 → already reset today
        val today = LocalDate(2026, 4, 5)
        val clock = fixedClock("2026-04-05T10:00:00Z")
        repository.saveAll(
            groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5, lastResetDate = today)),
            tasks = listOf(Task(id = "t1", groupId = "g1", title = "デイリー", isCompleted = true)),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.tasks.first().isCompleted)
    }

    @Test
    fun `auto reset does not trigger when date crossed but hour not reached`() = runTest {
        // 2026-04-05T03:00 UTC, resetHour=5, lastResetDate=04-04 → hour=3 < 5 → no reset
        val yesterday = LocalDate(2026, 4, 4)
        val clock = fixedClock("2026-04-05T03:00:00Z")
        repository.saveAll(
            groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5, lastResetDate = yesterday)),
            tasks = listOf(Task(id = "t1", groupId = "g1", title = "デイリー", isCompleted = true)),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.tasks.first().isCompleted)
    }

    // --- Weekly reset tests ---

    // 2026-04-06 は月曜日

    @Test
    fun `weekly reset triggers on reset day after reset hour`() = runTest {
        val clock = fixedClock("2026-04-06T10:00:00Z")
        repository.saveAll(
            groups = listOf(
                TaskGroup(
                    id = "g1",
                    name = "原神",
                    resetHour = 5,
                    resetDayOfWeek = 1,
                    lastWeeklyResetDate = LocalDate(2026, 3, 30),
                ),
            ),
            tasks = listOf(
                Task(id = "w1", groupId = "g1", title = "週ボス", isCompleted = true, type = TaskType.WEEKLY),
            ),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.tasks.first().isCompleted)
        assertEquals(LocalDate(2026, 4, 6), vm.uiState.value.groups.first().lastWeeklyResetDate)
    }

    @Test
    fun `weekly reset does not trigger before reset hour on reset day`() = runTest {
        val clock = fixedClock("2026-04-06T02:00:00Z")
        repository.saveAll(
            groups = listOf(
                TaskGroup(
                    id = "g1",
                    name = "原神",
                    resetHour = 5,
                    resetDayOfWeek = 1,
                    lastWeeklyResetDate = LocalDate(2026, 3, 30),
                ),
            ),
            tasks = listOf(
                Task(id = "w1", groupId = "g1", title = "週ボス", isCompleted = true, type = TaskType.WEEKLY),
            ),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.tasks.first().isCompleted)
    }

    @Test
    fun `weekly reset does not trigger twice in the same week`() = runTest {
        // 水曜日。直近の月曜 (4/6) は実施済み
        val clock = fixedClock("2026-04-08T10:00:00Z")
        repository.saveAll(
            groups = listOf(
                TaskGroup(
                    id = "g1",
                    name = "原神",
                    resetHour = 5,
                    resetDayOfWeek = 1,
                    lastWeeklyResetDate = LocalDate(2026, 4, 6),
                ),
            ),
            tasks = listOf(
                Task(id = "w1", groupId = "g1", title = "週ボス", isCompleted = true, type = TaskType.WEEKLY),
            ),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.tasks.first().isCompleted)
    }

    @Test
    fun `weekly reset triggers when app is opened later in the week`() = runTest {
        // 水曜日に起動。直近の月曜 (4/6) が未実施なのでリセットされる
        val clock = fixedClock("2026-04-08T10:00:00Z")
        repository.saveAll(
            groups = listOf(
                TaskGroup(
                    id = "g1",
                    name = "原神",
                    resetHour = 5,
                    resetDayOfWeek = 1,
                    lastWeeklyResetDate = LocalDate(2026, 3, 30),
                ),
            ),
            tasks = listOf(
                Task(id = "w1", groupId = "g1", title = "週ボス", isCompleted = true, type = TaskType.WEEKLY),
            ),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.tasks.first().isCompleted)
        assertEquals(LocalDate(2026, 4, 6), vm.uiState.value.groups.first().lastWeeklyResetDate)
    }

    @Test
    fun `weekly reset does not reset daily tasks`() = runTest {
        // 月曜 10:00。日次は実施済み (lastResetDate=当日) のため週課だけがリセットされる
        val clock = fixedClock("2026-04-06T10:00:00Z")
        repository.saveAll(
            groups = listOf(
                TaskGroup(
                    id = "g1",
                    name = "原神",
                    resetHour = 5,
                    lastResetDate = LocalDate(2026, 4, 6),
                    resetDayOfWeek = 1,
                    lastWeeklyResetDate = LocalDate(2026, 3, 30),
                ),
            ),
            tasks = listOf(
                Task(id = "d1", groupId = "g1", title = "デイリー", isCompleted = true),
                Task(id = "w1", groupId = "g1", title = "週ボス", isCompleted = true, type = TaskType.WEEKLY),
            ),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        val tasks = vm.uiState.value.tasks
        assertTrue(tasks.first { it.id == "d1" }.isCompleted)
        assertFalse(tasks.first { it.id == "w1" }.isCompleted)
    }

    @Test
    fun `setResetDayOfWeek keeps current week progress until next reset day`() = runTest {
        // 水曜日に曜日を木曜へ変更しても、その時点の進捗は消えない。翌日の木曜にリセットされる
        val mutableClock = object : Clock {
            var instant: Instant = Instant.parse("2026-04-08T10:00:00Z")
            override fun now(): Instant = instant
        }
        repository.saveAll(
            groups = listOf(
                TaskGroup(
                    id = "g1",
                    name = "原神",
                    lastResetDate = LocalDate(2026, 4, 8),
                    lastWeeklyResetDate = LocalDate(2026, 4, 6),
                ),
            ),
            tasks = listOf(
                Task(id = "w1", groupId = "g1", title = "週ボス", isCompleted = true, type = TaskType.WEEKLY),
            ),
        )
        val vm = HomeViewModel(repository, mutableClock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setResetDayOfWeek("g1", 4)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(4, vm.uiState.value.groups.first().resetDayOfWeek)

        vm.refreshAutoReset()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.tasks.first().isCompleted)

        // 木曜になったらリセットされる
        mutableClock.instant = Instant.parse("2026-04-09T10:00:00Z")
        vm.refreshAutoReset()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.tasks.first().isCompleted)
    }

    // --- Manual refresh tests ---

    @Test
    fun `manual refresh resets group when condition is met`() = runTest {
        // 起動時は resetHour 前 → リセットされない。その後時刻が進んだ想定で手動リフレッシュ
        val mutableClock = object : Clock {
            var instant: Instant = Instant.parse("2026-04-05T02:00:00Z")
            override fun now(): Instant = instant
        }
        repository.saveAll(
            groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5)),
            tasks = listOf(Task(id = "t1", groupId = "g1", title = "デイリー", isCompleted = true)),
        )
        val vm = HomeViewModel(repository, mutableClock, utc)
        testDispatcher.scheduler.advanceUntilIdle()
        // 起動時は条件未達なのでリセットされない
        assertTrue(vm.uiState.value.tasks.first().isCompleted)

        // 時刻を進めて手動リフレッシュ
        mutableClock.instant = Instant.parse("2026-04-05T10:00:00Z")
        vm.refreshAutoReset()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.tasks.first().isCompleted)
    }

    @Test
    fun `manual refresh is no-op when condition is not met`() = runTest {
        val clock = fixedClock("2026-04-05T02:00:00Z")
        repository.saveAll(
            groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5)),
            tasks = listOf(Task(id = "t1", groupId = "g1", title = "デイリー", isCompleted = true)),
        )
        val vm = HomeViewModel(repository, clock, utc)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.refreshAutoReset()
        testDispatcher.scheduler.advanceUntilIdle()

        // 条件未達なのでタスクの状態は変わらない
        assertTrue(vm.uiState.value.tasks.first().isCompleted)
    }

    @Test
    fun `manual refresh clears collapsed state of reset groups`() = runTest {
        // 起動時は resetHour 前 → リセットされず、全タスク完了で自動折りたたみされる
        val mutableClock = object : Clock {
            var instant: Instant = Instant.parse("2026-04-05T02:00:00Z")
            override fun now(): Instant = instant
        }
        repository.saveAll(
            groups = listOf(TaskGroup(id = "g1", name = "原神", resetHour = 5)),
            tasks = listOf(Task(id = "t1", groupId = "g1", title = "デイリー", isCompleted = true)),
        )
        val vm = HomeViewModel(repository, mutableClock, utc)
        testDispatcher.scheduler.advanceUntilIdle()
        // 全タスク完了により折りたたみ状態
        assertTrue("g1" in vm.uiState.value.collapsedGroupIds)

        // 時刻を進めて手動リフレッシュ → リセットされ、折りたたみも解除
        mutableClock.instant = Instant.parse("2026-04-05T10:00:00Z")
        vm.refreshAutoReset()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("g1" in vm.uiState.value.collapsedGroupIds)
        assertFalse(vm.uiState.value.tasks.first().isCompleted)
    }

    private fun fixedClock(isoInstant: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(isoInstant)
    }
}
