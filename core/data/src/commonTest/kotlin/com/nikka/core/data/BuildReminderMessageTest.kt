package com.nikka.core.data

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildReminderMessageTest {

    @Test
    fun `message lists header and titles`() {
        val message = buildReminderMessage(
            prefix = null,
            header = "以下の日課が未達成です。",
            titles = listOf("デイリー任務", "樹脂消費"),
        )
        assertEquals("以下の日課が未達成です。\n- デイリー任務\n- 樹脂消費", message)
    }

    @Test
    fun `prefix is prepended on its own line`() {
        val message = buildReminderMessage(
            prefix = "<@123456789>",
            header = "以下の週課が未達成です。",
            titles = listOf("週ボス"),
        )
        assertEquals("<@123456789>\n以下の週課が未達成です。\n- 週ボス", message)
    }

    @Test
    fun `blank prefix is omitted`() {
        val message = buildReminderMessage(
            prefix = "   ",
            header = "以下の日課が未達成です。",
            titles = listOf("デイリー任務"),
        )
        assertEquals("以下の日課が未達成です。\n- デイリー任務", message)
    }
}
