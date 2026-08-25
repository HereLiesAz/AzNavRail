package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * Regression coverage for [FloatingDockGroup]: every top-level `FLOATING`-anchored unattached host
 * now floats and docks independently (previously every `FLOATING` host shared a single draggable
 * stack), and two of them can be dragged flush against each other to form a group.
 */
@RunWith(RobolectricTestRunner::class)
class AzFloatingDockGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Declares two independent FLOATING hosts, each with its own reloc child. */
    private fun setTwoHostContent(onClickA: () -> Unit, onClickB: () -> Unit) {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(
                    id = "hostA", text = "A", anchor = AzUnattachedAnchor.FLOATING, initiallyExpanded = true,
                )
                azRailRelocItem(id = "itemA", hostId = "hostA", text = "Item A", onClick = onClickA)
                azUnattachedHostItem(
                    id = "hostB", text = "B", anchor = AzUnattachedAnchor.FLOATING, initiallyExpanded = true,
                )
                azRailRelocItem(id = "itemB", hostId = "hostB", text = "Item B", onClick = onClickB)
                onscreen { }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `item A stays clickable while a second independent FLOATING host coexists`() {
        var clickedA = false
        setTwoHostContent(onClickA = { clickedA = true }, onClickB = {})

        // `useUnmergedTree = true`: a reloc item has no semantics click action of its own (its tap is
        // detected via raw `pointerInput`, not `clickable`/`combinedClickable`).
        composeTestRule.onNodeWithContentDescription("Item A", useUnmergedTree = true).performTouchInput {
            down(center); advanceEventTime(60); up()
        }
        composeTestRule.waitForIdle()

        assertTrue("Item A should stay clickable with a second FLOATING host present", clickedA)
    }

    @Test
    fun `item B stays clickable while a second independent FLOATING host coexists`() {
        var clickedB = false
        setTwoHostContent(onClickA = {}, onClickB = { clickedB = true })

        composeTestRule.onNodeWithContentDescription("Item B", useUnmergedTree = true).performTouchInput {
            down(center); advanceEventTime(60); up()
        }
        composeTestRule.waitForIdle()

        assertTrue("Item B should stay clickable with a second FLOATING host present", clickedB)
    }

    @Test
    fun `dragging one FLOATING host flush against another docks them side by side`() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(
                    id = "hostA", text = "A", anchor = AzUnattachedAnchor.FLOATING, initiallyExpanded = false,
                )
                azUnattachedHostItem(
                    id = "hostB", text = "B", anchor = AzUnattachedAnchor.FLOATING, initiallyExpanded = false,
                )
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        val density = composeTestRule.density

        // Both hosts default to flush against the real screen edge (opposite the main rail) at the
        // top of the screen (`y == minY`). Move A away from every edge — horizontally AND
        // vertically — before docking B onto it: dragging B to sit flush against an edge-docked A
        // would need touch coordinates beyond the window's own bounds (which a synthetic touch
        // injection can't reliably target), and leaving A's y at the exact top-edge snap threshold
        // makes the B-onto-A drag below coincidentally ALSO look like a top-edge dock, racing against
        // the intended rail-to-rail attach depending on how much of the gesture touch-slop consumes.
        composeTestRule.onNodeWithContentDescription("A").performTouchInput {
            down(center)
            repeat(5) { i -> moveTo(center + Offset(-200f, 300f) * ((i + 1) / 5f)) }
            up()
        }
        composeTestRule.waitForIdle()

        val boundsA = composeTestRule.onNodeWithContentDescription("A").getUnclippedBoundsInRoot()
        val boundsB = composeTestRule.onNodeWithContentDescription("B").getUnclippedBoundsInRoot()

        // The delta needed to move B's top-left flush against A's right edge, well within the
        // rail-to-rail snap threshold. `performTouchInput`'s coordinate space is local to the node
        // (origin at B's own top-left as of gesture start), so drag by a DELTA rather than aiming at
        // an absolute root-space point.
        val deltaX = with(density) { (boundsA.right - boundsB.left).toPx() } + 4f
        val deltaY = with(density) { (boundsA.top - boundsB.top).toPx() }

        composeTestRule.onNodeWithContentDescription("B").performTouchInput {
            down(center)
            // Several incremental steps, not one big jump, so Compose's own touch-slop gesture
            // recognizer reliably registers this as a drag rather than a stray tap.
            repeat(5) { i ->
                val fraction = (i + 1) / 5f
                moveTo(center + Offset(deltaX * fraction, deltaY * fraction))
            }
            up()
        }
        composeTestRule.waitForIdle()

        // Docked: B's own content now starts immediately to the right of A's, not wherever it was
        // dropped on screen — its position is computed relative to A (see `resolvedPosition`).
        val newBoundsA = composeTestRule.onNodeWithContentDescription("A").getUnclippedBoundsInRoot()
        val newBoundsB = composeTestRule.onNodeWithContentDescription("B").getUnclippedBoundsInRoot()
        assertTrue(
            "Docked rail B should start right where A's stack ends, not floating independently",
            kotlin.math.abs(newBoundsB.left.value - newBoundsA.right.value) < 32f,
        )
    }
}
