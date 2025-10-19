package com.hereliesaz.aznavrail

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.util.text.AutoSizeText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzButtonShape

/**
 * A circular, text-only button with auto-sizing text, designed for the collapsed navigation rail.
 *
 * @param onClick A lambda to be executed when the button is clicked.
 * @param text The text to display on the button.
 * @param modifier The modifier to be applied to the button.
 * @param size The diameter of the circular button.
 * @param color The color of the button's border and text.
 * @param shape The shape of the button.
 * @param disabled Whether the button is disabled.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AzNavRailButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    disabled: Boolean = false
) {
    val buttonShape = when (shape) {
        AzButtonShape.CIRCLE -> CircleShape
        AzButtonShape.SQUARE -> RoundedCornerShape(0.dp)
        AzButtonShape.RECTANGLE -> RoundedCornerShape(0.dp)
    }

    val buttonModifier = when (shape) {
        AzButtonShape.CIRCLE -> modifier.size(size).aspectRatio(1f)
        AzButtonShape.SQUARE -> modifier.size(size).aspectRatio(1f)
        AzButtonShape.RECTANGLE -> modifier.height(size).widthIn(min = size)
    }

    val disabledColor = color.copy(alpha = 0.5f)

    OutlinedButton(
        onClick = onClick,
        modifier = buttonModifier,
        shape = buttonShape,
        border = BorderStroke(3.dp, if (disabled) disabledColor else color),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (disabled) disabledColor else color,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledColor
        ),
        contentPadding = PaddingValues(8.dp),
        enabled = !disabled
    ) {
        AutoSizeText(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = if (disabled) disabledColor else color
            ),
            modifier = Modifier.fillMaxSize(),
            maxLines = if (text.contains("\n")) Int.MAX_VALUE else 1,
            softWrap = false,
            alignment = Alignment.Center,
            lineSpaceRatio = 0.9f
        )
    }
}
