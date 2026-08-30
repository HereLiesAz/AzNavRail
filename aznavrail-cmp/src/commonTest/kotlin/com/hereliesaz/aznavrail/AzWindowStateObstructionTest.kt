package com.hereliesaz.aznavrail

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for [AzWindowState.clampInto]'s obstruction handling: a full-height obstruction
 * (a left/right-docked rail) must only ever narrow the window's X range, and a full-width
 * obstruction (a top/bottom-docked bar) must only ever narrow its Y range — even though a
 * full-height rail touches both `y=0` and `y=containerHeight` by construction, same as a
 * full-width bar touches both `x=0` and `x=containerWidth`.
 */
class AzWindowStateObstructionTest {

    private val containerSize = IntSize(1000, 800)
    private val chromeHeightPx = 40f
    private val windowSize = IntSize(200, 150)

    private fun freshWindow(x: Float = 500f, y: Float = 500f): AzWindowState {
        val state = AzWindowState(initialOffsetX = 0f, initialOffsetY = 0f)
        state.onPositioned(x, y, windowSize)
        return state
    }

    @Test
    fun leftDockedRail_onlyNarrowsX_leavesYUntouched() {
        val state = freshWindow(x = 500f, y = 500f)
        val leftRail = Rect(left = 0f, top = 0f, right = 80f, bottom = containerSize.height.toFloat())

        state.clampInto(containerSize, chromeHeightPx, listOf(leftRail))

        // The window was already clear of the rail and inside bounds, so nothing should move.
        assertEquals(500f, state.offsetX + state.anchorX, 0f)
        assertEquals(500f, state.offsetY + state.anchorY, 0f)
    }

    @Test
    fun leftDockedRail_pushesWindowClearHorizontally_withoutCorruptingY() {
        // Window sits under/overlapping the rail's X range.
        val state = freshWindow(x = 20f, y = 300f)
        val leftRail = Rect(left = 0f, top = 0f, right = 80f, bottom = containerSize.height.toFloat())

        state.clampInto(containerSize, chromeHeightPx, listOf(leftRail))

        val absX = state.offsetX + state.anchorX
        val absY = state.offsetY + state.anchorY
        assertEquals(80f, absX, 0f)
        // Bug regression: Y must remain wherever it was, never pinned to the container's full height.
        assertEquals(300f, absY, 0f)
    }

    @Test
    fun rightDockedRail_pushesWindowClearHorizontally_withoutCorruptingY() {
        val state = freshWindow(x = 850f, y = 300f)
        val rightRail = Rect(
            left = containerSize.width - 80f,
            top = 0f,
            right = containerSize.width.toFloat(),
            bottom = containerSize.height.toFloat(),
        )

        state.clampInto(containerSize, chromeHeightPx, listOf(rightRail))

        val absX = state.offsetX + state.anchorX
        val absY = state.offsetY + state.anchorY
        assertEquals(containerSize.width - 80f - windowSize.width, absX, 0f)
        assertEquals(300f, absY, 0f)
    }

    @Test
    fun topDockedBar_pushesWindowClearVertically_withoutCorruptingX() {
        val state = freshWindow(x = 300f, y = 10f)
        val topBar = Rect(left = 0f, top = 0f, right = containerSize.width.toFloat(), bottom = 60f)

        state.clampInto(containerSize, chromeHeightPx, listOf(topBar))

        val absX = state.offsetX + state.anchorX
        val absY = state.offsetY + state.anchorY
        assertEquals(300f, absX, 0f)
        assertEquals(60f, absY, 0f)
    }

    @Test
    fun fullScreenObstruction_narrowsBothAxes() {
        // A degenerate rect that is simultaneously full-height and full-width (covers the entire
        // container) must narrow both axes, not just whichever classification is checked first.
        val state = freshWindow(x = 300f, y = 300f)
        val fullScreen = Rect(
            left = 0f,
            top = 0f,
            right = containerSize.width.toFloat(),
            bottom = containerSize.height.toFloat(),
        )

        state.clampInto(containerSize, chromeHeightPx, listOf(fullScreen))

        val absX = state.offsetX + state.anchorX
        val absY = state.offsetY + state.anchorY
        // There is nowhere valid on either axis; both collapse to the single point the min/max
        // coercion converges on.
        assertEquals(containerSize.width.toFloat(), absX, 0f)
        assertEquals(containerSize.height.toFloat(), absY, 0f)
    }
}
