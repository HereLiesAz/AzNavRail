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
 * It renders as a [HorizontalDivider] in a vertical layout (e.g., a `Column`)
 * and a [VerticalDivider] in a horizontal layout (e.g., a `Row`).
 *
 * This composable is designed to be a direct replacement for `HorizontalDivider` or
 * `VerticalDivider` when the orientation needs to be dynamic.
 *
 * The default styling matches the divider used in the `AzNavRail` footer.
 *
 * @param modifier The modifier to be applied to the divider.
 * @param thickness The thickness of the divider line. Defaults to `1.dp`.
 * @param color The color of the divider line. Defaults to a semi-transparent outline color
 * from the `MaterialTheme`.
 * @param horizontalPadding The padding applied to the left and right of the divider.
 * Defaults to `16.dp`.
 * @param verticalPadding The padding applied to the top and bottom of the divider.
 * Defaults to `8.dp`.
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