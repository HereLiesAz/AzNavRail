package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
 * Desktop/JVM twin of the Android module's `AzUnattachedRelocSubHostDragTest`.
 *
 * A relocatable sub-host (`azRailRelocSubHostItem`) under an unattached host: one reloc slot
 * together with its descendants. Layout under test, top-first:
 *
 * ```
 * host
 *   a
 *   g        (relocatable sub-host, expanded)
 *     g1
 *     g2
 *   b
 *   c
 * ```
 *
 * Expected orders below are written out literally, never derived through `RelocItemHandler`.
 */
class AzUnattachedRelocSubHostDragTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val parentOrders = mutableListOf<List<String>>()
    private val groupOrders = mutableListOf<List<String>>()
    private val expansions = mutableListOf<Boolean>()
    private var recompose by mutableIntStateOf(0)

    private fun setRail(anchor: AzUnattachedAnchor = AzUnattachedAnchor.OPPOSITE) {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                @Suppress("UNUSED_VARIABLE") val tick = recompose
                val onParent: (Int, Int, List<String>) -> Unit = { _, _, o -> parentOrders += o }
                val onGroup: (Int, Int, List<String>) -> Unit = { _, _, o -> groupOrders += o }
                azUnattachedHostItem(id = "host", text = "Host", anchor = anchor, initiallyExpanded = true)
                azRailRelocItem(id = "a", hostId = "host", text = "Item a", onRelocate = onParent)
                azRailRelocSubHostItem(
                    id = "g", hostId = "host", text = "Item g",
                    initiallyExpanded = true, onRelocate = onParent,
                    onExpandedChange = { expansions += it },
                )
                azRailRelocItem(id = "g1", hostId = "g", text = "Item g1", onRelocate = onGroup)
                azRailRelocItem(id = "g2", hostId = "g", text = "Item g2", onRelocate = onGroup)
                azRailRelocItem(id = "b", hostId = "host", text = "Item b", onRelocate = onParent)
                azRailRelocItem(id = "c", hostId = "host", text = "Item c", onRelocate = onParent)
                onscreen { }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun top(id: String): Dp =
        composeTestRule.onNodeWithContentDescription("Item $id").getUnclippedBoundsInRoot().top

    private fun px(d: Dp): Float = with(composeTestRule.density) { d.toPx() }

    /** Presses [id], holds past long-press, then moves by [totalPx] in [steps] frames. */
    private fun dragInSteps(id: String, totalPx: Float, steps: Int = 12, release: Boolean = true) {
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

    private fun assertTopFirst(vararg ids: String) {
        val tops = ids.map { top(it) }
        for (i in 0 until tops.size - 1) {
            assertTrue("Expected ${ids.toList()} top-first, got tops $tops", tops[i] < tops[i + 1])
        }
    }

    @Test
    fun `initial layout stacks the group's children under it, between its siblings`() {
        setRail()
        assertTopFirst("a", "g", "g1", "g2", "b", "c")
    }

    @Test
    fun `dragging a relocatable host moves its whole block and reports the parent's order once`() {
        setRail()
        // One slot down: past b, whose pitch is the distance from b's top to c's top.
        val pitchB = px(top("c") - top("b"))
        val g1Offset = px(top("g1") - top("g"))
        dragInSteps("g", pitchB, release = false)
        // Mid-drag the children ride with the host.
        assertTrue(abs(px(top("g1") - top("g")) - g1Offset) < 2f)
        composeTestRule.onNodeWithContentDescription("Item g").performTouchInput { up() }
        composeTestRule.waitForIdle()

        assertEquals(listOf(listOf("a", "b", "g", "c")), parentOrders)
        assertTrue(groupOrders.isEmpty())
        assertTopFirst("a", "b", "g", "g1", "g2", "c")
    }

    @Test
    fun `a reloc item dragged past a host jumps over the whole block`() {
        setRail()
        // The block's pitch: from g's top to b's top (g + g1 + g2 + gaps).
        val blockPitch = px(top("b") - top("g"))
        dragInSteps("a", blockPitch)
        assertEquals(listOf(listOf("g", "a", "b", "c")), parentOrders)
        assertTopFirst("g", "g1", "g2", "a", "b", "c")
    }

    @Test
    fun `dragging upward past a host also jumps the whole block`() {
        setRail()
        val blockPitch = px(top("b") - top("g"))
        dragInSteps("b", -blockPitch)
        assertEquals(listOf(listOf("a", "b", "g", "c")), parentOrders)
        assertTopFirst("a", "b", "g", "g1", "g2", "c")
    }

    @Test
    fun `children stay under their host across moves and recomposition`() {
        setRail()
        dragInSteps("g", px(top("c") - top("b")))        // a b g c
        dragInSteps("c", -px(top("c") - top("g")))       // a b c g
        assertEquals(listOf(listOf("a", "b", "g", "c"), listOf("a", "b", "c", "g")), parentOrders)
        assertTopFirst("a", "b", "c", "g", "g1", "g2")

        // The DSL re-runs in declaration order; the saved order must restore the host's position
        // with its children still under it.
        recompose++
        composeTestRule.waitForIdle()
        assertTopFirst("a", "b", "c", "g", "g1", "g2")

        // Reordering inside the group reports only the group's own members and leaves the parent alone.
        dragInSteps("g1", px(top("g2") - top("g1")))
        assertEquals(listOf(listOf("g2", "g1")), groupOrders)
        assertEquals(2, parentOrders.size)
        assertTopFirst("a", "b", "c", "g", "g2", "g1")
    }

    @Test
    fun `a dropped host that did not move reports nothing`() {
        setRail()
        // Past touch slop (so it is a drag, not a slow tap) but well short of 40% of b's slot.
        dragInSteps("g", px(top("c") - top("b")) * 0.25f, steps = 4)
        assertTrue(parentOrders.isEmpty())
        assertTopFirst("a", "g", "g1", "g2", "b", "c")
    }

    @Test
    fun `tapping a relocatable host still toggles its children`() {
        setRail()
        composeTestRule.onNodeWithContentDescription("Item g").performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithContentDescription("Item g1").assertCountEquals(0)
        // Past the double-tap window, so the second press is a fresh tap.
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithContentDescription("Item g").performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertEquals(listOf(false, true), expansions)
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()
        assertTopFirst("a", "g", "g1", "g2", "b", "c")
    }

    @Test
    fun `FLOATING host drag moves the block too`() {
        setRail(AzUnattachedAnchor.FLOATING)
        dragInSteps("g", px(top("c") - top("b")))
        assertEquals(listOf(listOf("a", "b", "g", "c")), parentOrders)
        assertTopFirst("a", "b", "g", "g1", "g2", "c")
    }
}
