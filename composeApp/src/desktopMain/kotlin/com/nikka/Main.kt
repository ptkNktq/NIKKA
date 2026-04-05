package com.nikka

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nikka.desktop.ui.component.NikkaWindowTopBar

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(480.dp, 720.dp),
        position = WindowPosition(Alignment.Center),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "NIKKA",
        state = windowState,
        resizable = true,
        undecorated = true,
    ) {
        val scope = this
        App(
            topBar = { actions ->
                scope.NikkaWindowTopBar(
                    onMinimize = { windowState.isMinimized = true },
                    onClose = ::exitApplication,
                    actions = actions,
                )
            },
        )
    }
}
