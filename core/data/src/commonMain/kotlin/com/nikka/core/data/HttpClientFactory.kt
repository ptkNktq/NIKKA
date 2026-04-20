package com.nikka.core.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/**
 * Discord Webhook 送信用の HttpClient を生成する。
 * CIO エンジンは pure Kotlin で動作するため、将来 KMP 化しても流用可能。
 */
internal fun defaultHttpClient(): HttpClient = HttpClient(CIO)

/** ktor の型を呼び出し側に露出させずに DiscordWebhookClient を生成するためのファクトリ。 */
fun createDiscordWebhookClient(): DiscordWebhookClient = DiscordWebhookClient(defaultHttpClient())
