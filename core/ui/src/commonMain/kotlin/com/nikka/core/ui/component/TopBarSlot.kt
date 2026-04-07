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
 *
 * ### 設計トレードオフ
 * `actions` を `mutableStateOf<@Composable () -> Unit>` で保持しているのは Compose の
 * 「親が子に content を渡す」標準スロット API とは逆向きの実装で、本来は推奨されない形である。
 * しかし NIKKA の TopBar は undecorated window の `WindowDraggableArea` で包む必要があり、
 * かつ各画面が自身の TopBar を所有するのは重複が大きいため、CompositionLocal による
 * 「子から親へのスロット登録」という変則パターンを意図的に採用している。
 *
 * この設計には以下のトレードオフがある:
 * - 初回コンポーズで親 (`topBar { slot.actions() }`) が先に走るため、子の登録は次のフレームで反映される
 *   （実用上は 1 フレームのため知覚しにくい）
 * - `@Composable` ラムダを `MutableState` に格納するため、`@Stable` 推論によるスキップ最適化は効きにくい
 *
 * 画面が増えて TopBar まわりの要件が複雑化したら、各画面が自前の Scaffold + TopAppBar を持つ
 * 標準パターンへの移行を検討すること。
 */
@Stable
class TopBarSlot {
    var actions: @Composable () -> Unit by mutableStateOf({})
        private set

    private var ownerToken: Any? = null

    /**
     * アクションを登録し、所有権トークンを返す。
     * 解除時はこのトークンを `clear` に渡すこと。
     *
     * 通常は [ProvideTopBarActions] 経由で使用すること。
     */
    internal fun set(actions: @Composable () -> Unit): Any {
        val token = Any()
        this.actions = actions
        this.ownerToken = token
        return token
    }

    /**
     * トークンが現所有者と一致する場合のみアクションを解除する。
     * 既に他の画面が `set` した後の場合は何もしない。
     */
    internal fun clear(token: Any) {
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
