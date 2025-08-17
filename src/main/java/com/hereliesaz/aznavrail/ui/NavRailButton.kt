package com.hereliesaz.aznavrail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A circular, outlined button, designed for use in the collapsed navigation rail.
 * This is the basic building block for all buttons in the rail.
 *
 * @param onClick A lambda to be executed when the button is clicked.
 * @param text The text to display on the button.
 * @param modifier The modifier to be applied to the button.
 * @param size The diameter of the circular button.
 * @param color The color of the button's border and text. Defaults to the theme's primary color.
 */
@Composable
fun NavRailButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        border = BorderStroke(3.dp, color.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = color
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 14.sp,
                lineHeight = 16.sp
            ),
        )
    }
}
