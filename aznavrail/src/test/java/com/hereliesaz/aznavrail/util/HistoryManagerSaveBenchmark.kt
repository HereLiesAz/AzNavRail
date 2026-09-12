package com.hereliesaz.aznavrail.util

import org.junit.Test
import kotlin.system.measureNanoTime
import kotlin.random.Random

class HistoryManagerSaveBenchmark {

    @Test
    fun benchmarkSaveSizeCalculation() {
        val entries = mutableListOf<String>()
        val random = Random(42)
        for (i in 0 until 1000) {
            entries.add("random string ${random.nextInt()} with some more text to make it longer")
        }

        val maxSizeBytes = 5120
        val lineSeparator = System.lineSeparator()

        // Warmup
        for(i in 0 until 1000) {
            original(entries, maxSizeBytes, lineSeparator)
            optimized(entries, maxSizeBytes, lineSeparator)
        }

        val origTime = measureNanoTime {
            for(i in 0 until 1000) {
                original(entries, maxSizeBytes, lineSeparator)
            }
        }

        val optTime = measureNanoTime {
            for(i in 0 until 1000) {
                optimized(entries, maxSizeBytes, lineSeparator)
            }
        }

        println("BENCHMARK Original time: ${origTime / 1000000.0} ms")
        println("BENCHMARK Optimized time: ${optTime / 1000000.0} ms")

        val origRes = original(entries, maxSizeBytes, lineSeparator)
        val optRes = optimized(entries, maxSizeBytes, lineSeparator)
        println("BENCHMARK Results match: ${origRes == optRes}")
    }

    private fun original(entriesToWrite: List<String>, maxSizeBytes: Int, lineSeparator: String): Int {
        var currentSize = 0
        var count = 0
        val lineSeparatorSize = lineSeparator.toByteArray(Charsets.UTF_8).size
        for (entry in entriesToWrite) {
            if (maxSizeBytes == 0) break
            val entrySize = entry.toByteArray(Charsets.UTF_8).size + lineSeparatorSize
            if (currentSize + entrySize <= maxSizeBytes) {
                currentSize += entrySize
                count++
            } else {
                break
            }
        }
        return count
    }

    // utf8 length logic
    private fun utf8ByteLength(s: String): Int {
        var count = 0
        for (i in 0 until s.length) {
            val ch = s[i]
            if (ch.code <= 0x7F) {
                count++
            } else if (ch.code <= 0x7FF) {
                count += 2
            } else if (Character.isHighSurrogate(ch)) {
                count += 4
                // skip next
            } else if (Character.isLowSurrogate(ch)) {
                // already counted
            } else {
                count += 3
            }
        }
        return count
    }

    private fun optimized(entriesToWrite: List<String>, maxSizeBytes: Int, lineSeparator: String): Int {
        var currentSize = 0
        var count = 0
        val lineSeparatorSize = utf8ByteLength(lineSeparator)
        for (entry in entriesToWrite) {
            if (maxSizeBytes == 0) break
            val entrySize = utf8ByteLength(entry) + lineSeparatorSize
            if (currentSize + entrySize <= maxSizeBytes) {
                currentSize += entrySize
                count++
            } else {
                break
            }
        }
        return count
    }
}
