package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression coverage for the bug where a relocatable item (`azRailRelocItem`) declared under an
 * `azUnattachedHostItem` was completely unclickable: [RailContent] unconditionally nulls a reloc
 * item's `onClick`, expecting an externally-supplied `dragModifier` to detect the tap instead — and
 * `AzUnattachedRail`'s `UnattachedNode` never supplied one, so the item rendered (correctly styled,
 * correctly badged) but no gesture was ever wired to it.
 */
@RunWith(RobolectricTestRunner::class)
class AzUnattachedRelocItemClickTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tapping a reloc item under an expanded unattached host fires its onClick`() {
        var clicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(
                    id = "host",
                    text = "Host",
                    anchor = AzUnattachedAnchor.OPPOSITE,
                    initiallyExpanded = true,
                )
                azRailRelocItem(id = "item1", hostId = "host", text = "Item 1", onClick = { clicked = true })
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Item 1").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Tapping a reloc item under an expanded unattached host should fire its onClick", clicked)
    }

    @Test
    fun `long-press on a reloc item under an unattached host opens its hidden menu`() {
        var actionClicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(
                    id = "host",
                    text = "Host",
                    anchor = AzUnattachedAnchor.OPPOSITE,
                    initiallyExpanded = true,
                )
                azRailRelocItem(id = "item1", hostId = "host", text = "Item 1") {
                    listItem("Some action") { actionClicked = true }
                }
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        // The hidden menu is not shown until the long-press fires.
        composeTestRule.onNodeWithText("Some action").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Some action").assertExists()

        composeTestRule.onNodeWithText("Some action").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Tapping the hidden menu's list item should fire its own onClick", actionClicked)
    }
}
