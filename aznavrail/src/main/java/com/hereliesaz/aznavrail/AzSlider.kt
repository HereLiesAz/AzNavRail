package com.hereliesaz.aznavrail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.hereliesaz.aznavrail.model.AzMotion
import com.hereliesaz.aznavrail.model.AzSliderConfig
import com.hereliesaz.aznavrail.model.AzSliderOrientation
import com.hereliesaz.aznavrail.model.AzSliderSize
import com.hereliesaz.aznavrail.model.AzSliderVariant
import kotlin.math.abs
import kotlin.math.roundToInt

/** The content description an [AzSlider] answers to when the caller supplies no label. */
const val AzSliderContentDescription: String = "slider"

/**
 * The rail's slider, drawn on Material 3 Expressive lines.
 *
 * It is one instrument in four shapes — continuous, stepped, centred and range (see
 * [AzSliderVariant]) — because they differ only in where the active track begins and how many
 * thumbs ride it. Everything else is shared: a thick track with fully-rounded ends, an inset gap
 * where the thumb sits so the thumb reads as *on* the track rather than *in* it, a pill thumb
 * standing across the track, and stop indicators on the inactive track wherever the value is
 * quantised.
 *
 * It is drawn rather than composed out of Material's own `Slider` for the same reason the rest of
 * this library is: the rail has to render identically on Android and in Compose Multiplatform, and
 * it has to be able to stand in a rail slot 80dp wide and run vertically — which Material's slider
 * will not do.
 *
 * ```
 * var volume by remember { mutableFloatStateOf(0.4f) }
 * AzSlider(
 *     value = volume,
 *     onValueChange = { volume = it },
 *     config = AzSliderConfig(variant = AzSliderVariant.STEPPED, steps = 4),
 * )
 * ```
 *
 * @param value The current value, for every variant except [AzSliderVariant.RANGE].
 * @param onValueChange Called continuously as the thumb moves.
 * @param rangeValue The two ends, for [AzSliderVariant.RANGE] only. Ignored otherwise.
 * @param onRangeChange Called continuously as either range thumb moves.
 * @param length How long the slider is along its own axis. Null lets it fill the space it is given.
 * @param label An accessible name. Falls back to [AzSliderContentDescription].
 */
@Composable
fun AzSlider(
    value: Float = 0f,
    onValueChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    config: AzSliderConfig = AzSliderConfig(),
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    rangeValue: ClosedFloatingPointRange<Float> = 0f..1f,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit = {},
    length: Dp? = null,
    label: String? = null,
) {
    val active = color.takeOrElse { MaterialTheme.colorScheme.primary }
    val inactive = trackColor.takeOrElse { active.copy(alpha = 0.24f) }
    val isVertical = config.orientation == AzSliderOrientation.VERTICAL
    val span = (config.valueTo - config.valueFrom).takeIf { abs(it) > 1e-6f } ?: 1f

    // Which thumb a range drag has hold of. Decided once on touch-down by proximity, and kept for
    // the whole gesture — recomputing per move lets the thumbs swap under the finger mid-drag.
    var draggingUpper by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(AzMotion.PanelDurationMs),
        label = "az-slider-press",
    )

    fun quantise(fraction: Float): Float {
        val raw = config.valueFrom + fraction.coerceIn(0f, 1f) * span
        if (config.variant != AzSliderVariant.STEPPED || config.steps <= 0) return raw
        val slots = config.steps + 1
        val stepSize = span / slots
        return config.valueFrom + ((raw - config.valueFrom) / stepSize).roundToInt() * stepSize
    }

    fun fractionOf(v: Float): Float = ((v - config.valueFrom) / span).coerceIn(0f, 1f)

    // The along-axis fraction the pointer is at. Vertical sliders run bottom-up: down is less,
    // which is the direction every physical fader in the world moves.
    fun fractionAt(position: Offset, size: Size): Float =
        if (isVertical) 1f - (position.y / size.height.coerceAtLeast(1f))
        else position.x / size.width.coerceAtLeast(1f)

    fun emit(fraction: Float) {
        val next = quantise(fraction)
        if (config.variant == AzSliderVariant.RANGE) {
            val lower = if (draggingUpper) rangeValue.start else next.coerceAtMost(rangeValue.endInclusive)
            val upper = if (draggingUpper) next.coerceAtLeast(rangeValue.start) else rangeValue.endInclusive
            onRangeChange(lower..upper)
        } else {
            onValueChange(next.coerceIn(config.valueFrom, config.valueTo))
        }
    }

    val thickness = config.size.trackThicknessDp.dp
    val sizeModifier = when {
        length != null && isVertical -> Modifier.size(width = thickness, height = length)
        length != null -> Modifier.size(width = length, height = thickness)
        isVertical -> Modifier.defaultMinSize(minWidth = thickness).fillMaxHeight()
        else -> Modifier.defaultMinSize(minHeight = thickness).fillMaxWidth()
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .semantics { contentDescription = label ?: AzSliderContentDescription }
            .pointerInput(enabled, config, rangeValue.start, rangeValue.endInclusive) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val f = fractionAt(offset, size.toSize())
                    if (config.variant == AzSliderVariant.RANGE) {
                        draggingUpper =
                            abs(f - fractionOf(rangeValue.endInclusive)) < abs(f - fractionOf(rangeValue.start))
                    }
                    emit(f)
                }
            }
            .pointerInput(enabled, config, rangeValue.start, rangeValue.endInclusive) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val f = fractionAt(offset, size.toSize())
                        if (config.variant == AzSliderVariant.RANGE) {
                            draggingUpper =
                                abs(f - fractionOf(rangeValue.endInclusive)) < abs(f - fractionOf(rangeValue.start))
                        }
                        emit(f)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                ) { change, _ ->
                    emit(fractionAt(change.position, size.toSize()))
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val alongLength = if (isVertical) size.height else size.width
            val across = if (isVertical) size.width else size.height
            val trackRadius = CornerRadius(across / 2f, across / 2f)
            val dim = if (enabled) 1f else 0.38f

            // Position along the axis, in pixels from the low end, mapped so a vertical slider's
            // low end is at the bottom.
            fun px(fraction: Float): Float =
                if (isVertical) alongLength * (1f - fraction) else alongLength * fraction

            fun rect(fromFraction: Float, toFraction: Float): Rect {
                val a = px(fromFraction)
                val b = px(toFraction)
                val lo = minOf(a, b)
                val hi = maxOf(a, b)
                return if (isVertical) Rect(0f, lo, across, hi) else Rect(lo, 0f, hi, across)
            }

            // The inactive track, full length.
            drawRoundRect(
                color = inactive.copy(alpha = inactive.alpha * dim),
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = trackRadius,
            )

            val originFraction = when (config.variant) {
                AzSliderVariant.CENTERED -> config.origin?.let { fractionOf(it) } ?: 0.5f
                else -> 0f
            }
            val valueFraction = fractionOf(value)

            // The active track. Continuous and stepped fill from the low end; centred grows out of
            // its origin in whichever direction the value went; range spans its two thumbs.
            val activeRect = when (config.variant) {
                AzSliderVariant.RANGE -> rect(fractionOf(rangeValue.start), fractionOf(rangeValue.endInclusive))
                AzSliderVariant.CENTERED -> rect(originFraction, valueFraction)
                else -> rect(0f, valueFraction)
            }
            drawRoundRect(
                color = active.copy(alpha = active.alpha * dim),
                topLeft = activeRect.topLeft,
                size = activeRect.size,
                cornerRadius = trackRadius,
            )

            // Stop indicators. Only a stepped track gets them, and it always does: they are the
            // sole visible difference between a track that snaps and one that does not.
            if (config.variant == AzSliderVariant.STEPPED && config.steps > 0) {
                val slots = config.steps + 1
                val dotRadius = (across * 0.06f).coerceIn(1.5f, 4f)
                for (i in 0..slots) {
                    val f = i.toFloat() / slots
                    val at = px(f)
                    val onActive = f <= valueFraction
                    drawCircle(
                        color = (if (onActive) inactive else active).copy(alpha = 0.55f * dim),
                        radius = dotRadius,
                        center = if (isVertical) Offset(across / 2f, at) else Offset(at, across / 2f),
                    )
                }
            }

            // The thumb(s). A pill standing across the track, with a gap gouged out of the track on
            // either side so it reads as riding on top rather than being part of it — the detail
            // that makes an M3 Expressive slider look like an M3 Expressive slider.
            val thumbAcross = config.size.thumbLengthDp.dp.toPx().coerceAtMost(across * 1.4f)
            val thumbAlong = config.size.thumbThicknessDp.dp.toPx() * (1f + 0.5f * pressScale)
            val gap = thumbAlong * 1.6f

            fun drawThumb(fraction: Float) {
                val at = px(fraction).coerceIn(thumbAlong, alongLength - thumbAlong)
                // Clear the gap first, then lay the thumb into it.
                val gapRect = if (isVertical) Rect(0f, at - gap / 2f, across, at + gap / 2f)
                else Rect(at - gap / 2f, 0f, at + gap / 2f, across)
                drawRect(color = Color.Transparent, topLeft = gapRect.topLeft, size = gapRect.size,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear)

                val half = thumbAcross / 2f
                val thumbRect = if (isVertical) {
                    Rect(across / 2f - half, at - thumbAlong / 2f, across / 2f + half, at + thumbAlong / 2f)
                } else {
                    Rect(at - thumbAlong / 2f, across / 2f - half, at + thumbAlong / 2f, across / 2f + half)
                }
                drawRoundRect(
                    color = active.copy(alpha = active.alpha * dim),
                    topLeft = thumbRect.topLeft,
                    size = thumbRect.size,
                    cornerRadius = CornerRadius(thumbAlong / 2f, thumbAlong / 2f),
                )
            }

            if (config.variant == AzSliderVariant.RANGE) {
                drawThumb(fractionOf(rangeValue.start))
                drawThumb(fractionOf(rangeValue.endInclusive))
            } else {
                drawThumb(valueFraction)
            }
        }
    }
}
