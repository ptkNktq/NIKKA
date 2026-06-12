package com.nikka.core.data

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildReminderMessageTest {

    @Test
    fun `message lists header and group names`() {
        val message = buildReminderMessage(
            prefix = null,
            header = "以下の日課が未達成です。",
            groupNames = listOf("原神", "スターレイル"),
        )
        assertEquals("以下の日課が未達成です。\n- 原神\n- スターレイル", message)
    }

    @Test
    fun `prefix is prepended on its own line`() {
        val message = buildReminderMessage(
            prefix = "<@123456789>",
            header = "以下の週課が未達成です。",
            groupNames = listOf("原神"),
        )
        assertEquals("<@123456789>\n以下の週課が未達成です。\n- 原神", message)
    }

    @Test
    fun `blank prefix is omitted`() {
        val message = buildReminderMessage(
            prefix = "   ",
            header = "以下の日課が未達成です。",
            groupNames = listOf("原神"),
        )
        assertEquals("以下の日課が未達成です。\n- 原神", message)
    }
}
