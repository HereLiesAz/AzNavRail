package com.hereliesaz.aznavrail.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints

@Composable
fun EqualWidthLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val subcomposables = subcompose(Unit, content)
        val placeables = subcomposables.map { it.measure(constraints) }

        val maxWidth = placeables.maxOfOrNull { it.width } ?: 0
        val newConstraints = constraints.copy(minWidth = maxWidth, maxWidth = maxWidth)

        val newPlaceables = subcompose(Unit, content).map { it.measure(newConstraints) }

        layout(newConstraints.maxWidth, constraints.maxHeight) {
            var y = 0
            newPlaceables.forEach { placeable ->
                placeable.place(0, y)
                y += placeable.height
            }
        }
    }
}
