package com.hereliesaz.aznavrail

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AzNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBackgroundWeightSortingInHostActivityLayout() {
        val compositionOrder = mutableListOf<String>()

        composeTestRule.setContent {
            val navController = rememberNavController()

            // AzHostActivityLayout is the root screen container defined in AzNavHost.kt
            // It provides the AzNavHostScope for the builder/DSL and evaluates background weights.
            // Testing this DSL requires setting up this layout scope which takes ~20 lines.
            AzHostActivityLayout(
                navController = navController,
                currentDestination = "home"
            ) {
                background(weight = 10) {
                    SideEffect { compositionOrder.add("bg10") }
                }
                background(weight = 1) {
                    SideEffect { compositionOrder.add("bg1") }
                }
                background(weight = 5) {
                    SideEffect { compositionOrder.add("bg5") }
                }
            }
        }

        // AzHostActivityLayout correctly sorts the backgrounds by their weight property
        // before composing them, so weight 1 -> 5 -> 10 should be composed in that order.
        assertEquals(listOf("bg1", "bg5", "bg10"), compositionOrder)
    }

    @Test
    fun testRecompositionWithChangingContentDoesNotDuplicateIds() {
        // Regression: AzNavRailScopeImpl.reset() must clear globalIdSet so that
        // recomposing AzHostActivityLayout with a state-gated content lambda
        // doesn't see leftover IDs from the previous pass and throw
        // IllegalArgumentException("Duplicate ID detected: ...").
        val showConditional = mutableStateOf(true)

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(
                navController = navController,
                currentDestination = "home"
            ) {
                if (showConditional.value) {
                    azRailHostItem(id = "conditional.host", text = "C", route = "c")
                }
                azRailHostItem(id = "stable.host", text = "S", route = "s")
            }
        }

        composeTestRule.runOnIdle { showConditional.value = false }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { showConditional.value = true }
        composeTestRule.waitForIdle()
    }
}
