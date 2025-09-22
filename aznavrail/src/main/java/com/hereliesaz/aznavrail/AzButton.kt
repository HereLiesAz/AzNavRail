package com.hereliesaz.aznavrail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// --- AzButton ---

@Composable
fun AzButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    AzNavRailButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        color = color
    )
}

// --- AzToggle ---

@Composable
fun AzToggle(
    isChecked: Boolean,
    onToggle: () -> Unit,
    toggleOnText: String,
    toggleOffText: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val text = if (isChecked) toggleOnText else toggleOffText

    AzNavRailButton(
        onClick = onToggle,
        text = text,
        modifier = modifier,
        color = color
    )
}

// --- AzCycler ---

@Composable
fun AzCycler(
    options: List<String>,
    selectedOption: String,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var currentIndex by rememberSaveable { mutableStateOf(options.indexOf(selectedOption)) }

    if (currentIndex == -1) {
        currentIndex = 0
    }

    AzNavRailButton(
        onClick = {
            onCycle()
            currentIndex = (currentIndex + 1) % options.size
        },
        text = options[currentIndex],
        modifier = modifier,
        color = color
    )
}
