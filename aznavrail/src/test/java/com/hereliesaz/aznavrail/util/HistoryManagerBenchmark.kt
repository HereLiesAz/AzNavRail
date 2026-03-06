package com.hereliesaz.aznavrail.util

import org.junit.Test
import kotlin.system.measureNanoTime
import kotlin.random.Random

class HistoryManagerBenchmark {

    @Test
    fun benchmarkGetSuggestions() {
        val history = mutableListOf<String>()
        val random = Random(42)
        for (i in 0 until 10000) {
            val type = random.nextInt(10)
            when (type) {
                0 -> history.add("query something ${random.nextInt()}")
                1 -> history.add("something query ${random.nextInt()}")
                2 -> history.add("query")
                else -> history.add("random string ${random.nextInt()}")
            }
        }

        val query = "query"
        val maxSuggestions = 5

        // Warmup
        for(i in 0 until 1000) {
            original(history, query, maxSuggestions)
            optimized(history, query, maxSuggestions)
        }

        val origTime = measureNanoTime {
            for(i in 0 until 1000) {
                original(history, query, maxSuggestions)
            }
        }

        val optTime = measureNanoTime {
            for(i in 0 until 1000) {
                optimized(history, query, maxSuggestions)
            }
        }

        println("BENCHMARK Original time: ${origTime / 1000000.0} ms")
        println("BENCHMARK Optimized time: ${optTime / 1000000.0} ms")

        val origRes = original(history, query, maxSuggestions)
        val optRes = optimized(history, query, maxSuggestions)

        println("BENCHMARK Results match: ${origRes == optRes}")
    }

    private fun original(history: List<String>, query: String, maxSuggestions: Int): List<String> {
        val (startsWith, contains) = history
            .filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
            .partition { it.startsWith(query, ignoreCase = true) }

        return (startsWith + contains).take(maxSuggestions)
    }

    private fun optimized(history: List<String>, query: String, maxSuggestions: Int): List<String> {
        val startsWith = ArrayList<String>(maxSuggestions)
        val contains = ArrayList<String>(maxSuggestions)

        for (item in history) {
            if (item.equals(query, ignoreCase = true)) continue

            if (item.startsWith(query, ignoreCase = true)) {
                if (startsWith.size < maxSuggestions) {
                    startsWith.add(item)
                }
            } else if (contains.size < maxSuggestions && item.contains(query, ignoreCase = true)) {
                contains.add(item)
            }

            // Early exit if we have enough of both
            if (startsWith.size >= maxSuggestions && contains.size >= maxSuggestions) {
                break
            }
        }

        val result = ArrayList<String>(maxSuggestions)
        result.addAll(startsWith)

        val needed = maxSuggestions - result.size
        if (needed > 0) {
            result.addAll(contains.take(needed))
        }

        return result
    }
}
