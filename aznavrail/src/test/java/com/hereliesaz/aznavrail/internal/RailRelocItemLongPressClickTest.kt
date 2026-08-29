package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun `dragging a reloc item immediately after selecting it reorders without any hold`() {
        var relocatedFrom: Int? = null
        var relocatedTo: Int? = null

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailHostItem(id = "host", text = "Host", initiallyExpanded = true)
                azRailRelocItem(
                    id = "item1",
                    hostId = "host",
                    text = "Item 1",
                    onRelocate = { from, to, _ -> relocatedFrom = from; relocatedTo = to },
                )
                azRailRelocItem(id = "item2", hostId = "host", text = "Item 2")
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        // Step 1: an ordinary quick tap selects "Item 1" (same tap that would fire its onClick).
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput {
            down(center)
            advanceEventTime(60)
            up()
        }
        composeTestRule.waitForIdle()

        // Step 2: a SECOND press on the now-selected item drags it immediately — well under the
        // system long-press timeout — and must still reorder it. The hold-then-drag contract only
        // ever applied to the FIRST (selecting) press.
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(0f, 96f))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        assertNotNull("A tap-then-immediate-drag on a selected reloc item must reorder it", relocatedFrom)
        assertEquals(relocatedFrom!! + 1, relocatedTo)
    }

    @Test
    fun `dragging a not-yet-selected reloc item immediately does not reorder it`() {
        var relocated = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailHostItem(id = "host", text = "Host", initiallyExpanded = true)
                azRailRelocItem(id = "item1", hostId = "host", text = "Item 1", onRelocate = { _, _, _ -> relocated = true })
                azRailRelocItem(id = "item2", hostId = "host", text = "Item 2")
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        // Nothing has selected "Item 1" yet, so an immediate drag (well under the long-press
        // timeout) must NOT reorder it — the item must be selected first. This is what keeps this
        // fast path from racing the hidden-menu's own long-press on a fresh press.
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(0f, 96f))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        assertFalse("An unselected reloc item must not drag-reorder on a quick, un-held press", relocated)
    }
}
