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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A standard loading spinner component for AzNavRail.
 *
 * It renders a rotating circular border with "loading..." text in the center.
 * This component is used by [AzNavRail] when `isLoading` is set to true in `azAdvanced`,
 * and by [AzButton] when its `isLoading` state is true.
 *
 * It uses a simple Y-axis rotation animation.
 */
@Composable
fun AzLoad() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                this.rotationY = rotationY
            }
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "loading...",
            style = TextStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp
            )
        )
    }
}
