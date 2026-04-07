package com.hereliesaz.aznavrail

import org.junit.Test
import org.junit.Assert.assertTrue
import kotlin.system.measureTimeMillis

class AzNavRailScopeBenchmarkTest {

    @Test
    fun benchmarkAzRailRelocItem() {
        val scope = AzNavRailScopeImpl()

        val time = measureTimeMillis {
            for (i in 0 until 10000) {
                scope.azRailRelocItem(
                    id = "item_$i",
                    hostId = "host",
                    text = "Text $i"
                ) {
                    listItem("Action") {}
                    listItem("Action 2") {}
                    inputItem("Hint") {}
                }
            }
        }

        println("Time to run 10,000 azRailRelocItem calls: $time ms")

        assertTrue("Benchmark should take more than 0ms", time > 0)
    }
}
