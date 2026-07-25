package com.hereliesaz.aznavrail

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The standard AzNavRail loading spinner: a circular border rotating about its Y axis, with
 * "loading..." in the middle.
 *
 * Used full-size by the About/More-from-Az readers, and shrunk down **inside individual buttons** —
 * every rail item is its own loading animation, so a single item can spin while the rest of the rail
 * stays live. The label is dropped automatically at button scale, where there is no room for it.
 *
 * @param size The spinner's diameter. Defaults to the full-screen reader size.
 * @param color Ring and label colour. [Color.Unspecified] (the default) uses the theme primary.
 * @param showLabel Whether "loading..." is drawn. Defaults to true only when [size] is large enough
 *   to fit it.
 */
@Composable
fun AzLoad(
    size: Dp = 120.dp,
    color: Color = Color.Unspecified,
    showLabel: Boolean = size >= 96.dp,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val ringColor = color.takeOrElse { MaterialTheme.colorScheme.primary }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                this.rotationY = rotationY
            }
            .border(
                width = 2.dp,
                color = ringColor,
                shape = CircleShape
            )
            // The label is dropped at button scale, so the spinner needs an identity of its own for
            // accessibility (and for tests) that does not depend on rendering text.
            .semantics { contentDescription = AzLoadContentDescription },
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Text(
                text = "loading...",
                style = TextStyle(
                    color = ringColor,
                    fontSize = 16.sp
                )
            )
        }
    }
}

/** Accessibility label (and test handle) for every [AzLoad] spinner, at any size. */
const val AzLoadContentDescription: String = "loading"
