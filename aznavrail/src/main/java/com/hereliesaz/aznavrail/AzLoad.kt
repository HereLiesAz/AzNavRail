package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.model.AzMotion
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.internal.azMorphedPolygonPath

/**
 * The AzNavRail loading indicator: a filled shape that **morphs** through a sequence of rounded
 * polygons while it turns.
 *
 * Modelled on M3 Expressive's loading indicator rather than on a spinner. The old version was a
 * rotating ring with the word "loading..." printed inside it — two elements doing one element's job,
 * and the word was the only English sentence the core rail put in front of a user. A shape that
 * refuses to settle already reads as "working"; it needs no caption, and it localises for free.
 *
 * The morph is the message: the indicator is never the same shape twice in a row, so it cannot be
 * mistaken for a static graphic the way a paused ring can.
 *
 * @param size The indicator's diameter.
 * @param color Fill colour. [Color.Unspecified] (the default) uses the theme primary.
 * @param vertexCounts The polygon sequence it morphs through, in order, looping.
 */
@Composable
fun AzLoad(
    size: Dp = 120.dp,
    color: Color = Color.Unspecified,
    vertexCounts: List<Int> = AzLoadDefaultVertexCounts,
) {
    val transition = rememberInfiniteTransition(label = "az-load")

    // One full turn per cycle. Rotation alone would be a spinner; it is the pairing with the morph
    // that makes the shape's own geometry carry the progress.
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AzMotion.IndicatorStepMs * 3, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "az-load-angle",
    )

    // Walks 0 → vertexCounts.size, so `progress.toInt()` selects the current pair and the fraction
    // drives the interpolation between them. Eased so each shape holds for a beat before giving way.
    val steps = vertexCounts.size.coerceAtLeast(2)
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = steps.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AzMotion.IndicatorStepMs * steps, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "az-load-morph",
    )

    val fill = color.takeOrElse { MaterialTheme.colorScheme.primary }

    Box(
        modifier = Modifier
            .size(size)
            // The shape carries the meaning visually; assistive tech still needs it named.
            .semantics { contentDescription = AzLoadContentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val index = progress.toInt().coerceIn(0, steps - 1)
            val fraction = (progress - index).coerceIn(0f, 1f)
            val from = vertexCounts[index % vertexCounts.size]
            val to = vertexCounts[(index + 1) % vertexCounts.size]
            rotate(angle) {
                drawPath(
                    path = azMorphedPolygonPath(
                        size = this.size,
                        fromVertices = from,
                        toVertices = to,
                        fraction = fraction,
                        cornerFraction = 0.35f,
                    ),
                    color = fill,
                )
            }
        }
    }
}

/**
 * The default morph sequence: a rounded square softening to a pentagon, a hexagon, and back through
 * a triangle. Deliberately never a circle — a circle mid-rotation is indistinguishable from a still
 * one, which is exactly the "is it frozen?" ambiguity this replaces.
 */
val AzLoadDefaultVertexCounts: List<Int> = listOf(4, 5, 6, 3)

/** Accessibility label (and test handle) for every [AzLoad] indicator, at any size. */
const val AzLoadContentDescription: String = "loading"
