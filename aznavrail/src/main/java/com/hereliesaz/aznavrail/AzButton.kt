package com.hereliesaz.aznavrail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A circular, text-only button with auto-sizing text.
 *
 * @param onClick A lambda to be executed when the button is clicked.
 * @param text The text to display on the button.
 * @param modifier The modifier to be applied to the button.
 * @param color The color of the button's border and text.
 */
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

/**
 * A toggle button that displays different text for its on and off states.
 *
 * @param isChecked Whether the toggle is in the "on" state.
 * @param onToggle The callback to be invoked when the button is toggled.
 * @param toggleOnText The text to display when the toggle is on.
 * @param toggleOffText The text to display when the toggle is off.
 * @param modifier The modifier to be applied to the button.
 * @param color The color of the button's border and text.
 */
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

/**
 * A button that cycles through a list of options when clicked.
 *
 * @param options The list of options to cycle through.
 * @param selectedOption The currently selected option.
 * @param onCycle The callback to be invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param color The color of the button's border and text.
 */
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
