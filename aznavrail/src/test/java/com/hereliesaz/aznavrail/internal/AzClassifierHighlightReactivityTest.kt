package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression coverage for the bug where an item made "selected" via
 * `azConfig(activeClassifiers = setOf(...))` only appeared highlighted while the finger was
 * physically pressing it. This test flips `activeClassifiers` through ordinary Compose snapshot
 * state — no touch interaction at all — and asserts the item's `selected` semantics (now reported
 * by [com.hereliesaz.aznavrail.AzNavRailButton], added alongside this test) update on a plain
 * recomposition, for both a rail-strip item and a relocatable item under an `azUnattachedHostItem`.
 */
@RunWith(RobolectricTestRunner::class)
class AzClassifierHighlightReactivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a plain rail item reflects activeClassifiers without any press`() {
        var active by mutableStateOf(false)

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azConfig(activeClassifiers = if (active) setOf("hot") else emptySet())
                azRailItem(id = "item1", text = "Item 1", classifiers = setOf("hot"))
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Item 1").assertIsNotSelected()

        composeTestRule.runOnIdle { active = true }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Item 1").assertIsSelected()

        composeTestRule.runOnIdle { active = false }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Item 1").assertIsNotSelected()
    }

    @Test
    fun `a reloc item under an unattached host reflects activeClassifiers without any press`() {
        var active by mutableStateOf(false)

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azConfig(activeClassifiers = if (active) setOf("hot") else emptySet())
                azUnattachedHostItem(
                    id = "host",
                    text = "Host",
                    anchor = AzUnattachedAnchor.OPPOSITE,
                    initiallyExpanded = true,
                )
                azRailRelocItem(id = "item1", hostId = "host", text = "Item 1", classifiers = setOf("hot"))
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Item 1").assertIsNotSelected()

        composeTestRule.runOnIdle { active = true }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Item 1").assertIsSelected()

        composeTestRule.runOnIdle { active = false }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Item 1").assertIsNotSelected()
    }
}
