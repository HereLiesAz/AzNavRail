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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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

    /** Moves the window by a drag delta, keeping its title bar reachable. */
    internal fun dragBy(dx: Float, dy: Float, bounds: IntSize) {
        // Clamp so the window can be pushed to any edge but never entirely off it: at least a
        // title-bar's worth stays inside on every side. A window you can lose is a window you have
        // to reopen.
        val keep = MIN_VISIBLE_PX
        val maxX = (bounds.width - keep).toFloat()
        val minX = -(size.width - keep).toFloat().coerceAtMost(0f)
        val maxY = (bounds.height - keep).toFloat()
        val minY = -(size.height - keep).toFloat().coerceAtMost(0f)
        offsetX = (offsetX + dx).coerceIn(minX.coerceAtMost(maxX), maxX)
        offsetY = (offsetY + dy).coerceIn(minY.coerceAtMost(maxY), maxY)
    }

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
    content: @Composable () -> Unit,
) {
    val resolvedAccent = accent.takeOrElse { MaterialTheme.colorScheme.primary }
    val containerSize = LocalWindowInfo.current.containerSize

    Surface(
        modifier = modifier
            .offset { IntOffset(state.offsetX.roundToInt(), state.offsetY.roundToInt()) }
            .onSizeChanged { state.size = it },
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
            )
            if (!state.minimized) {
                content()
            }
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
) {
    val dragModifier = if (movable) {
        Modifier.pointerInput(containerSize) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                state.dragBy(dragAmount.x, dragAmount.y, containerSize)
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
