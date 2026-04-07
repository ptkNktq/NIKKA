package com.nikka.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 各画面が TopBar に差し込む画面固有アクションのスロット。
 * App ルートで生成し、CompositionLocal 経由で各画面が利用する。
 *
 * **単一所有者前提**：このスロットは「現在表示中の単一画面が固有アクションを差し込む」用途に
 * 限定される。複数画面が同時に `set` するシナリオ（ネスト Composable から同時登録など）には
 * 対応していない。後勝ちで上書きされ、他者の登録は失われる。
 *
 * 所有権はトークン方式で管理する。`set` が返したトークンを `clear` に渡すことで、
 * 「自分が登録したアクションだけを解除する」を保証する。
 *
 * 通常は直接 `set/clear` を呼ばず、[ProvideTopBarActions] を経由すること。
 * リコンポーズ時のスタールクロージャを `rememberUpdatedState` で吸収する。
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

/**
 * 画面が TopBar に固有アクションを差し込むためのヘルパー。
 *
 * `rememberUpdatedState` で最新のラムダを参照するため、リコンポーズで `actions` の中身が
 * 変わってもスロット側は同じラムダ参照を保持したまま、内部で常に最新版を呼び出せる。
 * 画面の Composable が破棄されると自動的にスロットがクリアされる。
 */
@Composable
fun ProvideTopBarActions(actions: @Composable () -> Unit) {
    val slot = LocalTopBarSlot.current
    val currentActions by rememberUpdatedState(actions)
    DisposableEffect(slot) {
        val token = slot.set { currentActions() }
        onDispose { slot.clear(token) }
    }
}
