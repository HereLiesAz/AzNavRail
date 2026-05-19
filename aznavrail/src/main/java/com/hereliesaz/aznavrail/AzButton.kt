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
 * It automatically handles text resizing to fit the container and supports various shapes,
 * including circular and rectangular variants. Internally delegates to the same primitive
 * ([AzNavRailButton]) used by every rail/menu button so visual parity is guaranteed.
 *
 * Example:
 * ```
 * AzButton(
 *     onClick = { viewModel.save() },
 *     text = "Save",
 *     shape = AzButtonShape.RECTANGLE,
 * )
 * ```
 *
 * @param onClick Callback invoked when the button is clicked.
 * @param text The text to display when [itemContent] is null.
 * @param modifier The modifier to apply to the button.
 * @param color The border/icon color in the unselected state.
 * @param activeColor The color used when the button is pressed.
 * @param textColor Overrides the computed text color. Falls back to [color] when null.
 * @param fillColor Overrides the translucent background fill. Falls back to a computed
 *   light/dark inversion of [color] when null.
 * @param colors Optional [ButtonColors] slot reserved for Material compatibility (currently unused).
 * @param shape The geometric shape of the button (Circle, Square, Rectangle, None).
 * @param enabled Whether the button is enabled and interactive.
 * @param isLoading If true, hides the text and overlays an [AzLoad] spinner.
 * @param contentPadding Custom padding for the button content. Defaults to 8dp on every side.
 * @param itemContent Optional custom composable to render in place of [text]. Wrapped into an
 *   [AzComposableContent] internally so the renderer can apply alpha/scale uniformly.
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
 * A standalone toggle button that switches between two text states.
 *
 * The toggle's visual text and the callback are driven by [isChecked]. The component is
 * intentionally stateless — store the boolean in your own state holder and feed it back here.
 *
 * Example:
 * ```
 * var dark by remember { mutableStateOf(false) }
 * AzToggle(
 *     isChecked = dark,
 *     onToggle = { dark = it },
 *     toggleOnText = "Dark",
 *     toggleOffText = "Light",
 * )
 * ```
 *
 * @param isChecked The current state of the toggle.
 * @param onToggle Callback invoked with the inverted state when the button is tapped.
 * @param toggleOnText Text shown when [isChecked] is true.
 * @param toggleOffText Text shown when [isChecked] is false.
 * @param modifier The modifier to apply to the button container.
 * @param color The base border/icon color in the unselected state.
 * @param activeColor The color used when the toggle is pressed.
 * @param textColor Overrides the computed text color. Falls back to [color] when null.
 * @param fillColor Overrides the translucent background fill.
 * @param colors Reserved Material [ButtonColors] slot (currently unused).
 * @param shape The geometric shape of the button.
 * @param enabled Whether the toggle is interactive.
 * @param itemContent Optional custom composable to render instead of the toggle text.
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
 * A standalone cycler button that rotates through a list of string options.
 *
 * The cycler implements a 1000ms commit delay: each tap advances the displayed option
 * immediately, but [onCycle] only fires after the user has stopped tapping for one second.
 * This lets a user quickly skip past several options without firing intermediate selection
 * callbacks (useful when the underlying state mutation is expensive).
 *
 * Example:
 * ```
 * var quality by remember { mutableStateOf("Auto") }
 * AzCycler(
 *     options = listOf("Auto", "Low", "Medium", "High"),
 *     selectedOption = quality,
 *     onCycle = { quality = it },
 * )
 * ```
 *
 * @param options The list of options to cycle through. Must be non-empty.
 * @param selectedOption The currently committed option from the consumer's state.
 * @param onCycle Callback invoked with the settled option after the 1000ms quiet period.
 * @param modifier The modifier to apply.
 * @param color The base border/icon color.
 * @param activeColor The color used when the cycler is pressed.
 * @param textColor Overrides the computed text color.
 * @param fillColor Overrides the translucent background fill.
 * @param colors Reserved Material [ButtonColors] slot (currently unused).
 * @param shape The geometric shape of the button.
 * @param enabled Whether the cycler is interactive.
 * @param itemContent Optional custom composable to render instead of the option text.
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