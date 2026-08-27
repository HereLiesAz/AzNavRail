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
 * Regression coverage for the bug where a relocatable item (`azRailRelocItem`) in the docked rail
 * swallowed a tap outright if the finger stayed down past the system long-press timeout without
 * moving: [DraggableRailItemWrapper]'s gesture armed `isLongPress` purely from dwell time, and its
 * `finally` block only ever dispatched a click when `!isLongPress` — so a slightly slow (but
 * perfectly stationary) tap fell into neither the "click" branch nor the "drag" branch and did
 * nothing at all, even though the item had no hidden menu to show for the long-press either. A
 * plain [androidx.compose.ui.test.longClick] reproduces exactly this: press, hold past the
 * long-press timeout, release without moving.
 */
@RunWith(RobolectricTestRunner::class)
class RailRelocItemLongPressClickTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `holding a reloc item with no hidden menu past the long-press timeout still fires its onClick`() {
        var clicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailHostItem(id = "host", text = "Host", initiallyExpanded = true)
                azRailRelocItem(id = "item1", hostId = "host", text = "Item 1", onClick = { clicked = true })
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        // A held-but-stationary press must still register as a tap, not vanish silently.
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        assertTrue(
            "Holding a reloc item with no hidden menu past the long-press timeout (without " +
                "dragging) must still fire its onClick, exactly as an ordinary quick tap would.",
            clicked
        )
    }

    @Test
    fun `holding a reloc item with a hidden menu past the long-press timeout opens the menu instead of clicking`() {
        var clicked = false
        var actionClicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailHostItem(id = "host", text = "Host", initiallyExpanded = true)
                azRailRelocItem(id = "item1", hostId = "host", text = "Item 1", onClick = { clicked = true }) {
                    listItem("Some action") { actionClicked = true }
                }
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // The hidden menu opens instead of the plain click firing — the two must never both fire
        // for the same press.
        composeTestRule.onNodeWithText("Some action").assertExists()
        assertFalse(
            "A press that legitimately opens a hidden menu must not also fire the item's onClick.",
            clicked
        )

        composeTestRule.onNodeWithText("Some action").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Tapping the hidden menu's list item should still fire its own onClick.", actionClicked)
    }
}
