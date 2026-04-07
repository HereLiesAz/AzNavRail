package com.hereliesaz.aznavrail.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun EqualWidthLayout(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val maxWidth = measurables.maxOfOrNull { it.maxIntrinsicWidth(constraints.maxHeight) } ?: 0
        val newConstraints = constraints.copy(minWidth = maxWidth, maxWidth = maxWidth)

        val placeables = measurables.map { it.measure(newConstraints) }

        val spacingPx = verticalSpacing.toPx()
        val totalHeight = (placeables.sumOf { it.height } + (spacingPx * (placeables.size - 1))).coerceAtLeast(0f)

        layout(newConstraints.maxWidth, totalHeight.roundToInt()) {
            var y = 0f
            placeables.forEach { placeable ->
                placeable.place(0, y.roundToInt())
                y += placeable.height + spacingPx
            }
        }
    }
}
