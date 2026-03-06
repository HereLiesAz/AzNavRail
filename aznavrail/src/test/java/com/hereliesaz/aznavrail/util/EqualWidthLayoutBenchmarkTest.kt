package com.hereliesaz.aznavrail.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class EqualWidthLayoutBenchmarkTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun benchmarkEqualWidthLayout() {
        val iterations = 1000
        val time = measureTimeMillis {
            composeTestRule.setContent {
                for (i in 0..iterations) {
                    EqualWidthLayout(verticalSpacing = 8.dp) {
                        Box(modifier = Modifier.size(50.dp, 20.dp))
                        Box(modifier = Modifier.size(100.dp, 20.dp))
                        Box(modifier = Modifier.size(75.dp, 20.dp))
                    }
                }
            }
            composeTestRule.waitForIdle()
        }
        println("Benchmark EqualWidthLayout took: $time ms")
    }
}
