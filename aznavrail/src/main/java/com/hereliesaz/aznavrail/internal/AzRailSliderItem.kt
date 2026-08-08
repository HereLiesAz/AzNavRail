package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzNavRailButton
import com.hereliesaz.aznavrail.AzSlider
import com.hereliesaz.aznavrail.model.AzMotion
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzSliderConfig
import com.hereliesaz.aznavrail.model.AzSliderOrientation
import com.hereliesaz.aznavrail.model.AzSliderVariant
import kotlin.math.roundToInt

/** How much taller than a rail button an unfolded slider stands. */
private const val UNFOLDED_LENGTH_MULTIPLIER = 3f

/**
 * A rail item that unfolds into a slider **in its own slot**.
 *
 * Collapsed it is an ordinary rail button carrying the item's label. Tapped, the slot grows along
 * the rail and the button becomes the track; the value sits underneath, and tapping the value folds
 * it back. Nothing opens over the rail, nothing scrolls, and the control arrives where the user was
 * already looking — which is the whole point of putting it in the rail rather than in a dialog.
 *
 * The track is a plain [AzSlider], so a rail slider is the same instrument as a standalone one and
 * supports every [AzSliderVariant]. The rail forces it vertical, because the rail is.
 */
@Composable
internal fun AzRailSliderItem(
    item: AzNavItem,
    buttonSize: Dp,
    enabled: Boolean,
    color: Color,
    onValueChange: (Float) -> Unit,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
) {
    var unfolded by remember(item.id) { mutableStateOf(false) }

    // The rail is a vertical strip, so a rail slider runs vertically no matter how the item was
    // configured. Everything else the host asked for is honoured.
    val config = (item.sliderConfig ?: AzSliderConfig())
        .copy(orientation = AzSliderOrientation.VERTICAL)

    val unfoldedLength = buttonSize * UNFOLDED_LENGTH_MULTIPLIER
    val length by animateDpAsState(
        targetValue = if (unfolded) unfoldedLength else buttonSize,
        animationSpec = tween(AzMotion.SettleDurationMs, easing = com.hereliesaz.aznavrail.model.AzEasing.Wp7Decelerate),
        label = "az-rail-slider-unfold",
    )
    val trackAlpha by animateFloatAsState(
        targetValue = if (unfolded) 1f else 0f,
        animationSpec = tween(AzMotion.PanelDurationMs),
        label = "az-rail-slider-fade",
    )

    val label = item.sliderValueFormatter?.invoke(item.sliderValue)
        ?: defaultFormat(item, config)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (trackAlpha > 0f) {
            AzSlider(
                value = item.sliderValue,
                onValueChange = onValueChange,
                modifier = Modifier
                    .height(length)
                    .graphicsLayer { alpha = trackAlpha },
                config = config,
                color = color,
                enabled = enabled && unfolded,
                rangeValue = item.sliderRangeStart..item.sliderRangeEnd,
                onRangeChange = onRangeChange,
                length = length,
                label = item.text,
            )
        } else {
            // Folded: the ordinary rail button, indistinguishable from its neighbours until tapped.
            AzNavRailButton(
                onClick = { if (enabled) unfolded = true },
                text = item.text,
                color = color,
                activeColor = color,
                textColor = item.textColor,
                fillColor = item.fillColor,
                backgroundColor = item.translucentBackgroundColor,
                size = buttonSize,
                shape = item.shape ?: com.hereliesaz.aznavrail.model.AzButtonShape.CIRCLE,
                enabled = enabled,
                isSelected = false,
                isLoading = item.isLoading,
                itemContent = item.content,
            )
        }

        // The value doubles as the way back. A separate close affordance would be a second control
        // for a job this one is already doing.
        if (trackAlpha > 0f) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color.copy(alpha = trackAlpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(buttonSize)
                    .clickable(enabled = enabled) { unfolded = false }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/**
 * The label under an unfolded track when the host supplied no formatter.
 *
 * A stepped slider prints its step index, because on a quantised track "3 of 5" is what the user is
 * actually choosing; everything else prints the value to two decimals, and a range prints both ends.
 */
private fun defaultFormat(item: AzNavItem, config: AzSliderConfig): String = when {
    config.variant == AzSliderVariant.RANGE ->
        "${twoDp(item.sliderRangeStart)}–${twoDp(item.sliderRangeEnd)}"

    config.variant == AzSliderVariant.STEPPED && config.steps > 0 -> {
        val slots = config.steps + 1
        val span = (config.valueTo - config.valueFrom).takeIf { it != 0f } ?: 1f
        val index = (((item.sliderValue - config.valueFrom) / span) * slots).roundToInt()
        "$index/$slots"
    }

    else -> twoDp(item.sliderValue)
}

private fun twoDp(v: Float): String = ((v * 100f).roundToInt() / 100f).toString()
