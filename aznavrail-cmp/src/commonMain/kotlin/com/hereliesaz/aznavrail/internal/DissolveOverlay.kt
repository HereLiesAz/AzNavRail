package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A tapped item, captured at the moment it dismissed the thing it lived in.
 *
 * When a menu entry's tap closes the drawer, the drawer records what the entry said and exactly
 * where it was, so the label can carry on across the screen after the drawer it came from has gone.
 */
data class DissolveState(
    val itemId: String,
    val text: String,
    val bounds: Rect,
)

/**
 * The tapped label, continuing across the screen and fading as it goes.
 *
 * This is the feedback loop the manifesto asks for: the consequence of the tap is *shown* travelling
 * out of the menu and into the app, rather than the menu simply blinking away and leaving the user
 * to infer that something happened.
 *
 * **This must be hosted at the window root, not inside the rail.** It used to render itself in a
 * `Popup`, whose window is sized to its own content — so the label was clipped the instant it left
 * that window, which looked exactly like it vanishing at the rail's edge. `AzHostActivityLayout`
 * renders it as a full-screen sibling above the rail instead, which is the only way it can actually
 * cross the screen.
 */
@Composable
internal fun DissolveOverlay(
    state: DissolveState,
    textStyle: TextStyle,
    color: Color,
    durationMs: Int,
    easing: Easing,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val translateX = remember(state.itemId) { Animatable(0f) }
    val alpha = remember(state.itemId) { Animatable(1f) }

    Box(Modifier.fillMaxSize()) {
        // The label's centre travels to the screen's centre, so the motion reads as "this went into
        // the app" regardless of which edge the rail is docked to.
        val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()
        val labelCenterX = state.bounds.left + state.bounds.width / 2f
        val targetTranslateX = (screenWidthPx / 2f) - labelCenterX

        LaunchedEffect(state.itemId, targetTranslateX) {
            val spec = tween<Float>(durationMillis = durationMs, easing = easing)
            launch { translateX.animateTo(targetTranslateX, spec) }
            launch { alpha.animateTo(0f, spec) }
            kotlinx.coroutines.delay(durationMs.toLong() + 16L)
            onFinished()
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (state.bounds.left + translateX.value).roundToInt(),
                        state.bounds.top.roundToInt(),
                    )
                }
                .size(
                    width = with(density) { state.bounds.width.toDp() },
                    height = with(density) { state.bounds.height.toDp() },
                )
                .graphicsLayer { this.alpha = alpha.value },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = state.text,
                style = textStyle,
                color = color,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(horizontal = AzNavRailDefaults.MenuItemHorizontalPadding),
            )
        }
    }
}
