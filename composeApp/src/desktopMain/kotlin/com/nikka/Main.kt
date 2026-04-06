package com.nikka

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
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

    var isVisible by remember { mutableStateOf(true) }
    val icon = painterResource("icon.png")

    Tray(
        icon = icon,
        tooltip = "NIKKA",
        onAction = { isVisible = true },
        menu = {
            Item("開く", onClick = { isVisible = true })
            Item("終了", onClick = ::exitApplication)
        },
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "NIKKA",
        icon = icon,
        state = windowState,
        visible = isVisible,
        resizable = true,
        undecorated = true,
    ) {
        val scope = this
        App(
            topBar = { actions ->
                scope.NikkaWindowTopBar(
                    onMinimize = { isVisible = false },
                    onClose = ::exitApplication,
                    actions = actions,
                )
            },
        )
    }
}
