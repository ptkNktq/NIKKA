package com.nikka.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeViewModelTest {

    private val viewModel = HomeViewModel()

    @Test
    fun `initial state has empty task list`() {
        val state = viewModel.uiState.value
        assertTrue(state.tasks.isEmpty())
    }

    @Test
    fun `addTask adds a new task`() {
        viewModel.addTask("Test Task")

        val state = viewModel.uiState.value
        assertEquals(1, state.tasks.size)
        assertEquals("Test Task", state.tasks.first().title)
        assertFalse(state.tasks.first().isCompleted)
    }

    @Test
    fun `addTask with blank title does nothing`() {
        viewModel.addTask("   ")

        val state = viewModel.uiState.value
        assertTrue(state.tasks.isEmpty())
    }

    @Test
    fun `toggleTask toggles completion status`() {
        viewModel.addTask("Test Task")
        val taskId = viewModel.uiState.value.tasks.first().id

        viewModel.toggleTask(taskId)

        assertTrue(viewModel.uiState.value.tasks.first().isCompleted)

        viewModel.toggleTask(taskId)

        assertFalse(viewModel.uiState.value.tasks.first().isCompleted)
    }
}
