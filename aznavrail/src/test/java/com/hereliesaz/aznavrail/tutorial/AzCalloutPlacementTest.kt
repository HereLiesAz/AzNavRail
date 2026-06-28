package com.hereliesaz.aznavrail.tutorial

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the non-blocking callout placement: near the target, off obstacles, inside the safe area. */
class AzCalloutPlacementTest {

    private val safe = Rect(0f, 0f, 1000f, 2000f)
    private val size = Size(240f, 100f)

    @Test
    fun `overlapArea computes the intersection`() {
        assertEquals(25f, overlapArea(Rect(0f, 0f, 10f, 10f), Rect(5f, 5f, 15f, 15f)))
        assertEquals(0f, overlapArea(Rect(0f, 0f, 10f, 10f), Rect(20f, 20f, 30f, 30f)))
    }

    @Test
    fun `prefers below the target when there is room`() {
        val target = Rect(400f, 400f, 600f, 500f)
        val r = placeCallout(target, size, obstacles = emptyList(), safe = safe)
        assertEquals(400f, r.left)
        assertEquals(508f, r.top) // target.bottom + gap(8)
    }

    @Test
    fun `never covers the target`() {
        val target = Rect(400f, 400f, 600f, 500f)
        val r = placeCallout(target, size, obstacles = emptyList(), safe = safe)
        assertEquals(0f, overlapArea(r, target))
    }

    @Test
    fun `flips above when below would leave the safe area`() {
        val target = Rect(400f, 1850f, 600f, 1950f)
        val r = placeCallout(target, size, obstacles = emptyList(), safe = safe)
        assertEquals(1742f, r.top) // target.top - height - gap
        assertTrue(r.bottom <= safe.bottom)
        assertEquals(0f, overlapArea(r, target))
    }

    @Test
    fun `avoids an obstacle blocking the preferred side`() {
        val target = Rect(400f, 400f, 600f, 500f)
        val obstacleBelow = Rect(380f, 500f, 680f, 700f)
        val r = placeCallout(target, size, obstacles = listOf(obstacleBelow), safe = safe)
        assertEquals(0f, overlapArea(r, obstacleBelow))
        assertEquals(0f, overlapArea(r, target))
    }

    @Test
    fun `stays fully inside the safe area`() {
        val target = Rect(950f, 50f, 999f, 99f) // hard against the right edge
        val r = placeCallout(target, size, obstacles = emptyList(), safe = safe)
        assertTrue(r.left >= safe.left && r.right <= safe.right)
        assertTrue(r.top >= safe.top && r.bottom <= safe.bottom)
    }

    @Test
    fun `untargeted callouts spread off each other`() {
        val placed = Rect(0f, 0f, 240f, 100f) // a callout already placed top-left
        val r = placeCallout(target = null, size = size, obstacles = listOf(placed), safe = safe)
        assertEquals(0f, overlapArea(r, placed))
        assertTrue(r.left >= safe.left && r.right <= safe.right && r.top >= safe.top && r.bottom <= safe.bottom)
    }
}
