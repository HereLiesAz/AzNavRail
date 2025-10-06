package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A divider that automatically adjusts its orientation based on the available space.
 * It renders as a [HorizontalDivider] in a vertical layout and a [VerticalDivider]
 * in a horizontal layout.
 *
 * @param modifier The modifier to be applied to the divider.
 * @param thickness The thickness of the divider.
 * @param color The color of the divider.
 * @param horizontalPadding The padding to apply to the left and right of the divider.
 * @param verticalPadding The padding to apply to the top and bottom of the divider.
 */
@Composable
fun AzDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 8.dp
) {
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        if (maxHeight > maxWidth) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = thickness,
                color = color
            )
        } else {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = thickness,
                color = color
            )
        }
    }
}