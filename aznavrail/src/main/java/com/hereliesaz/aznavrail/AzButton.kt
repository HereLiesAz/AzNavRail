// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzButton.kt
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
import com.hereliesaz.aznavrail.model.AzComposableContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A standalone button component that matches the aesthetic of the AzNavRail.
 *
 * It automatically handles text resizing to fit the container and supports various shapes.
 *
 * @param onClick Callback invoked when the button is clicked.
 * @param text The text to display.
 * @param modifier The modifier to apply to the button.
 * @param color The background color of the button.
 * @param activeColor The color used when the button is in an active state (if applicable).
 * @param colors Optional [ButtonColors] to override default colors.
 * @param shape The shape of the button (Circle, Square, Rectangle, None).
 * @param enabled Whether the button is enabled.
 * @param isLoading If true, replaces text with a loading spinner.
 * @param contentPadding Custom padding for the button content.
 */
@Composable
fun AzButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color? = null,
    fillColor: Color? = null,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentPadding: PaddingValues? = null,
    itemContent: @Composable (() -> Unit)? = null
) {
    val composableContent = itemContent?.let {
        AzComposableContent { _ -> it() }
    }
    AzNavRailButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        color = color,
        activeColor = activeColor,
        textColor = textColor,
        fillColor = fillColor,
        colors = colors,
        size = AzNavRailDefaults.ButtonWidth,
        shape = shape,
        enabled = enabled,
        isSelected = false,
        isLoading = isLoading,
        contentPadding = contentPadding ?: PaddingValues(8.dp),
        itemContent = composableContent
    )
}

/**
 * A standalone toggle button.
 *
 * This button switches between two text states based on `isChecked`.
 *
 * @param isChecked The current state of the toggle.
 * @param onToggle Callback invoked when the toggle is clicked, providing the new state.
 * @param toggleOnText Text to display when checked.
 * @param toggleOffText Text to display when unchecked.
 * @param modifier The modifier to apply.
 * @param color The background color.
 * @param activeColor The active color.
 * @param colors Optional color overrides.
 * @param shape The shape of the button.
 * @param enabled Whether the toggle is enabled.
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
    textColor: Color? = null,
    fillColor: Color? = null,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true,
    itemContent: @Composable (() -> Unit)? = null
) {
    val text = if (isChecked) toggleOnText else toggleOffText
    val composableContent = itemContent?.let {
        AzComposableContent { _ -> it() }
    }
    AzNavRailButton(
        onClick = { onToggle(!isChecked) },
        text = text,
        color = color,
        activeColor = activeColor,
        textColor = textColor,
        fillColor = fillColor,
        colors = colors,
        size = AzNavRailDefaults.ButtonWidth,
        shape = shape,
        enabled = enabled,
        isSelected = false,
        itemContent = composableContent
    )
}

/**
 * A standalone cycler button.
 *
 * This button cycles through a list of options. It implements a safety delay: clicking cycles
 * the visual display immediately, but the `onCycle` callback is only triggered after a short
 * delay (1000ms) of inactivity to prevent accidental selection during rapid cycling.
 *
 * @param options The list of options to cycle through.
 * @param selectedOption The currently selected option.
 * @param onCycle Callback invoked when an option is settled upon.
 * @param modifier The modifier to apply.
 * @param color The background color.
 * @param activeColor The active color.
 * @param colors Optional color overrides.
 * @param shape The shape of the button.
 * @param enabled Whether the cycler is enabled.
 */
@Composable
fun AzCycler(
    options: List<String>,
    selectedOption: String,
    onCycle: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color? = null,
    fillColor: Color? = null,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true,
    itemContent: @Composable (() -> Unit)? = null
) {
    var displayedOption by rememberSaveable(selectedOption) { mutableStateOf(selectedOption) }
    var job by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val composableContent = itemContent?.let {
        AzComposableContent { _ -> it() }
    }

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
        textColor = textColor,
        fillColor = fillColor,
        colors = colors,
        size = AzNavRailDefaults.ButtonWidth,
        shape = shape,
        enabled = enabled,
        isSelected = false,
        itemContent = composableContent
    )
}