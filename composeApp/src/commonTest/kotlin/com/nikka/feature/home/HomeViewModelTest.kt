package com.nikka.feature.home

import com.nikka.core.data.FakeTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
    fun `addGroup assigns incremental color index`() = runTest {
        viewModel.addGroup("原神")
        viewModel.addGroup("スターレイル")
        testDispatcher.scheduler.advanceUntilIdle()

        val groups = viewModel.uiState.value.groups
        assertEquals(0, groups[0].colorIndex)
        assertEquals(1, groups[1].colorIndex)
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

        viewModel.dismissAddTaskDialog()
        val dismissed = viewModel.uiState.value
        assertFalse(dismissed.isAddTaskDialogVisible)
        assertNull(dismissed.addTaskTargetGroupId)
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
}
