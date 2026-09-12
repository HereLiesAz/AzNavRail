package com.hereliesaz.aznavrail.util

import org.junit.Test
import org.junit.Assert.assertEquals

class HistoryManagerUtf8Test {

    private fun String.utf8ByteLength(): Int {
        var count = 0
        var i = 0
        while (i < length) {
            val ch = this[i]
            if (ch.code <= 0x7F) {
                count++
            } else if (ch.code <= 0x7FF) {
                count += 2
            } else if (Character.isHighSurrogate(ch)) {
                count += 4
                i++ // Skip low surrogate
            } else {
                count += 3
            }
            i++
        }
        return count
    }

    @Test
    fun testUtf8Length() {
        val strings = listOf(
            "hello",
            "你好",
            "こんにちは",
            "👋🌍", // Emojis
            "A\u00A9\u2260\uD834\uDD1E", // 1 + 2 + 3 + 4 bytes
            ""
        )
        for (s in strings) {
            assertEquals("Failed for $s", s.toByteArray(Charsets.UTF_8).size, s.utf8ByteLength())
        }
    }
}
