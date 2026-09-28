package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/**
 * Desktop/JVM twin of the Android module's `AzUnattachedRelocDragGlitchTest`.
 *
 * Regression coverage for the "glitchy" unattached reloc drag: the drag offset was applied by a
 * `Modifier.offset` placed *before* the gesture's own `pointerInput`, so every pointer position the
 * gesture read was already shifted by the offset it had just applied. The item fed its own
 * displacement back into its delta and lagged / jittered behind the finger, and the drop index was
 * computed from that corrupted distance. These tests drag in many small steps with a frame between
 * each, which is what a real finger does and what exposes the feedback.
 */
class AzUnattachedRelocDragGlitchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var lastOrder: List<String>? = null
    private var relocateCalls = 0

    private fun setRail(anchor: AzUnattachedAnchor = AzUnattachedAnchor.OPPOSITE) {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(id = "host", text = "Host", anchor = anchor, initiallyExpanded = true)
                listOf("a", "b", "c", "d").forEach { id ->
                    azRailRelocItem(
                        id = id, hostId = "host", text = "Item $id",
                        onRelocate = { _, _, order -> relocateCalls++; lastOrder = order },
                    )
                }
                azUnattachedHostItem(id = "other", text = "Other", anchor = anchor, initiallyExpanded = true)
                azRailRelocItem(id = "x", hostId = "other", text = "Item x")
                onscreen { }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun top(id: String): Dp =
        composeTestRule.onNodeWithContentDescription("Item $id").getUnclippedBoundsInRoot().top

    /** Presses [id], holds past long-press, then moves by [totalPx] in [steps] frames. */
    private fun dragInSteps(id: String, totalPx: Float, steps: Int, release: Boolean = true) {
        val node = composeTestRule.onNodeWithContentDescription("Item $id")
        node.performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.waitForIdle()
        repeat(steps) {
            node.performTouchInput { moveBy(Offset(0f, totalPx / steps)); advanceEventTime(16) }
            composeTestRule.waitForIdle()
        }
        if (release) {
            node.performTouchInput { up() }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun `dragged item tracks the finger during an unattached drag`() {
        setRail()
        val pitchPx = with(composeTestRule.density) { (top("b") - top("a")).toPx() }
        val startTop = top("a")
        dragInSteps("a", pitchPx, steps = 10, release = false)
        val movedPx = with(composeTestRule.density) { (top("a") - startTop).toPx() }
        assertTrue(
            "Dragged item must follow the finger (moved ${movedPx}px for a ${pitchPx}px drag)",
            abs(movedPx - pitchPx) < 4f,
        )
        composeTestRule.onNodeWithContentDescription("Item a").performTouchInput { up() }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `step-wise drag of two slots drops two slots down and reports only this host's order once`() {
        setRail()
        val pitchPx = with(composeTestRule.density) { (top("b") - top("a")).toPx() }
        dragInSteps("a", pitchPx * 2, steps = 12)
        assertEquals(1, relocateCalls)
        assertEquals(listOf("b", "c", "a", "d"), lastOrder)
        // What the UI shows after the drop must match what was reported.
        assertTrue(top("b") < top("c") && top("c") < top("a") && top("a") < top("d"))
    }

    @Test
    fun `step-wise drag under FLOATING also lands where the finger went`() {
        setRail(AzUnattachedAnchor.FLOATING)
        val pitchPx = with(composeTestRule.density) { (top("b") - top("a")).toPx() }
        dragInSteps("d", -pitchPx * 3, steps = 15)
        assertEquals(listOf("d", "a", "b", "c"), lastOrder)
    }
}
