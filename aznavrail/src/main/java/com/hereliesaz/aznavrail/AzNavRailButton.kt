package com.hereliesaz.aznavrail

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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

/**
 * A circular, text-only button with auto-sizing text, designed for the collapsed navigation rail.
 *
 * @param onClick A lambda to be executed when the button is clicked.
 * @param text The text to display on the button.
 * @param modifier The modifier to be applied to the button.
 * @param size The diameter of the circular button.
 * @param color The color of the button's border and text.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
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
        border = BorderStroke(3.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = color
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        AutoSizeText(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = color
            ),
            modifier = Modifier.fillMaxSize(),
            maxLines = if (text.contains("\n")) Int.MAX_VALUE else 1,
            softWrap = false,
            alignment = Alignment.Center,
            lineSpaceRatio = 0.9f
        )
    }
}
