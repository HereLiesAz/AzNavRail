package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Scroll state for the unfolded children of one top-level unattached host.
 *
 * The host button itself stays outside this viewport, fixed; only its sub-items (and, to any depth,
 * the sub-items of relocatable sub-hosts unfolded among them — one list) scroll.
 *
 * Scrolling only exists while the children overflow the height their slot is given (see
 * [azUnattachedChildScroll]). Without overflow [maxOffset] is 0, nothing is clipped and the
 * `scrollable` is disabled, so layout and gestures are exactly what they were before scrolling
 * existed.
 */
internal class UnattachedHostScroll {
    /** How far the children are scrolled, px. 0 = first child flush under the host. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** Largest valid [offset]: natural children height minus viewport height, or 0 when they fit. */
    var maxOffset by mutableFloatStateOf(0f)
        private set

    /** Whether the children overflow, i.e. whether the scroll gesture is live. */
    var canScroll by mutableStateOf(false)
        private set

    /** Viewport top/bottom in window coordinates, updated every layout pass. */
    var viewportTop = 0f
        private set
    var viewportBottom = 0f
        private set

    /** The unscrolled children content, for locating an item inside it. */
    internal var content: LayoutCoordinates? = null

    /** Coordinates of the selected/active items inside this list, by id. */
    internal val selected: MutableMap<String, LayoutCoordinates> = HashMap()

    /** Drag auto-scroll speed, px/s. Positive scrolls toward later children. 0 = idle. */
    var autoScrollSpeed by mutableFloatStateOf(0f)
        private set

    /** Told how far an auto-scroll step actually moved, so the dragged item can follow. */
    private var autoScrollListener: ((Float) -> Unit)? = null

    val scrollableState = ScrollableState { delta ->
        // Finger down (positive delta) reveals earlier children, i.e. lowers the offset.
        val old = offset
        val new = (old - delta).coerceIn(0f, maxOffset)
        offset = new
        old - new
    }

    /** Scrolls by [dy] px toward later children (negative = earlier). Returns the distance moved. */
    fun scrollByRaw(dy: Float): Float {
        val old = offset
        val new = (old + dy).coerceIn(0f, maxOffset)
        offset = new
        return new - old
    }

    internal fun onMeasured(naturalHeight: Int, viewportHeight: Int) {
        val over = (naturalHeight - viewportHeight).coerceAtLeast(0).toFloat()
        if (maxOffset != over) maxOffset = over
        if (offset > over) offset = over
        val scrolls = over > 0f
        if (canScroll != scrolls) canScroll = scrolls
    }

    internal fun onViewportPositioned(c: LayoutCoordinates) {
        val top = c.positionInWindow().y
        viewportTop = top
        viewportBottom = top + c.size.height
    }

    /**
     * Feeds the dragging finger's window-space y. Inside [edgePx] of the viewport's top or bottom
     * the list auto-scrolls toward that edge, faster the deeper the finger sits in the zone; every
     * step's actual travel goes to [onScrolled]. Outside the zones, or when nothing overflows, it
     * stops.
     */
    fun updateDragAutoScroll(fingerY: Float, edgePx: Float, maxSpeedPx: Float, onScrolled: (Float) -> Unit) {
        autoScrollListener = onScrolled
        val speed = when {
            !canScroll || edgePx <= 0f -> 0f
            fingerY < viewportTop + edgePx ->
                -maxSpeedPx * ((viewportTop + edgePx - fingerY) / edgePx).coerceIn(0f, 1f)
            fingerY > viewportBottom - edgePx ->
                maxSpeedPx * ((fingerY - (viewportBottom - edgePx)) / edgePx).coerceIn(0f, 1f)
            else -> 0f
        }
        if (autoScrollSpeed != speed) autoScrollSpeed = speed
    }

    fun stopDragAutoScroll() {
        autoScrollSpeed = 0f
        autoScrollListener = null
    }

    /**
     * Runs while [autoScrollSpeed] is non-zero, one step per frame. Ends by itself when the list
     * can move no further, so an idle list never holds a frame loop open.
     */
    suspend fun runAutoScroll() {
        var last = -1L
        while (autoScrollSpeed != 0f) {
            val moved = withFrameNanos { now ->
                val dt = if (last < 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                if (dt > 0f) scrollByRaw(autoScrollSpeed * dt.coerceAtMost(0.05f)) else Float.NaN
            }
            if (moved.isNaN()) continue
            if (moved == 0f) break
            autoScrollListener?.invoke(moved)
        }
    }

    /**
     * Brings the first attached selected item fully into view, unless the user has already scrolled.
     */
    suspend fun revealSelected() {
        val base = content ?: return
        if (!canScroll || offset != 0f || !base.isAttached) return
        val coords = selected.values.firstOrNull { it.isAttached } ?: return
        val y = base.localPositionOf(coords, Offset.Zero).y
        val h = coords.size.height
        val viewportH = viewportBottom - viewportTop
        val target = when {
            y < offset -> y
            y + h > offset + viewportH -> y + h - viewportH
            else -> return
        }.coerceIn(0f, maxOffset)
        scrollableState.animateScrollBy(-(target - offset))
    }
}

/**
 * Makes a column of unattached children scroll vertically when — and only when — its natural height
 * exceeds the height the parent offers it (a fixed anchor stack's padded window area, or the
 * `heightIn` cap a FLOATING rail gets from its position).
 *
 * The content is always measured unbounded; when it fits, it is placed at its natural size and
 * position with no clip and no active gesture, identical to having no modifier at all.
 *
 * [reservePx] is kept free below the viewport for the host buttons that follow this host in the same
 * anchor stack, so an overflowing host cannot push them out of the window.
 */
internal fun Modifier.azUnattachedChildScroll(state: UnattachedHostScroll, reservePx: Int = 0): Modifier = this
    .onGloballyPositioned { state.onViewportPositioned(it) }
    .scrollable(state.scrollableState, Orientation.Vertical, enabled = state.canScroll)
    // Clip only while overflowing, and as a layer clip rather than a draw clip: children scrolled
    // out of the viewport must not take touches either. Without overflow nothing is clipped, so a
    // lifted drag item can still be drawn past the list's ends as before.
    .graphicsLayer { clip = state.maxOffset > 0f }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
        val height = if (constraints.hasBoundedHeight) {
            placeable.height.coerceAtMost((constraints.maxHeight - reservePx).coerceAtLeast(0))
        } else {
            placeable.height
        }.coerceAtLeast(constraints.minHeight)
        state.onMeasured(placeable.height, height)
        layout(placeable.width, height) {
            placeable.place(0, -state.offset.roundToInt())
        }
    }
    .onGloballyPositioned { state.content = it }
