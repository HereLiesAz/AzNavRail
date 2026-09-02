package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression coverage for the hidden context menu being reloc-item-only: `azRailItem` had no
 * `hiddenMenu` parameter at all, and [DraggableRailItemWrapper]'s long-press-opens-menu gesture
 * only ever ran when `item.isRelocItem`, so a plain rail item's `hiddenMenuItems` (settable
 * directly on [com.hereliesaz.aznavrail.model.AzNavItem]) could never be opened. A hidden menu is
 * not a reloc-only affordance — any rail item may carry one.
 */
@RunWith(RobolectricTestRunner::class)
class RailItemHiddenMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `long-press on a plain rail item with a hidden menu opens it instead of clicking`() {
        var clicked = false
        var actionClicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailItem(id = "item1", text = "Item 1", onClick = { clicked = true }) {
                    listItem("Some action") { actionClicked = true }
                }
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Some action").assertExists()
        assertFalse(
            "A press that legitimately opens a hidden menu must not also fire the item's onClick.",
            clicked
        )

        composeTestRule.onNodeWithText("Some action").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Tapping the hidden menu's list item should still fire its own onClick.", actionClicked)
    }

    @Test
    fun `a quick tap on a plain rail item with a hidden menu still fires onClick, not the menu`() {
        var clicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailItem(id = "item1", text = "Item 1", onClick = { clicked = true }) {
                    listItem("Some action") {}
                }
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Item 1").performClick()
        composeTestRule.waitForIdle()

        assertTrue("An ordinary quick tap must still fire onClick, hidden menu or not.", clicked)
        composeTestRule.onNodeWithText("Some action").assertDoesNotExist()
    }

    @Test
    fun `long-press on a rail toggle with a hidden menu opens it`() {
        var actionClicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailToggle(id = "toggle1", isChecked = false, toggleOnText = "On", toggleOffText = "Off") {
                    listItem("Reset") { actionClicked = true }
                }
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Off").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Reset").assertExists()
        composeTestRule.onNodeWithText("Reset").performClick()
        composeTestRule.waitForIdle()

        assertTrue("The hidden menu wired onto azRailToggle should be reachable and clickable.", actionClicked)
    }

    @Test
    fun `long-press on a rail host item with a hidden menu opens it`() {
        var actionClicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailHostItem(id = "host1", text = "Host") {
                    listItem("Rename") { actionClicked = true }
                }
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Host").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rename").assertExists()
        composeTestRule.onNodeWithText("Rename").performClick()
        composeTestRule.waitForIdle()

        assertTrue("The hidden menu wired onto azRailHostItem should be reachable and clickable.", actionClicked)
    }
}
