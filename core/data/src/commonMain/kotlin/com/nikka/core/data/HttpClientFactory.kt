package com.nikka.core.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

/**
 * Discord Webhook 送信用の HttpClient を生成する。
 * CIO エンジンは pure Kotlin で動作するため、将来 KMP 化しても流用可能。
 */
internal fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
    }
}

/** ktor の型を呼び出し側に露出させずに DiscordWebhookClient を生成するためのファクトリ。 */
fun createDiscordWebhookClient(): DiscordWebhookClient = DiscordWebhookClient(defaultHttpClient())

private const val REQUEST_TIMEOUT_MS = 10_000L
private const val CONNECT_TIMEOUT_MS = 5_000L
