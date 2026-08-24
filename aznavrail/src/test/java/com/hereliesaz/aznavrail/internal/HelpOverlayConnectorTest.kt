package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic coverage for [computeConnectorElbow] — the routing behind the Help overlay's
 * item-to-card connector lines. No Compose runtime/Robolectric needed: the function only touches
 * plain [Rect]/[Offset] value types.
 */
class HelpOverlayConnectorTest {

    private fun rect(left: Float, top: Float, width: Float, height: Float) =
        Rect(left, top, left + width, top + height)

    @Test
    fun `starts at the item's right edge and ends at the card's left edge`() {
        val item = rect(left = 0f, top = 100f, width = 60f, height = 60f)
        val card = rect(left = 200f, top = 500f, width = 300f, height = 150f)

        val points = computeConnectorElbow(
            itemBounds = item,
            cardBounds = card,
            gutterStartX = item.right,
            gutterEndX = card.left,
            laneFraction = 0.5f,
        )

        assertEquals(Offset(item.right, item.center.y), points.first())
        assertEquals(Offset(card.left, card.center.y), points.last())
    }

    @Test
    fun `vertical segment never leaves the gutter, however far the card sits from the item`() {
        val gutterStart = 60f
        val gutterEnd = 200f
        val item = rect(left = 0f, top = 40f, width = gutterStart, height = 60f)

        // A card sitting far below its item (the exact geometry that made the old straight
        // diagonal sweep through every intervening card once items and cards diverge in height).
        val farCard = rect(left = gutterEnd, top = 4000f, width = 300f, height = 150f)

        val points = computeConnectorElbow(
            itemBounds = item,
            cardBounds = farCard,
            gutterStartX = gutterStart,
            gutterEndX = gutterEnd,
            laneFraction = 0.5f,
        )

        // Every point's x must stay within [gutterStart, gutterEnd] — i.e. never past gutterEnd,
        // which is where card content begins, so the line can never be drawn over a card.
        points.forEach { p ->
            assertTrue("x=${p.x} should be >= gutterStart=$gutterStart", p.x >= gutterStart - 1e-3f)
            assertTrue("x=${p.x} should be <= gutterEnd=$gutterEnd", p.x <= gutterEnd + 1e-3f)
        }
    }

    @Test
    fun `laneFraction spreads the vertical segment across the gutter width`() {
        val item = rect(left = 0f, top = 0f, width = 60f, height = 60f)
        val card = rect(left = 220f, top = 300f, width = 300f, height = 150f)

        val near = computeConnectorElbow(item, card, gutterStartX = 60f, gutterEndX = 220f, laneFraction = 0.1f)
        val mid = computeConnectorElbow(item, card, gutterStartX = 60f, gutterEndX = 220f, laneFraction = 0.5f)
        val far = computeConnectorElbow(item, card, gutterStartX = 60f, gutterEndX = 220f, laneFraction = 0.9f)

        // The corner points (index 1/2) hold the lane x; a larger laneFraction must produce a
        // strictly larger lane x, so distinct items fan out to distinct verticals in the gutter.
        val nearLaneX = near[1].x
        val midLaneX = mid[1].x
        val farLaneX = far[1].x
        assertTrue(nearLaneX < midLaneX)
        assertTrue(midLaneX < farLaneX)
    }

    @Test
    fun `a degenerate (too-narrow or inverted) gutter clamps instead of inverting`() {
        val item = rect(left = 0f, top = 0f, width = 200f, height = 60f)
        // gutterEndX < gutterStartX: the cards viewport starts before the item even ends (e.g. an
        // extremely narrow screen). The lane must not go negative-width or throw.
        val card = rect(left = 150f, top = 500f, width = 300f, height = 150f)

        val points = computeConnectorElbow(
            itemBounds = item,
            cardBounds = card,
            gutterStartX = 200f,
            gutterEndX = 150f,
            laneFraction = 0.5f,
        )

        // Clamped to a single point rather than an inverted range.
        assertEquals(200f, points[1].x, 1e-3f)
        assertEquals(200f, points[2].x, 1e-3f)
    }

    @Test
    fun `laneFraction is clamped to 0 and 1 outside that range`() {
        val item = rect(left = 0f, top = 0f, width = 60f, height = 60f)
        val card = rect(left = 220f, top = 300f, width = 300f, height = 150f)

        val belowZero = computeConnectorElbow(item, card, gutterStartX = 60f, gutterEndX = 220f, laneFraction = -5f)
        val atZero = computeConnectorElbow(item, card, gutterStartX = 60f, gutterEndX = 220f, laneFraction = 0f)
        val aboveOne = computeConnectorElbow(item, card, gutterStartX = 60f, gutterEndX = 220f, laneFraction = 5f)
        val atOne = computeConnectorElbow(item, card, gutterStartX = 60f, gutterEndX = 220f, laneFraction = 1f)

        assertEquals(atZero[1].x, belowZero[1].x, 1e-3f)
        assertEquals(atOne[1].x, aboveOne[1].x, 1e-3f)
        assertEquals(60f, atZero[1].x, 1e-3f)
        assertEquals(220f, atOne[1].x, 1e-3f)
    }
}
