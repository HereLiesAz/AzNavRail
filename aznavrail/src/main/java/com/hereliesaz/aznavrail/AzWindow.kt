package com.hereliesaz.aznavrail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Position and folded/unfolded state of one [AzWindow].
 *
 * Hoist it when the window's placement has to outlive the window — a hidden menu that reopens where
 * the user left it, a panel that stays minimized between visits.
 */
@Stable
class AzWindowState(
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
    initialMinimized: Boolean = false,
) {
    /** Horizontal displacement in px from wherever the window's parent placed it. */
    var offsetX: Float by mutableStateOf(initialOffsetX)
        internal set

    /** Vertical displacement in px from wherever the window's parent placed it. */
    var offsetY: Float by mutableStateOf(initialOffsetY)
        internal set

    /** True when the window is folded up to its title bar. */
    var minimized: Boolean by mutableStateOf(initialMinimized)

    /** Measured size of the whole window, used to keep drags inside the screen. */
    internal var size: IntSize by mutableStateOf(IntSize.Zero)

    /**
     * Whether this window has ever been pulled fully onscreen. Set the first time [AzWindow] learns
     * the window's real position and size, so a window that opens off-screen (e.g. a long-press menu
     * placed beside whatever raised it, near a screen edge) is clamped fully into view immediately —
     * not only after the user finds and drags it. Unlike [settleFullyOnscreen] itself, this only ever
     * fires once per window instance; later container-size changes are handled by [clampInto] instead,
     * which preserves an intentional off-screen park.
     */
    internal var hasSettledInitial: Boolean = false

    /**
     * Where the parent placed the window's top-left, in container coordinates, *before* this
     * state's offset is applied.
     *
     * The offset alone cannot be clamped: it is a displacement, and a displacement says nothing
     * about where the window actually is. A window the parent centred and a window the parent
     * pinned to a corner need completely different limits for the same offset. So the window
     * reports its real position after every layout ([onPositioned]) and the drag maths works in
     * absolute coordinates, converting back to an offset only at the end.
     */
    internal var anchorX: Float = 0f
        private set
    internal var anchorY: Float = 0f
        private set

    /** Records the window's measured size and the position its parent gave it. */
    internal fun onPositioned(xInContainer: Float, yInContainer: Float, measured: IntSize) {
        size = measured
        // Where it sits now, minus what we moved it by, is where the parent put it.
        anchorX = xInContainer - offsetX
        anchorY = yInContainer - offsetY
    }

    /**
     * Moves the window directly to the given offset — the same coordinate space as
     * [AzWindowState]'s `initialOffsetX`/`initialOffsetY` constructor params (a displacement from
     * wherever the window's parent placed it), and the same space [offsetX]/[offsetY] report.
     *
     * Unlike [dragBy] this is not clamped against screen bounds or an obstruction: it is meant for a
     * host that already knows exactly where it wants the window (restoring a saved position,
     * snapping it beside another panel), not for live gesture input. A host that wants the move
     * clamped too can follow it with [clampInto].
     */
    fun moveTo(x: Float, y: Float) {
        offsetX = x
        offsetY = y
    }

    /**
     * Moves the window by a drag delta, keeping its grab bar on screen and reachable.
     *
     * Horizontally the window may be pushed off either edge until only [MIN_VISIBLE_PX] of it
     * remains. Vertically the *bar* is the limit rather than the window: it lives along the top
     * edge, so letting the top scroll past the bottom of the container would leave nothing to grab
     * and no way back.
     *
     * @param obstructions Additional rects (in the same container coordinates as [bounds], e.g. a
     *   docked nav rail's own strip, or several sibling rails/toolbars) the window is kept clear of,
     *   on top of [bounds] itself. Only honoured on the edge(s) a given rect actually touches — one
     *   docked to the left edge pushes the window's minimum X inward, one docked to the top pushes
     *   its minimum Y down, and so on; a rect touching no edge of [bounds] is ignored, since there is
     *   no side to define "clear of it" from.
     */
    internal fun dragBy(dx: Float, dy: Float, bounds: IntSize, chromeHeightPx: Float, obstructions: List<Rect> = emptyList()) {
        if (bounds.width <= 0 || bounds.height <= 0) return
        val keep = MIN_VISIBLE_PX.toFloat()
        val width = size.width.toFloat()

        // Absolute position this drag is asking for.
        val targetX = anchorX + offsetX + dx
        val targetY = anchorY + offsetY + dy

        // Horizontal: at least `keep` px of the window stays inside on both sides.
        val minX = (keep - width).coerceAtMost(0f)
        val maxX = (bounds.width - keep).coerceAtLeast(minX)
        // Vertical: the whole bar stays inside, top and bottom.
        val minY = 0f
        val maxY = (bounds.height - chromeHeightPx).coerceAtLeast(minY)

        val (clampedMinX, clampedMaxX, clampedMinY, clampedMaxY) =
            narrowForObstruction(obstructions, bounds, width, chromeHeightPx, minX, maxX, minY, maxY)

        offsetX = targetX.coerceIn(clampedMinX, clampedMaxX) - anchorX
        offsetY = targetY.coerceIn(clampedMinY, clampedMaxY) - anchorY
    }

    /**
     * Pulls the window back inside [bounds] (and clear of [obstructions]) after the container changed
     * size — a rotation, a split-screen resize, a desktop window drag. Without this a window parked
     * near the bottom in portrait is simply gone in landscape, bar and all.
     */
    internal fun clampInto(bounds: IntSize, chromeHeightPx: Float, obstructions: List<Rect> = emptyList()) {
        if (bounds.width <= 0 || bounds.height <= 0) return
        dragBy(0f, 0f, bounds, chromeHeightPx, obstructions)
    }

    /**
     * Pulls the window **fully** inside [bounds] (and clear of [obstructions]) — not merely the
     * [MIN_VISIBLE_PX] sliver [dragBy] guarantees while a drag is in progress. Meant to be called
     * once a drag settles (see [AzWindow]'s `snapFullyOnscreen` param) or the first time the window's
     * real position is known (its initial placement), not on every frame of a drag in progress —
     * always enforcing full containment mid-drag would make it impossible to park a window mostly
     * off-screen on purpose.
     */
    internal fun settleFullyOnscreen(bounds: IntSize, chromeHeightPx: Float, obstructions: List<Rect> = emptyList()) {
        if (bounds.width <= 0 || bounds.height <= 0) return
        val width = size.width.toFloat()
        val height = size.height.toFloat()

        val targetX = anchorX + offsetX
        val targetY = anchorY + offsetY

        val minX = 0f
        val maxX = (bounds.width - width).coerceAtLeast(minX)
        val minY = 0f
        val maxY = (bounds.height - height).coerceAtLeast(minY)

        val (clampedMinX, clampedMaxX, clampedMinY, clampedMaxY) =
            narrowForObstruction(obstructions, bounds, width, height, minX, maxX, minY, maxY)

        offsetX = targetX.coerceIn(clampedMinX, clampedMaxX) - anchorX
        offsetY = targetY.coerceIn(clampedMinY, clampedMaxY) - anchorY
    }

    /**
     * Narrows an already-computed `(minX, maxX, minY, maxY)` window bound so the window also stays
     * clear of every rect in [obstructions]. Each obstruction is classified first — a full-height
     * strip (spanning from `y=0` to `y=bounds.height`, e.g. a left/right-docked rail) only narrows
     * the X range, and a full-width strip (spanning `x=0` to `x=bounds.width`, e.g. a top/bottom
     * toolbar) only narrows the Y range. Testing the four edges independently instead would treat a
     * full-height rail — which touches both `y=0` and `y=bounds.height` by construction, regardless
     * of which side it's docked on — as also constraining Y, corrupting the window's vertical bound.
     * A rect that is neither a full-height nor a full-width strip is ignored, since there is no
     * single side to define "clear of it" from. A degenerate rect that is *both* (i.e. it covers
     * the entire container) narrows both axes. [verticalExtent] is the window's own extent along
     * the axis checked against a full-width obstruction — the full window height from
     * [settleFullyOnscreen], just the chrome bar's height from [dragBy] (which only guarantees the
     * bar, not the whole window, stays clear).
     */
    private fun narrowForObstruction(
        obstructions: List<Rect>,
        bounds: IntSize,
        width: Float,
        verticalExtent: Float,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float,
    ): DragBounds {
        if (obstructions.isEmpty()) return DragBounds(minX, maxX, minY, maxY)
        var newMinX = minX
        var newMaxX = maxX
        var newMinY = minY
        var newMaxY = maxY
        for (obstruction in obstructions) {
            val isFullHeight = obstruction.top <= 0f && obstruction.bottom >= bounds.height
            val isFullWidth = obstruction.left <= 0f && obstruction.right >= bounds.width
            // Independent ifs, not else-if: a degenerate obstruction that is both a full-height
            // and a full-width strip (i.e. it covers the entire container) needs both axes
            // narrowed, not just whichever classification happened to be checked first.
            if (isFullHeight) {
                if (obstruction.left <= 0f) newMinX = newMinX.coerceAtLeast(obstruction.right)
                if (obstruction.right >= bounds.width) newMaxX = newMaxX.coerceAtMost(obstruction.left - width)
            }
            if (isFullWidth) {
                if (obstruction.top <= 0f) newMinY = newMinY.coerceAtLeast(obstruction.bottom)
                if (obstruction.bottom >= bounds.height) newMaxY = newMaxY.coerceAtMost(obstruction.top - verticalExtent)
            }
        }
        // A rail wider/taller than the container (or a degenerate rect) can invert the range;
        // collapse to a single point rather than leave min > max for `coerceIn` to throw on.
        if (newMinX > newMaxX) newMaxX = newMinX
        if (newMinY > newMaxY) newMaxY = newMinY
        return DragBounds(newMinX, newMaxX, newMinY, newMaxY)
    }

    private data class DragBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

    /** Returns the window to where its parent put it. */
    fun resetPosition() {
        offsetX = 0f
        offsetY = 0f
    }

    companion object {
        private const val MIN_VISIBLE_PX = 96

        internal val Saver: Saver<AzWindowState, List<Any>> = Saver(
            save = { listOf(it.offsetX, it.offsetY, it.minimized) },
            restore = {
                AzWindowState(
                    initialOffsetX = it[0] as Float,
                    initialOffsetY = it[1] as Float,
                    initialMinimized = it[2] as Boolean,
                )
            },
        )
    }
}

/** Creates an [AzWindowState] that survives recomposition and configuration changes. */
@Composable
fun rememberAzWindowState(
    initialMinimized: Boolean = false,
): AzWindowState = rememberSaveable(saver = AzWindowState.Saver) {
    AzWindowState(initialMinimized = initialMinimized)
}

/**
 * The library's floating window: a bordered panel with a grab bar, drawn in the rail's own colours.
 *
 * Every panel this library floats over an app — a popup, a hidden menu, anything a developer wants
 * to put in front of the user — is one of these, and they all behave the same way:
 *
 *  - **It moves.** Drag the bar and the window follows, clamped so it can never be lost off-screen.
 *    A panel that lands on top of the thing you needed to read is otherwise a dead end.
 *  - **It minimizes.** Tap the bar's fold control and the window collapses to just that bar, still
 *    where you left it, still one tap from coming back. That is the difference between getting a
 *    panel out of the way and having to throw it away and re-open it later.
 *  - **It closes**, when the caller gave it a way to.
 *
 * @param modifier Applied to the window surface (place it with the parent's alignment/padding).
 * @param title Shown in the grab bar. Blank draws a bare bar — right when the body already has a
 *   heading of its own.
 * @param state Position and minimized state; hoist it to make either outlive the window.
 * @param accent Border and chrome colour. Defaults to the rail's accent, so a window matches the
 *   rail that raised it.
 * @param surfaceColor Panel fill. Defaults to the theme surface.
 * @param movable Whether the grab bar drags the window.
 * @param minimizable Whether the grab bar offers the fold control.
 * @param onDismiss Close handler; null draws no close control.
 * @param obstruction An additional rect (in window/container px coordinates — e.g. from
 *   [androidx.compose.ui.layout.positionInWindow] on the obstruction's own layout) the window is
 *   kept clear of, on top of the screen bounds it is already clamped to. Meant for chrome this
 *   library itself draws on top of the window, such as a docked [AzNavRail] — pass a rect covering
 *   the rail's own gutter (`AzNavHostScope.dockingSide` + `AzNavHostScope.railWidth` are enough to
 *   build one) so the window can never land or be dragged fully underneath it. Only the edge(s) the
 *   rect touches are enforced; see [AzWindowState.dragBy]. For more than one rect (several sibling
 *   rails/toolbars) use [obstructions] instead — both are honoured together.
 * @param obstructions Like [obstruction] but for any number of rects at once — e.g. every other
 *   docked rail/toolbar on screen the window should never land or be dragged underneath.
 * @param snapFullyOnscreen When true, releasing a drag pulls the window **fully** back inside the
 *   screen (and clear of [obstruction]/[obstructions]) instead of only guaranteeing the
 *   [AzWindowState]-internal 96px sliver stays reachable while dragging. Off by default, since
 *   always forcing full containment isn't every host's preferred feel — some want a window parkable
 *   mostly off-screen. The window's *initial* placement is always fully clamped on open, regardless
 *   of this flag — only mid-drag and post-drag behavior differ.
 * @param onDragEnd Called with the window's resulting offset each time a drag gesture ends (after
 *   [snapFullyOnscreen] has been applied, if enabled) — e.g. to persist the position, or to react to
 *   the user having moved the window without polling [state] on every frame.
 * @param content The window's body. Not composed while minimized.
 */
@Composable
fun AzWindow(
    modifier: Modifier = Modifier,
    title: String = "",
    state: AzWindowState = rememberAzWindowState(),
    accent: Color = azAccent(),
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    movable: Boolean = true,
    minimizable: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    obstruction: Rect? = null,
    obstructions: List<Rect> = emptyList(),
    snapFullyOnscreen: Boolean = false,
    onDragEnd: ((Offset) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val resolvedAccent = accent.takeOrElse { MaterialTheme.colorScheme.primary }
    val containerSize = LocalWindowInfo.current.containerSize
    val chromeHeightPx = with(LocalDensity.current) { AzWindowDefaults.ChromeHeight.toPx() }
    val allObstructions = if (obstruction != null) obstructions + obstruction else obstructions

    // A container that changed size (rotation, split-screen, a resized desktop window) can leave a
    // window parked outside it. Pull it back rather than stranding it.
    LaunchedEffect(containerSize, chromeHeightPx, allObstructions) {
        state.clampInto(containerSize, chromeHeightPx, allObstructions)
    }

    Surface(
        modifier = modifier
            .offset { IntOffset(state.offsetX.roundToInt(), state.offsetY.roundToInt()) }
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                state.onPositioned(position.x, position.y, coordinates.size)
                // The very first time the window's real position/size are known, force it fully
                // onscreen: a window can open anchored beside whatever raised it (e.g. a long-press
                // menu beside a rail item near a screen edge), and nothing before this point has ever
                // clamped that placement against the actual screen bounds.
                if (!state.hasSettledInitial) {
                    state.hasSettledInitial = true
                    state.settleFullyOnscreen(containerSize, chromeHeightPx, allObstructions)
                }
            },
        shape = AzWindowDefaults.Shape,
        color = surfaceColor,
        border = BorderStroke(2.dp, resolvedAccent),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
    ) {
        Column {
            AzWindowChrome(
                title = title,
                accent = resolvedAccent,
                state = state,
                movable = movable,
                minimizable = minimizable,
                onDismiss = onDismiss,
                containerSize = containerSize,
                chromeHeightPx = chromeHeightPx,
                obstruction = allObstructions,
                snapFullyOnscreen = snapFullyOnscreen,
                onDragEnd = onDragEnd,
            )
            // Folding must not destroy what the window holds. The body stays in composition and is
            // merely given no room, so a half-typed field in a hidden menu — the very thing this
            // window exists to keep reachable — is still half-typed when the user unfolds it.
            // Removing the content from the composition would discard every `remember` inside it,
            // which is "throw it away and re-open it later" wearing a fold control's clothes.
            AzCollapsible(collapsed = state.minimized, content = content)
        }
    }
}

/**
 * Lays out [content] normally, or — when [collapsed] — measures it and reports no size, clipping it
 * away without ever removing it from the composition.
 */
@Composable
private fun AzCollapsible(collapsed: Boolean, content: @Composable () -> Unit) {
    Layout(
        content = { content() },
        modifier = Modifier.clipToBounds(),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = placeables.maxOfOrNull { it.height } ?: 0
        if (collapsed) {
            // Zero-height, but still placed: the nodes stay alive and keep their state.
            layout(0, 0) { placeables.forEach { it.place(0, 0) } }
        } else {
            layout(width, height) { placeables.forEach { it.place(0, 0) } }
        }
    }
}

/** Shared metrics for the library's floating windows. */
object AzWindowDefaults {
    val Shape = RoundedCornerShape(12.dp)

    /** Height of the grab bar — a real touch target, not a hairline. */
    val ChromeHeight = 36.dp
}

/**
 * The grab bar: drag handle, optional title, fold control, optional close.
 *
 * The whole bar is the drag surface, not just the little grip, because a 4dp handle is a target you
 * have to aim at.
 */
@Composable
private fun AzWindowChrome(
    title: String,
    accent: Color,
    state: AzWindowState,
    movable: Boolean,
    minimizable: Boolean,
    onDismiss: (() -> Unit)?,
    containerSize: IntSize,
    chromeHeightPx: Float,
    obstruction: List<Rect>,
    snapFullyOnscreen: Boolean,
    onDragEnd: ((Offset) -> Unit)?,
) {
    val dragModifier = if (movable) {
        Modifier.pointerInput(containerSize, chromeHeightPx, obstruction, snapFullyOnscreen) {
            detectDragGestures(
                onDragEnd = {
                    if (snapFullyOnscreen) {
                        state.settleFullyOnscreen(containerSize, chromeHeightPx, obstruction)
                    }
                    onDragEnd?.invoke(Offset(state.offsetX, state.offsetY))
                },
            ) { change, dragAmount ->
                change.consume()
                state.dragBy(dragAmount.x, dragAmount.y, containerSize, chromeHeightPx, obstruction)
            }
        }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(dragModifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (movable) {
            // The grip: two short rules. It says "this edge is a handle" without spending a glyph.
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(2) {
                    Box(
                        Modifier
                            .size(width = 18.dp, height = 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(accent.copy(alpha = 0.55f))
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = accent,
            maxLines = 1,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        if (minimizable) {
            AzWindowControl(
                accent = accent,
                description = if (state.minimized) "Restore" else "Minimize",
                onClick = { state.minimized = !state.minimized },
            ) {
                if (state.minimized) {
                    // Restore: an outlined pane, i.e. the window you are getting back.
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent)
                    )
                } else {
                    // Minimize: the bar the window is about to become.
                    Box(
                        Modifier
                            .size(width = 12.dp, height = 2.dp)
                            .background(accent)
                    )
                }
            }
        }
        if (onDismiss != null) {
            AzWindowControl(accent = accent, description = "Close", onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** One chrome control: a 36dp target wrapped around a 12dp mark. */
@Composable
private fun AzWindowControl(
    accent: Color,
    description: String,
    onClick: () -> Unit,
    mark: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(AzWindowDefaults.ChromeHeight)
            .clip(RoundedCornerShape(AzWindowDefaults.ChromeHeight / 2))
            .clickable { onClick() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        mark()
    }
}
