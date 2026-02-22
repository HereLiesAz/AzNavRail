package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A circular (or rectangular per strict mode), text-only button with auto-sizing text.
 *
 * This component is designed to be used both within the rail and as a standalone button.
 *
 * @param onClick A lambda to be executed when the button is clicked.
 * @param text The text to display on the button.
 * @param modifier The modifier to be applied to the button.
 * @param color The base color of the button's border and text.
 * @param activeColor The color used when the button is in an active state (if applicable).
 * @param colors Custom [ButtonColors] to override the default color logic.
 * @param shape The shape of the button ([AzButtonShape.CIRCLE], [AzButtonShape.RECTANGLE], etc.). Note: In strict mode, all shapes render as Rectangle.
 * @param enabled Whether the button is enabled and interactive.
 * @param isLoading Whether the button is in a loading state, replacing text with a spinner.
 * @param contentPadding The padding to be applied to the button's content. Defaults to 8.dp if null.
 */
@Composable
fun AzButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.RECTANGLE, // Default changed to RECTANGLE for strict compliance
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentPadding: PaddingValues? = null
) {
    AzNavRailButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        color = color,
        activeColor = activeColor,
        colors = colors,
        size = AzNavRailDefaults.HeaderIconSize,
        shape = shape,
        enabled = enabled,
        isSelected = false,
        isLoading = isLoading,
        contentPadding = contentPadding ?: PaddingValues(8.dp)
    )
}

/**
 * A toggle button that displays different text for its on and off states.
 *
 * @param isChecked Whether the toggle is currently in the "on" state.
 * @param onToggle The callback to be invoked when the button is toggled. Passes the new state.
 * @param toggleOnText The text to display when the toggle is on.
 * @param toggleOffText The text to display when the toggle is off.
 * @param modifier The modifier to be applied to the button.
 * @param color The base color of the button's border and text.
 * @param activeColor The color used when the button is in an active state.
 * @param colors Custom [ButtonColors] to override the default color logic.
 * @param shape The shape of the button.
 * @param enabled Whether the button is enabled and interactive.
 */
@Composable
fun AzToggle(
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    toggleOnText: String,
    toggleOffText: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.RECTANGLE,
    enabled: Boolean = true
) {
    val text = if (isChecked) toggleOnText else toggleOffText
    AzNavRailButton(
        onClick = { onToggle(!isChecked) },
        text = text,
        color = color,
        activeColor = activeColor,
        colors = colors,
        size = AzNavRailDefaults.HeaderIconSize,
        shape = shape,
        enabled = enabled,
        isSelected = false
    )
}

/**
 * A button that cycles through a list of options when clicked.
 *
 * The displayed option changes immediately on click, but the [onCycle]
 * action is delayed by one second. This allows for rapid cycling through
 * options without triggering an action for each intermediate selection.
 * Each click resets the delay timer.
 *
 * @param options The list of options to cycle through.
 * @param selectedOption The currently selected option (source of truth).
 * @param onCycle The callback to be invoked for the final selected option after a delay. Passes the new selected option.
 * @param modifier The modifier to be applied to the button.
 * @param color The base color of the button's border and text.
 * @param activeColor The color used when the button is in an active state.
 * @param colors Custom [ButtonColors] to override the default color logic.
 * @param shape The shape of the button.
 * @param enabled Whether the button is enabled and interactive.
 */
@Composable
fun AzCycler(
    options: List<String>,
    selectedOption: String,
    onCycle: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.RECTANGLE,
    enabled: Boolean = true
) {
    var displayedOption by rememberSaveable(selectedOption) { mutableStateOf(selectedOption) }
    var job by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedOption) {
        job?.cancel()
        job = null
        displayedOption = selectedOption
    }

    AzNavRailButton(
        onClick = {
            job?.cancel()

            val currentIndex = options.indexOf(displayedOption)
            val nextIndex = (currentIndex + 1) % options.size
            displayedOption = options[nextIndex]

            job = coroutineScope.launch {
                delay(1000L)
                onCycle(displayedOption)
            }
        },
        text = displayedOption,
        color = color,
        activeColor = activeColor,
        colors = colors,
        size = AzNavRailDefaults.HeaderIconSize,
        shape = shape,
        enabled = enabled,
        isSelected = false
    )
}
