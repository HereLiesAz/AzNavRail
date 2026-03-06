package com.hereliesaz.aznavrail.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HistoryManagerBenchmarkTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        HistoryManager.coroutineScope = CoroutineScope(Dispatchers.Unconfined)
    }

    @After
    fun teardown() {
        HistoryManager.resetForTesting()
    }

    @Test
    fun benchmarkLoadHistory() = runBlocking {
        // Setup a large history file
        HistoryManager.init(context, 1000)
        for (i in 1..1000) {
            HistoryManager.addEntry("benchmark_entry_$i", "benchmark_context")
        }

        // Run multiple iterations of loading
        var totalTime = 0L
        val iterations = 100 // Iterations

        for (i in 1..iterations) {
            // Reset in-memory state so loadHistory runs
            HistoryManager.resetForTesting()
            HistoryManager.init(context, 1000)

            val time = measureNanoTime {
                HistoryManager.getSuggestions("", "benchmark_context")
            }
            totalTime += time
        }

        println("Average load time over $iterations iterations: ${totalTime.toDouble() / iterations / 1_000_000.0} ms")
    }
}
