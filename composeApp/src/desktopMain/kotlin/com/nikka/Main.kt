package com.nikka

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NIKKA",
        state = rememberWindowState(
            size = DpSize(480.dp, 720.dp),
            position = WindowPosition(Alignment.Center),
        ),
        resizable = true,
    ) {
        App()
    }
}
