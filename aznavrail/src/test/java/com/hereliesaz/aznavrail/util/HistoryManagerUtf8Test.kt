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
                if (i + 1 < length && Character.isLowSurrogate(this[i + 1])) {
                    count += 4
                    i++ // Skip low surrogate
                } else {
                    count++ // Unpaired high surrogate replaced with '?'
                }
            } else if (Character.isLowSurrogate(ch)) {
                count++ // Unpaired low surrogate replaced with '?'
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
            "",
            "\uD83D", // Unpaired high surrogate
            "\uDC00", // Unpaired low surrogate
            "\uD83D\u0020", // High surrogate followed by ASCII
            "\uD83D\uD83D", // High surrogate followed by High surrogate
            "\uDC00\uDC00", // Low surrogate followed by Low surrogate
            "\uD83D\uDC00\uD83D", // Pair then high
            "\uD83D\uDC00\uDC00" // Pair then low
        )
        for (s in strings) {
            assertEquals("Failed for $s", s.toByteArray(Charsets.UTF_8).size, s.utf8ByteLength())
        }
    }
}
