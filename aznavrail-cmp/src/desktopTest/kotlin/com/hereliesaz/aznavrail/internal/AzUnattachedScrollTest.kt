package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/**
 * Desktop/JVM twin of the Android module's `AzUnattachedScrollTest`.
 *
 * An expanded unattached host whose children run past the window scrolls them; one whose children
 * fit is left exactly as it was. Expected values are read off the screen or written literally, never
 * computed through the scroll code.
 */
class AzUnattachedScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val clicks = mutableListOf<String>()
    private val relocations = mutableListOf<Triple<Int, Int, List<String>>>()

    private fun setRail(
        count: Int,
        reloc: Boolean,
        anchor: AzUnattachedAnchor = AzUnattachedAnchor.OPPOSITE,
    ) {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azUnattachedHostItem(id = "host", text = "Host", anchor = anchor, initiallyExpanded = true)
                repeat(count) { i ->
                    if (reloc) {
                        azRailRelocItem(
                            id = "$i", hostId = "host", text = "Item $i",
                            onRelocate = { from, to, order -> relocations += Triple(from, to, order) },
                            onClick = { clicks += "$i" },
                        )
                    } else {
                        azRailSubItem(id = "$i", hostId = "host", text = "Item $i", onClick = { clicks += "$i" })
                    }
                }
                onscreen { }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun node(id: String) = composeTestRule.onNodeWithContentDescription("Item $id")
    private fun top(id: String): Dp = node(id).getUnclippedBoundsInRoot().top
    private fun px(d: Dp): Float = with(composeTestRule.density) { d.toPx() }
    private fun rootHeight(): Dp = composeTestRule.onRoot().getUnclippedBoundsInRoot().bottom

    /** The fixed OPPOSITE stack's lower limit: the window minus its 10% bottom safe inset. */
    private fun viewportLimit(): Dp = rootHeight() * 0.9f

    /**
     * How much of item [id] lies inside the scrolling viewport — below the host button and above
     * the window's bottom safe inset; 0 when scrolled out of it.
     */
    private fun visibleHeight(id: String): Dp {
        val b = node(id).getUnclippedBoundsInRoot()
        val viewportTop = composeTestRule.onNodeWithContentDescription("Host").getUnclippedBoundsInRoot().bottom
        return (minOf(b.bottom, viewportLimit()) - maxOf(b.top, viewportTop)).coerceAtLeast(0.dp)
    }

    /** A fast finger swipe across the host's list, in root coordinates relative to the host. */
    private fun swipeList(fromY: Dp, toY: Dp) {
        val host = composeTestRule.onNodeWithContentDescription("Host")
        val hb = host.getUnclippedBoundsInRoot()
        host.performTouchInput {
            val x = center.x
            swipe(Offset(x, px(fromY - hb.top)), Offset(x, px(toY - hb.top)), durationMillis = 250)
        }
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()
    }

    private fun scrollToEnd() {
        val h = rootHeight()
        repeat(8) { swipeList(h * 0.8f, h * 0.3f) }
    }

    @Test
    fun `an overflowing host scrolls and its last item becomes reachable and clickable`() {
        setRail(count = 30, reloc = false)
        val limit = viewportLimit()
        assertTrue("Last item should start below the window", top("29") > rootHeight())
        assertEquals(0f, visibleHeight("29").value, 0.5f)
        // The host button stays put while its children scroll.
        val hostTop = composeTestRule.onNodeWithContentDescription("Host").getUnclippedBoundsInRoot().top

        scrollToEnd()

        assertEquals(hostTop, composeTestRule.onNodeWithContentDescription("Host").getUnclippedBoundsInRoot().top)
        val last = node("29").getUnclippedBoundsInRoot()
        assertTrue("Last item ${last.bottom} should sit inside $limit", last.bottom <= limit + 1.dp)
        assertTrue(visibleHeight("29") > 20.dp)
        // Children never scroll up under the host button.
        assertTrue("Swipes must not click", clicks.isEmpty())

        node("29").performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertEquals(listOf("29"), clicks)
    }

    @Test
    fun `a host that fits does not scroll or clip`() {
        setRail(count = 3, reloc = false)
        val hostBounds = composeTestRule.onNodeWithContentDescription("Host").getUnclippedBoundsInRoot()
        val tops = listOf(top("0"), top("1"), top("2"))
        val pitch = tops[1] - tops[0]
        assertEquals(pitch.value, (tops[2] - tops[1]).value, 0.5f)
        // Same rhythm from the host to its first child as between children.
        assertEquals(pitch.value, (tops[0] - hostBounds.top).value, 0.5f)
        listOf("0", "1", "2").forEach { assertEquals(node(it).getUnclippedBoundsInRoot().let { b -> px(b.bottom - b.top) }, px(visibleHeight(it)), 1f) }

        swipeList(tops[2], tops[0] - 10.dp)
        assertEquals(tops, listOf(top("0"), top("1"), top("2")))
        assertTrue(clicks.isEmpty())
    }

    @Test
    fun `reloc drag after scrolling reports the right order`() {
        setRail(count = 30, reloc = true)
        scrollToEnd()
        val pitch = px(top("29") - top("28"))
        val n = node("29")
        n.performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.waitForIdle()
        repeat(10) {
            n.performTouchInput { moveBy(Offset(0f, -pitch / 10f)); advanceEventTime(16) }
            composeTestRule.waitForIdle()
        }
        n.performTouchInput { up() }
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()

        val expected = (0..27).map { "$it" } + listOf("29", "28")
        assertEquals(listOf(Triple(29, 28, expected)), relocations)
        assertTrue("29 at ${top("29")} should sit above 28 at ${top("28")}", top("29") < top("28"))
        assertEquals(emptyList<String>(), clicks)
    }

    @Test
    fun `dragging to the edge auto-scrolls so an item can land off-screen`() {
        setRail(count = 30, reloc = true)
        val limit = viewportLimit()
        val initiallyVisible = (0 until 30).count { top("$it") < limit }
        val n = node("0")
        n.performTouchInput { down(center) }
        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.waitForIdle()

        // Walk the finger down to just above the viewport's bottom edge.
        val start = n.getUnclippedBoundsInRoot()
        val targetY = px(limit - 12.dp)
        val startY = px((start.top + start.bottom) / 2)
        composeTestRule.mainClock.autoAdvance = false
        repeat(10) {
            n.performTouchInput { moveBy(Offset(0f, (targetY - startY) / 10f)); advanceEventTime(16) }
            composeTestRule.mainClock.advanceTimeByFrame()
        }
        // Hold it there; the list keeps scrolling without any further finger movement.
        repeat(180) { composeTestRule.mainClock.advanceTimeByFrame() }
        n.performTouchInput { up() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()

        assertEquals(1, relocations.size)
        val (from, to, order) = relocations.single()
        assertEquals(0, from)
        assertTrue("Landed at $to, but only $initiallyVisible items were visible", to >= initiallyVisible)
        assertEquals("0", order[to])
        assertEquals((0 until 30).map { "$it" }.toSet(), order.toSet())
        // Dropped where the finger was: its slot starts inside the viewport, not off-screen.
        assertTrue("Dropped item at ${top("0")} (landed $to) should be on screen, limit $limit", visibleHeight("0") > 0.dp)
    }

    @Test
    fun `a swipe scrolls without clicking, a tap still clicks`() {
        setRail(count = 30, reloc = true)
        val before = top("3")
        val h = rootHeight()
        // A swipe that starts on a reloc item scrolls: no click, no long-press drag, no relocation.
        swipeList(h * 0.7f, h * 0.4f)
        assertTrue("List should have scrolled", abs(px(top("3") - before)) > 20f)
        assertTrue(clicks.isEmpty())
        assertTrue(relocations.isEmpty())

        val visible = (0 until 30).first { visibleHeight("$it") > 40.dp && top("$it") > h * 0.3f }
        node("$visible").performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertEquals(listOf("$visible"), clicks)
        assertTrue(relocations.isEmpty())
    }

    @Test
    fun `FLOATING host scrolls inside the window too`() {
        setRail(count = 30, reloc = false, anchor = AzUnattachedAnchor.FLOATING)
        assertTrue(top("29") > rootHeight())
        scrollToEnd()
        assertTrue(node("29").getUnclippedBoundsInRoot().bottom <= rootHeight())
        assertTrue(visibleHeight("29") > 20.dp)
        node("29").performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertEquals(listOf("29"), clicks)
    }
}
