package com.nikka

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nikka.core.ui.theme.NikkaTheme
import com.nikka.desktop.ui.component.NikkaWindowTopBar
import com.nikka.feature.license.LicenseScreen
import com.nikka.feature.settings.SettingsScreen

fun main() = application {
    val mainWindowState = rememberWindowState(
        size = DpSize(480.dp, 720.dp),
        position = WindowPosition(Alignment.Center),
    )

    var showSettings by remember { mutableStateOf(false) }
    var settingsFocusRequest by remember { mutableStateOf(0) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "NIKKA",
        icon = painterResource("icon.png"),
        state = mainWindowState,
        resizable = true,
        undecorated = true,
    ) {
        val scope = this
        App(
            onOpenSettings = {
                if (showSettings) {
                    settingsFocusRequest++
                } else {
                    showSettings = true
                }
            },
            topBar = { actions ->
                scope.NikkaWindowTopBar(
                    onMinimize = { mainWindowState.isMinimized = true },
                    onClose = ::exitApplication,
                    actions = actions,
                )
            },
        )
    }

    if (showSettings) {
        SettingsWindow(
            focusRequest = settingsFocusRequest,
            onClose = { showSettings = false },
        )
    }
}

@Composable
private fun androidx.compose.ui.window.ApplicationScope.SettingsWindow(
    focusRequest: Int,
    onClose: () -> Unit,
) {
    val settingsWindowState = rememberWindowState(
        size = DpSize(480.dp, 600.dp),
        position = WindowPosition(Alignment.Center),
    )

    var showLicense by remember { mutableStateOf(false) }
    val aboutLibsJson = remember {
        useResource("aboutlibraries.json") { it.bufferedReader().readText() }
    }

    Window(
        onCloseRequest = onClose,
        title = "設定",
        icon = painterResource("icon.png"),
        state = settingsWindowState,
        resizable = true,
        undecorated = true,
    ) {
        LaunchedEffect(focusRequest) {
            window.toFront()
        }
        NikkaTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                SettingsContent(
                    showLicense = showLicense,
                    aboutLibsJson = aboutLibsJson,
                    windowState = settingsWindowState,
                    onNavigateToLicense = { showLicense = true },
                    onBackFromLicense = { showLicense = false },
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
private fun WindowScope.SettingsContent(
    showLicense: Boolean,
    aboutLibsJson: String,
    windowState: WindowState,
    onNavigateToLicense: () -> Unit,
    onBackFromLicense: () -> Unit,
    onClose: () -> Unit,
) {
    if (showLicense) {
        LicenseScreen(
            aboutLibsJson = aboutLibsJson,
            topBar = {
                NikkaWindowTopBar(
                    onMinimize = { windowState.isMinimized = true },
                    onClose = onClose,
                    actions = {
                        IconButton(onClick = onBackFromLicense) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "戻る",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            },
        )
    } else {
        SettingsScreen(
            onNavigateToLicense = onNavigateToLicense,
            topBar = {
                NikkaWindowTopBar(
                    onMinimize = { windowState.isMinimized = true },
                    onClose = onClose,
                )
            },
        )
    }
}
