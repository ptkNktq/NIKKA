package com.nikka

import androidx.compose.runtime.LaunchedEffect
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
import com.nikka.core.data.DiscordWebhookClient
import com.nikka.core.data.NotificationScheduler
import com.nikka.desktop.ui.component.NikkaWindowTopBar
import com.nikka.di.appModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

fun main() {
    startKoin { modules(appModule) }
    application {
        val windowState = rememberWindowState(
            size = DpSize(WINDOW_WIDTH_DP.dp, WINDOW_HEIGHT_DP.dp),
            position = WindowPosition(Alignment.Center),
        )

        var isVisible by remember { mutableStateOf(true) }
        val icon = painterResource("icon.png")

        val onExit: () -> Unit = {
            releaseResources()
            exitApplication()
        }

        Tray(
            icon = icon,
            tooltip = "NIKKA",
            onAction = { isVisible = true },
            menu = {
                Item("開く", onClick = { isVisible = true })
                Item("終了", onClick = onExit)
            },
        )

        Window(
            onCloseRequest = onExit,
            title = "NIKKA",
            icon = icon,
            state = windowState,
            visible = isVisible,
            resizable = true,
            undecorated = true,
        ) {
            LaunchedEffect(isVisible) {
                if (isVisible) {
                    windowState.isMinimized = false
                    window.toFront()
                }
            }
            val scope = this
            App(
                topBar = { actions ->
                    scope.NikkaWindowTopBar(
                        onMinimize = { isVisible = false },
                        onClose = onExit,
                        actions = actions,
                    )
                },
            )
        }
    }
}

// アプリ終了時に DI で保持している長命リソース (Scheduler の scope / HttpClient) を明示解放する
private fun releaseResources() {
    val koin = GlobalContext.getOrNull() ?: return
    runCatching { koin.get<NotificationScheduler>().close() }
    runCatching { koin.get<DiscordWebhookClient>().close() }
    stopKoin()
}

private const val WINDOW_WIDTH_DP = 480
private const val WINDOW_HEIGHT_DP = 720
