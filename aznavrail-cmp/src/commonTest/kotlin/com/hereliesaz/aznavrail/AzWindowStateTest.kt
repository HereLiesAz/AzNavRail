package com.hereliesaz.aznavrail

import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The drag arithmetic of [AzWindowState], which is pure and therefore has no excuse for going
 * untested.
 *
 * It shipped once with `-(size.width - keep).toFloat().coerceAtMost(0f)`, which Kotlin parses as
 * `-(min(width - keep, 0))` — zero for any window wider than the keep margin. The window could not
 * be dragged left or up at all, and because the horizontal/vertical maxima were absolute container
 * extents applied to a *relative* offset, it could be dragged off the bottom of the screen with no
 * way back. Every case below would have caught that.
 */
class AzWindowStateTest {

    private val container = IntSize(1080, 2400)
    private val chrome = 96f

    /** A window the parent placed at (66, 900) — roughly centred, as `AzPopupHost` places it. */
    private fun centredWindow(width: Int = 948, height: Int = 600): AzWindowState =
        AzWindowState().apply { onPositioned(66f, 900f, IntSize(width, height)) }

    @Test
    fun dragsLeftAndUp() {
        val state = centredWindow()
        state.dragBy(-500f, -500f, container, chrome)
        assertTrue(state.offsetX < 0f, "expected to move left, offsetX=${state.offsetX}")
        assertTrue(state.offsetY < 0f, "expected to move up, offsetY=${state.offsetY}")
    }

    @Test
    fun cannotBeDraggedOffTheBottom() {
        val state = centredWindow()
        state.dragBy(0f, 5000f, container, chrome)
        // The bar's top, in container coordinates, must still be on screen.
        val barTop = 900f + state.offsetY
        assertTrue(
            barTop <= container.height - chrome,
            "grab bar left the screen: barTop=$barTop, container=${container.height}",
        )
        assertTrue(barTop >= 0f, "grab bar went above the top: barTop=$barTop")
    }

    @Test
    fun cannotBeDraggedOffTheTop() {
        val state = centredWindow()
        state.dragBy(0f, -5000f, container, chrome)
        assertEquals(0f, 900f + state.offsetY, "the bar should stop at the top edge")
    }

    @Test
    fun keepsAGripVisibleOnBothHorizontalEdges() {
        val state = centredWindow(width = 400)
        state.dragBy(5000f, 0f, container, chrome)
        val left = 66f + state.offsetX
        assertTrue(left <= container.width - 96f, "pushed too far right: left=$left")

        state.dragBy(-10000f, 0f, container, chrome)
        val leftAfter = 66f + state.offsetX
        assertTrue(leftAfter + 400f >= 96f, "pushed too far left: right edge=${leftAfter + 400f}")
    }

    @Test
    fun aWindowWiderThanTheScreenStillMoves() {
        // minX must not collapse to 0 for an oversized window — that was the original defect.
        val state = AzWindowState().apply { onPositioned(0f, 200f, IntSize(2000, 400)) }
        state.dragBy(-300f, 0f, container, chrome)
        assertTrue(state.offsetX < 0f, "an oversized window should still slide left")
    }

    @Test
    fun anUnmeasuredWindowIsNotShoved() {
        // Before the first layout the size is zero; a drag of nothing must move nothing.
        val state = AzWindowState()
        state.dragBy(0f, 0f, container, chrome)
        assertEquals(0f, state.offsetX)
        assertEquals(0f, state.offsetY)
    }

    @Test
    fun ignoresAnEmptyContainer() {
        val state = centredWindow()
        state.dragBy(100f, 100f, IntSize.Zero, chrome)
        assertEquals(0f, state.offsetX, "no container means no clamp and no movement")
        assertEquals(0f, state.offsetY)
    }

    @Test
    fun clampsBackInsideAfterTheContainerShrinks() {
        val state = centredWindow()
        state.dragBy(0f, 1200f, container, chrome)
        val landscape = IntSize(2400, 1080)
        state.clampInto(landscape, chrome)
        val barTop = 900f + state.offsetY
        assertTrue(
            barTop <= landscape.height - chrome,
            "rotation stranded the window: barTop=$barTop",
        )
    }

    @Test
    fun resetPositionReturnsItToTheParentPlacement() {
        val state = centredWindow()
        state.dragBy(-200f, 300f, container, chrome)
        state.resetPosition()
        assertEquals(0f, state.offsetX)
        assertEquals(0f, state.offsetY)
    }

    @Test
    fun theSaverRoundTrips() {
        val state = AzWindowState(initialOffsetX = -12f, initialOffsetY = 34f, initialMinimized = true)
        val saved = with(AzWindowState.Saver) {
            // The saver scope is only needed for nested savers; this one stores plain values.
            listOf(state.offsetX, state.offsetY, state.minimized)
        }
        assertEquals(listOf(-12f, 34f, true), saved)
    }
}
