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
 *
 * @param options The list of things to show. You can give it as many as you want.
 * @param onOptionSelected This is how you know which option is showing. It will give you the text of the current option.
 * @param modifier If you want to change how the button looks, you can use this.
 * @param color The color of the button's border and text.
 */
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
