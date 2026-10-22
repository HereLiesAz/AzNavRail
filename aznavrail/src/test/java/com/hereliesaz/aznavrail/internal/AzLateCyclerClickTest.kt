package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression: a rail cycler declared only after the rail's first composition (shown behind a
 * condition) never got its transient state, because the effect that seeds it was keyed on the
 * scope's item list — one mutable list instance, so the key never changed. Every tap on such a
 * cycler was dropped and its onClick never ran.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzLateCyclerClickTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a cycler added after the first composition still commits its taps`() {
        var showCycler by mutableStateOf(false)
        var selected by mutableStateOf("A")
        var clicks = 0

        composeTestRule.setContent {
            val navController = rememberNavController()
            // Read here, as an app reads its UI state, so a change recomposes the whole rail block.
            val show = showCycler
            val current = selected
            AzHostActivityLayout(navController = navController) {
                // As in a real app, the change that reveals the cycler also flips a highlight, which
                // is what recomposes the rail and lets it render the new item.
                azConfig(activeClassifiers = if (show) setOf("on") else emptySet())
                azRailItem(id = "anchor", text = "Anchor", classifiers = setOf("on"))
                if (show) {
                    azRailCycler(
                        id = "late",
                        options = listOf("A", "B", "C"),
                        selectedOption = current,
                        onClick = {
                            clicks++
                            selected = when (selected) { "A" -> "B"; "B" -> "C"; else -> "A" }
                        },
                    )
                }
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { showCycler = true }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("A").performClick()
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.waitForIdle()

        assertEquals(1, clicks)
        assertEquals("B", selected)
    }
}
