package com.hereliesaz.aznavrail.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A custom layout that forces all children to share the width of the widest child.
 *
 * Useful for aligning a column of buttons or labels so they appear uniform even when
 * their intrinsic widths differ. Children are stacked vertically with [verticalSpacing] between them.
 *
 * @param modifier Applied to the layout container.
 * @param verticalSpacing Gap between adjacent children.
 * @param content The child composables.
 */
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
