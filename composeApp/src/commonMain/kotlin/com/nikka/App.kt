package com.nikka

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikka.core.ui.theme.NikkaTheme
import com.nikka.di.appModule
import com.nikka.feature.home.HomeScreen
import org.koin.compose.KoinApplication

@Composable
fun App(
    topBar: @Composable (actions: @Composable () -> Unit) -> Unit = {},
) {
    KoinApplication(application = { modules(appModule) }) {
        NikkaTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                HomeScreen(topBar = topBar)
            }
        }
    }
}
