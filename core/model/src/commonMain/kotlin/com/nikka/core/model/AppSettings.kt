package com.nikka.core.model

import kotlinx.serialization.Serializable

/** アプリ全般の表示・挙動設定 */
@Serializable
data class AppSettings(
    /** 週課が未達成でも、日課がすべて完了していればグループを自動で折りたたむ */
    val collapseOnDailyCompleted: Boolean = false,
)
