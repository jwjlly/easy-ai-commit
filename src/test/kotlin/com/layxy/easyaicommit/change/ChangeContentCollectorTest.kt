package com.layxy.easyaicommit.change

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ChangeContentCollectorTest {
    @Test
    fun `truncates large content to requested size and keeps both ends`() {
        val value = "A".repeat(120) + "B".repeat(120)
        val result = ChangeContentCollector.truncateMiddle(value, 100)

        assertEquals(100, result.length)
        assertContains(result, "[truncated]")
        assertEquals('A', result.first())
        assertEquals('B', result.last())
    }
}
