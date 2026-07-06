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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * State handle for a single tapped-and-dismissing menu item. When the user taps a menu item whose
 * click causes the drawer to close (`collapseOnClick = true`), the drawer captures the item's
 * rendered text and window-space bounds, stores them here, and mounts a [DissolveOverlay] that
 * slides the text toward the middle of the screen while fading it out — while the rest of the
 * items run their normal bottom-up exit cascade.
 *
 * The original item slot is skipped from the exit render so the label doesn't animate in two
 * places at once.
 */
internal data class DissolveState(
    val itemId: String,
    val text: String,
    val bounds: Rect,
)

/**
 * A full-screen [Popup] that renders the tapped menu item's label at its captured [DissolveState]
 * position and animates it: `translationX` from `0` toward the middle of the screen, `alpha` from
 * `1` to `0`. Runs over [durationMs] with [easing] (default WP7 decelerate). Calls [onFinished]
 * once the animation completes so the caller can clear its state.
 *
 * The popup deliberately uses `clippingEnabled = false` and `focusable = false` so the slide can
 * cross the rail's docked edge, and taps still fall through to whatever is behind the drawer.
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
    val translateX = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    Popup(
        properties = PopupProperties(clippingEnabled = false, focusable = false),
        // The popup itself fills the window (its content is `Box(fillMaxSize)`). The actual label
        // is positioned by an `.offset(x, y)` inside that Box, so we don't need any anchor here.
        popupPositionProvider = TopLeftPositionProvider,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Screen center X in px — the label's `translationX` targets this from its captured
            // left edge, minus half the label's width so its centre lands on the screen centre.
            val screenWidthPx = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toFloat()
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
}

private object TopLeftPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(0, 0)
}
