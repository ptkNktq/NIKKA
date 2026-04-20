package com.nikka.core.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSettings(
    val enabled: Boolean = false,
    val webhookUrl: String = "",
    val message: String? = null,
    val hour: Int = DEFAULT_HOUR,
) {
    companion object {
        const val DEFAULT_HOUR = 21
        const val DEFAULT_MESSAGE = "本日の日課に未達成の項目があります。"
    }
}
