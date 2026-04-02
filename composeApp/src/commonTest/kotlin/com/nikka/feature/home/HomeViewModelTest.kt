package com.nikka.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeViewModelTest {

    private val viewModel = HomeViewModel()

    // --- Group tests ---

    @Test
    fun `initial state has empty groups and tasks`() {
        val state = viewModel.uiState.value
        assertTrue(state.groups.isEmpty())
        assertTrue(state.tasks.isEmpty())
    }

    @Test
    fun `addGroup adds a new group`() {
        viewModel.addGroup("原神")

        val state = viewModel.uiState.value
        assertEquals(1, state.groups.size)
        assertEquals("原神", state.groups.first().name)
    }

    @Test
    fun `addGroup with blank name does nothing`() {
        viewModel.addGroup("   ")

        assertTrue(viewModel.uiState.value.groups.isEmpty())
    }

    @Test
    fun `addGroup assigns incremental color index`() {
        viewModel.addGroup("原神")
        viewModel.addGroup("スターレイル")

        val groups = viewModel.uiState.value.groups
        assertEquals(0, groups[0].colorIndex)
        assertEquals(1, groups[1].colorIndex)
    }

    @Test
    fun `removeGroup removes group and its tasks`() {
        viewModel.addGroup("原神")
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "デイリー")

        viewModel.removeGroup(groupId)

        val state = viewModel.uiState.value
        assertTrue(state.groups.isEmpty())
        assertTrue(state.tasks.isEmpty())
    }

    // --- Task tests ---

    @Test
    fun `addTask adds a task to specified group`() {
        viewModel.addGroup("原神")
        val groupId = viewModel.uiState.value.groups.first().id

        viewModel.addTask(groupId, "デイリー任務")

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("デイリー任務", state.tasks.first().title)
        assertEquals(groupId, state.tasks.first().groupId)
        assertFalse(state.tasks.first().isCompleted)
    }

    @Test
    fun `addTask with blank title does nothing`() {
        viewModel.addGroup("原神")
        val groupId = viewModel.uiState.value.groups.first().id

        viewModel.addTask(groupId, "   ")

        assertTrue(viewModel.uiState.value.tasks.isEmpty())
    }

    @Test
    fun `removeTask removes only the specified task`() {
        viewModel.addGroup("原神")
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "タスク1")
        viewModel.addTask(groupId, "タスク2")

        val taskId = viewModel.uiState.value.tasks.first().id
        viewModel.removeTask(taskId)

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("タスク2", state.tasks.first().title)
    }

    @Test
    fun `toggleTask toggles completion status`() {
        viewModel.addGroup("原神")
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.addTask(groupId, "デイリー")
        val taskId = viewModel.uiState.value.tasks.first().id

        viewModel.toggleTask(taskId)
        assertTrue(viewModel.uiState.value.tasks.first().isCompleted)

        viewModel.toggleTask(taskId)
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
    fun `addGroup dismisses dialog`() {
        viewModel.showAddGroupDialog()
        viewModel.addGroup("テスト")

        assertFalse(viewModel.uiState.value.isAddGroupDialogVisible)
    }

    @Test
    fun `addTask dismisses dialog`() {
        viewModel.addGroup("原神")
        val groupId = viewModel.uiState.value.groups.first().id
        viewModel.showAddTaskDialog(groupId)

        viewModel.addTask(groupId, "デイリー")

        val state = viewModel.uiState.value
        assertFalse(state.isAddTaskDialogVisible)
        assertNull(state.addTaskTargetGroupId)
    }
}
