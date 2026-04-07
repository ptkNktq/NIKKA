package com.nikka.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 各画面が TopBar に差し込む画面固有アクションのスロット。
 * App ルートで生成し、CompositionLocal 経由で各画面が利用する。
 */
@Stable
class TopBarSlot {
    var actions: @Composable () -> Unit by mutableStateOf({})
        private set

    fun set(actions: @Composable () -> Unit) {
        this.actions = actions
    }

    fun clear() {
        this.actions = {}
    }
}

val LocalTopBarSlot = staticCompositionLocalOf<TopBarSlot> {
    error("TopBarSlot not provided. Wrap your composables with CompositionLocalProvider(LocalTopBarSlot provides ...)")
}
