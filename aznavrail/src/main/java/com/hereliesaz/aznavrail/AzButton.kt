package com.hereliesaz.aznavrail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What's so hard to understand about a button? It's a circle, it has text, and you click it.
 * This one is special, though, because it's the same size as the buttons in the `AzNavRail`.
 * The text will also shrink to fit, so you don't have to worry about it looking ugly.
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
 * This is a button that can be on or off. It's like a light switch.
 * You give it two pieces of text, one for when it's on and one for when it's off.
 * It will show the right text all by itself.
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
 * This button is like a rolodex. You give it a list of things, and it will show them one by one.
 * Every time you click it, it shows the next thing in the list.
 * When it gets to the end, it starts over from the beginning.
 * The action for the selected option is executed after a 1-second delay.
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
