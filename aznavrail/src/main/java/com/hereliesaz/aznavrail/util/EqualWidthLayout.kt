package com.hereliesaz.aznavrail.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun EqualWidthLayout(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val placeables = subcompose(Unit, content).map { it.measure(constraints) }

        val maxWidth = placeables.maxOfOrNull { it.width } ?: 0
        val newConstraints = constraints.copy(minWidth = maxWidth, maxWidth = maxWidth)

        val newPlaceables = subcompose(Unit, content).map { it.measure(newConstraints) }

        val spacingPx = verticalSpacing.toPx()
        val totalHeight = (newPlaceables.sumOf { it.height } + (spacingPx * (newPlaceables.size - 1))).coerceAtLeast(0f)

        layout(newConstraints.maxWidth, totalHeight.roundToInt()) {
            var y = 0f
            newPlaceables.forEach { placeable ->
                placeable.place(0, y.roundToInt())
                y += placeable.height + spacingPx
            }
        }
    }
}
