package com.nikka.core.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSettings(
    val enabled: Boolean = false,
    val webhookUrl: String = "",
    val hour: Int = DEFAULT_HOUR,
    /** 通知メッセージの先頭に付ける文言 (メンション等)。null で付けない */
    val messagePrefix: String? = null,
) {
    companion object {
        const val DEFAULT_HOUR = 21
    }
}
