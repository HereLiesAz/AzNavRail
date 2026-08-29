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
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun `holding a reloc item with no hidden menu past the long-press timeout still fires its onClick`() {
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

        // A held-but-stationary press must still register as a tap, not vanish silently — see
        // `RailRelocItemLongPressClickTest` for the equivalent bug in the docked rail's own gesture.
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        assertTrue(
            "Holding a reloc item with no hidden menu past the long-press timeout (without " +
                "dragging) must still fire its onClick, exactly as an ordinary quick tap would.",
            clicked
        )
    }

    @Test
    fun `a realistically-timed quick tap fires onClick under the OPPOSITE anchor`() {
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

        // A realistically-timed down/up pair (well under the long-press timeout), driven through
        // performTouchInput rather than the synthetic performClick(), to exercise the same
        // awaitEachGesture code path a real quick tap does.
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput {
            down(center)
            advanceEventTime(60)
            up()
        }
        composeTestRule.waitForIdle()

        assertTrue("A realistically-timed quick tap should fire onClick under OPPOSITE", clicked)
    }

    @Test
    fun `a realistically-timed quick tap fires onClick under the FLOATING anchor`() {
        var clicked = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(
                    id = "host",
                    text = "Host",
                    anchor = AzUnattachedAnchor.FLOATING,
                    initiallyExpanded = true,
                )
                azRailRelocItem(id = "item1", hostId = "host", text = "Item 1", onClick = { clicked = true })
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        // Same as above, but under FLOATING, which wraps the whole stack in an additional
        // `detectDragGestures` pointerInput for repositioning — verifying that ancestor gesture
        // detector does not swallow a plain tap on a reloc item nested inside it.
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput {
            down(center)
            advanceEventTime(60)
            up()
        }
        composeTestRule.waitForIdle()

        assertTrue("A realistically-timed quick tap should fire onClick under FLOATING", clicked)
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

    @Test
    fun `long-press dragging a reloc item reorders it inside a FLOATING unattached host`() {
        var relocatedFrom: Int? = null
        var relocatedTo: Int? = null
        var orderAfterDrop: List<String>? = null

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(
                    id = "host",
                    text = "Host",
                    anchor = AzUnattachedAnchor.FLOATING,
                    initiallyExpanded = true,
                )
                azRailRelocItem(
                    id = "item1",
                    hostId = "host",
                    text = "Item 1",
                    onRelocate = { from, to, order ->
                        relocatedFrom = from
                        relocatedTo = to
                        orderAfterDrop = order
                    },
                )
                azRailRelocItem(id = "item2", hostId = "host", text = "Item 2")
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, 96f))
            advanceEventTime(32)
            up()
        }
        composeTestRule.waitForIdle()

        assertNotNull("A long-press drag under FLOATING must invoke onRelocate", orderAfterDrop)
        assertNotNull("Relocation must report its source index", relocatedFrom)
        assertNotNull("Relocation must report its destination index", relocatedTo)
        assertEquals(relocatedFrom!! + 1, relocatedTo)
        val order = orderAfterDrop!!
        assertTrue(
            "Dragged item must land after its sibling in the emitted order; got $order",
            order.indexOf("item2") < order.indexOf("item1"),
        )
    }

    @Test
    fun `dragging a reloc item immediately after selecting it reorders under OPPOSITE without any hold`() {
        var relocatedFrom: Int? = null
        var relocatedTo: Int? = null

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(
                    id = "host",
                    text = "Host",
                    anchor = AzUnattachedAnchor.OPPOSITE,
                    initiallyExpanded = true,
                )
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

        // Step 1: an ordinary quick tap selects "Item 1".
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput {
            down(center)
            advanceEventTime(60)
            up()
        }
        composeTestRule.waitForIdle()

        // Step 2: a second press on the now-selected item drags it immediately, well under the
        // system long-press timeout, and must still reorder it.
        composeTestRule.onNodeWithContentDescription("Item 1").performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(0f, 96f))
            advanceEventTime(16)
            up()
        }
        composeTestRule.waitForIdle()

        assertNotNull(
            "A tap-then-immediate-drag on a selected reloc item must reorder it under OPPOSITE",
            relocatedFrom,
        )
        assertEquals(relocatedFrom!! + 1, relocatedTo)
    }

    @Test
    fun `dragging a not-yet-selected reloc item immediately does not reorder it under FLOATING`() {
        var relocated = false

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(
                    id = "host",
                    text = "Host",
                    anchor = AzUnattachedAnchor.FLOATING,
                    initiallyExpanded = true,
                )
                azRailRelocItem(id = "item1", hostId = "host", text = "Item 1", onRelocate = { _, _, _ -> relocated = true })
                azRailRelocItem(id = "item2", hostId = "host", text = "Item 2")
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        // Nothing has selected "Item 1" yet, so an immediate drag (well under the long-press
        // timeout) must not reorder it — this is the FLOATING host's own drag detector's chance to
        // win instead, exactly as it did before the reloc item was ever selected.
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
