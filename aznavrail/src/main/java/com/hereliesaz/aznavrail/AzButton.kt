package com.hereliesaz.aznavrail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A circular, text-only button with auto-sizing text.
 * This button is styled to be consistent with the buttons used in the `AzNavRail` component.
 *
 * @param onClick What happens when you click the button. You have to tell it what to do.
 * @param text The text that shows up on the button.
 * @param modifier If you want to change how the button looks, you can use this.
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
 * A toggle button that displays different text based on its on/off state.
 * This button is styled to be consistent with the buttons used in the `AzNavRail` component.
 *
 * @param textWhenOn The text to show when the button is on.
 * @param textWhenOff The text to show when the button is off.
 * @param isOn A boolean that tells the button if it's on or off.
 * @param onClick What happens when you click the button.
 * @param modifier If you want to change how the button looks, you can use this.
 * @param color The color of the button's border and text.
 */
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

/**
 * A button that cycles through a list of options. Each option consists of a text label and an associated action.
 * When the button is clicked, it displays the next option in the list.
 * The action for an option is executed one second after it is displayed.
 *
 * @param options The list of things to show, as pairs of (text, action). You can give it as many as you want.
 * @param modifier If you want to change how the button looks, you can use this.
 * @param color The color of the button's border and text.
 */
@Composable
fun AzCycler(
    vararg options: Pair<String, () -> Unit>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val coroutineScope = rememberCoroutineScope()
    var currentIndex by remember { mutableStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(currentIndex) {
        job?.cancel()
        job = coroutineScope.launch {
            delay(1000L)
            options[currentIndex].second()
        }
    }

    AzButton(
        onClick = {
            currentIndex = (currentIndex + 1) % options.size
        },
        text = options[currentIndex].first,
        modifier = modifier,
        color = color
    )
}
