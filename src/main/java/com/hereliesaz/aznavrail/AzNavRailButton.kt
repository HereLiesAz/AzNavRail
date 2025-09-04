package com.hereliesaz.aznavrail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A circular, text-only button with auto-sizing text, designed for the collapsed navigation rail.
 *
 * @param onClick A lambda to be executed when the button is clicked.
 * @param text The text to display on the button.
 * @param modifier The modifier to be applied to the button.
 * @param size The diameter of the circular button.
 * @param color The color of the button's border and text.
 */
@Composable
fun AzNavRailButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(size).aspectRatio(1f),
        shape = CircleShape,
        border = BorderStroke(3.dp, color.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = color
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        var fontSize by remember(text) { mutableStateOf(14.sp) }
        var readyToDraw by remember(text) { mutableStateOf(false) }

        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            fontSize = fontSize,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowHeight || textLayoutResult.didOverflowWidth) {
                    fontSize *= 0.9f
                } else {
                    readyToDraw = true
                }
            },
            modifier = Modifier.alpha(if (readyToDraw) 1f else 0f)
        )
    }
}
