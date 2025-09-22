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

    val scope = AzButtonScopeImpl().apply(content)
    require(scope.text.isNotEmpty()) {
        """
        The text for an AzButton cannot be empty.

        // AzButton example
            AzButton(
                text = "Click Me",
                color: Color? = null,
                onClick = { /* Do something */ }
            )
        """.trimIndent()
    }
   
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
    val scope = AzToggleScopeImpl().apply(content)
    require(scope.defaultText.isNotEmpty() && scope.altText.isNotEmpty()) {
        """
        The default and alt text for an AzToggle cannot be empty.

        // AzToggle example
            var isToggled by remember { mutableStateOf(false) }
            AzToggle(
               isChecked = isToggled,
               onToggle = { isToggled = !isToggled },
               toggleOnText = "On",
               toggleOffText = "Off",
               color: Color? = null
            )
        """.trimIndent()
    }

    val text = if (isOn) scope.altText else scope.defaultText
    val color = if (isOn) scope.altColor else scope.defaultColor
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

    require(scope.states.isNotEmpty()) {
        """
        An AzCycler must have at least one state.

        // AzCycler example        
            val cyclerOptions = listOf("A", "B", "C")
            var selectedOption by remember { mutableStateOf(cyclerOptions.first()) }
            AzCycler(
              options = cyclerOptions,
              selectedOption = selectedOption,
              color: Color? = null,
              onCycle = {
                val currentIndex = cyclerOptions.indexOf(selectedOption)
                selectedOption = cyclerOptions[(currentIndex + 1) % cyclerOptions.size]
              }           
            )
        """.trimIndent()
    }

    if (scope.states.isEmpty()) {
        return
    }   
}
