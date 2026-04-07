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
 *
 * 所有権はトークン方式で管理する。`set` が返したトークンを `clear` に渡すことで、
 * 「自分が登録したアクションだけを解除する」を保証する。
 *
 * これにより、画面遷移時に Compose の DisposableEffect が
 *   1. 新画面で `set`（新しいアクションを登録）
 *   2. 旧画面の `onDispose` で `clear`（旧画面が登録した分のみ解除）
 * の順序で実行されても、新画面のアクションが誤って消されない。
 */
@Stable
class TopBarSlot {
    var actions: @Composable () -> Unit by mutableStateOf({})
        private set

    private var ownerToken: Any? = null

    /**
     * アクションを登録し、所有権トークンを返す。
     * 解除時はこのトークンを `clear` に渡すこと。
     */
    fun set(actions: @Composable () -> Unit): Any {
        val token = Any()
        this.actions = actions
        this.ownerToken = token
        return token
    }

    /**
     * トークンが現所有者と一致する場合のみアクションを解除する。
     * 既に他の画面が `set` した後の場合は何もしない。
     */
    fun clear(token: Any) {
        if (this.ownerToken === token) {
            this.actions = {}
            this.ownerToken = null
        }
    }
}

val LocalTopBarSlot = staticCompositionLocalOf<TopBarSlot> {
    error("TopBarSlot not provided. Wrap your composables with CompositionLocalProvider(LocalTopBarSlot provides ...)")
}
