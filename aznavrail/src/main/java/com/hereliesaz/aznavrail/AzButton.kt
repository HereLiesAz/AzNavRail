package com.hereliesaz.aznavrail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.util.text.AutoSizeText

@Composable
fun AzButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(72.dp).aspectRatio(1f),
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

@Composable
fun AzToggle(
    textWhenOn: String,
    textWhenOff: String,
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    AzButton(
        onClick = onClick,
        text = if (isOn) textWhenOn else textWhenOff,
        modifier = modifier,
        color = color
    )
}

@Composable
fun AzCycler(
    vararg options: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var currentIndex by remember { mutableStateOf(0) }
    AzButton(
        onClick = {
            currentIndex = (currentIndex + 1) % options.size
            onOptionSelected(options[currentIndex])
        },
        text = options[currentIndex],
        modifier = modifier,
        color = color
    )
}
