package com.nikka.core.data

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DiscordWebhookClient internal constructor(
    private val httpClient: HttpClient,
) : AutoCloseable {

    @Serializable
    private data class WebhookPayload(val content: String)

    private val json = Json { encodeDefaults = true }

    @Suppress("TooGenericExceptionCaught")
    suspend fun send(webhookUrl: String, content: String): Result<Unit> = runCatching {
        val payload = json.encodeToString(WebhookPayload.serializer(), WebhookPayload(content))
        val response = httpClient.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        check(response.status.isSuccess()) {
            "Discord webhook failed: ${response.status} ${response.bodyAsText()}"
        }
    }

    override fun close() {
        httpClient.close()
    }
}
