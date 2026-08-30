package com.hereliesaz.aznavrail

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression tests for [AzWindowState.offscreenFraction] and [AzWindowState.overlapsObstruction] —
 * the two checks `AzWindow`'s abandonment-timer effect uses to decide, once a dragged window has
 * sat untouched for its grace period, whether to pull it back onscreen or dismiss it outright.
 */
class AzWindowStateAbandonmentTest {

    private val containerSize = IntSize(1000, 800)
    private val windowSize = IntSize(200, 150)

    private fun freshWindow(x: Float, y: Float): AzWindowState {
        val state = AzWindowState(initialOffsetX = 0f, initialOffsetY = 0f)
        state.onPositioned(x, y, windowSize)
        return state
    }

    @Test
    fun fullyOnscreen_hasZeroOffscreenFraction() {
        val state = freshWindow(x = 400f, y = 300f)
        assertEquals(0f, state.offscreenFraction(containerSize), 0f)
    }

    @Test
    fun halfOffTheRightEdge_reportsHalfOffscreen() {
        // Window is 200 wide; parked so exactly half (100px) hangs off the right edge.
        val state = freshWindow(x = containerSize.width - 100f, y = 300f)
        assertEquals(0.5f, state.offscreenFraction(containerSize), 0.001f)
    }

    @Test
    fun entirelyBeyondTheEdge_isFullyOffscreen() {
        // No overlap with the container at all.
        val state = freshWindow(x = containerSize.width + 50f, y = 300f)
        assertEquals(1f, state.offscreenFraction(containerSize), 0f)
    }

    @Test
    fun mostlyOffscreen_clearsTheNearlyGoneThreshold() {
        // Only a 10px sliver (5% of the 200px width) remains onscreen — "basically completely off
        // screen" by the 0.9 threshold `AzWindow`'s abandonment timer dismisses at.
        val state = freshWindow(x = containerSize.width - 10f, y = 300f)
        assertTrue(state.offscreenFraction(containerSize) >= 0.9f)
    }

    @Test
    fun halfOffscreen_doesNotClearTheNearlyGoneThreshold() {
        val state = freshWindow(x = containerSize.width - 100f, y = 300f)
        assertTrue(state.offscreenFraction(containerSize) < 0.9f)
    }

    @Test
    fun overlapsObstruction_trueWhenWindowSitsOnTheRail() {
        val state = freshWindow(x = 20f, y = 300f)
        val leftRail = Rect(left = 0f, top = 0f, right = 80f, bottom = containerSize.height.toFloat())
        assertTrue(state.overlapsObstruction(listOf(leftRail)))
    }

    @Test
    fun overlapsObstruction_falseWhenClearOfTheRail() {
        val state = freshWindow(x = 500f, y = 300f)
        val leftRail = Rect(left = 0f, top = 0f, right = 80f, bottom = containerSize.height.toFloat())
        assertFalse(state.overlapsObstruction(listOf(leftRail)))
    }

    @Test
    fun overlapsObstruction_falseWithNoObstructions() {
        val state = freshWindow(x = 20f, y = 300f)
        assertFalse(state.overlapsObstruction(emptyList()))
    }
}
