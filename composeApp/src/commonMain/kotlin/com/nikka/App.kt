package com.nikka

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.useResource
import com.nikka.core.data.DiscordWebhookClient
import com.nikka.core.data.NotificationScheduler
import com.nikka.core.ui.component.LocalTopBarSlot
import com.nikka.core.ui.component.TopBarSlot
import com.nikka.core.ui.theme.NikkaTheme
import com.nikka.di.appModule
import com.nikka.feature.home.HomeScreen
import com.nikka.feature.license.LicenseScreen
import com.nikka.feature.settings.NotificationSettingsScreen
import com.nikka.feature.settings.SettingsScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

private enum class Screen {
    Home,
    Settings,
    NotificationSettings,
    License,
}

@Composable
fun App(
    topBar: @Composable (actions: @Composable () -> Unit) -> Unit = {},
) {
    KoinApplication(application = { modules(appModule) }) {
        NikkaTheme {
            val scheduler: NotificationScheduler = koinInject()
            val webhookClient: DiscordWebhookClient = koinInject()
            DisposableEffect(Unit) {
                scheduler.start()
                onDispose {
                    scheduler.close()
                    webhookClient.close()
                }
            }

            val topBarSlot = remember { TopBarSlot() }
            var currentScreen by remember { mutableStateOf(Screen.Home) }

            CompositionLocalProvider(LocalTopBarSlot provides topBarSlot) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        topBar {
                            topBarSlot.actions()
                            TopBarActions(currentScreen) { currentScreen = it }
                        }
                    },
                ) { innerPadding ->
                    AppContent(
                        currentScreen = currentScreen,
                        innerPadding = innerPadding,
                        onNavigate = { currentScreen = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarActions(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
) {
    when (currentScreen) {
        Screen.Home -> {
            IconButton(onClick = { onNavigate(Screen.Settings) }) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "設定",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Screen.Settings -> BackButton(onClick = { onNavigate(Screen.Home) })
        Screen.NotificationSettings -> BackButton(onClick = { onNavigate(Screen.Settings) })
        Screen.License -> BackButton(onClick = { onNavigate(Screen.Settings) })
    }
}

@Composable
private fun AppContent(
    currentScreen: Screen,
    innerPadding: PaddingValues,
    onNavigate: (Screen) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        when (currentScreen) {
            Screen.Home -> HomeScreen()

            Screen.Settings -> SettingsScreen(
                onNavigateToNotification = { onNavigate(Screen.NotificationSettings) },
                onNavigateToLicense = { onNavigate(Screen.License) },
            )

            Screen.NotificationSettings -> NotificationSettingsScreen()

            Screen.License -> {
                val aboutLibsJson = remember {
                    useResource("aboutlibraries.json") { it.bufferedReader().readText() }
                }
                LicenseScreen(aboutLibsJson = aboutLibsJson)
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "戻る",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
