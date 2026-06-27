package com.hereliesaz.aznavrail.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val DIM_ALPHA = 0.7f

/**
 * Renders the current guidance [instructions] as callouts, **each placed next to its own highlight
 * target** (so the user sees, in place, how to accomplish each active goal), over a single dim layer
 * with a spotlight punch-out per `Item`/`Area` target. There is no Next button — the app's own state
 * change advances guidance, so this composable is purely presentational.
 */
@Composable
internal fun AzInstructionOverlay(
    instructions: List<AzInstruction>,
    itemBoundsCache: Map<String, Rect>,
    accent: Color,
) {
    if (instructions.isEmpty()) return
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    // Resolve each instruction to its target rect (null = no spotlight; floats centred).
    val placed = instructions.map { it to it.highlight.resolveBounds(itemBoundsCache) }

    Box(modifier = Modifier.fillMaxSize()) {
        // One dim layer, punched out at every targeted rect.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = DIM_ALPHA))
                    placed.forEach { (_, r) ->
                        if (r != null) {
                            drawRoundRect(
                                color = Color.Transparent,
                                topLeft = Offset(r.left, r.top),
                                size = Size(r.width, r.height),
                                cornerRadius = CornerRadius(16.dp.toPx()),
                                blendMode = BlendMode.Clear,
                            )
                        }
                    }
                }
        )

        placed.forEach { (instruction, r) ->
            if (r == null) {
                // No anchor: float bottom-centre.
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
                    Callout(instruction, accent)
                }
            } else {
                // Place adjacent: below the target if there's room, otherwise above; clamp horizontally.
                val belowHasRoom = r.bottom + with(density) { 96.dp.toPx() } < screenHeightPx
                val xPx = r.left.coerceIn(0f, (screenWidthPx - with(density) { 240.dp.toPx() }).coerceAtLeast(0f))
                val yPx = if (belowHasRoom) r.bottom + with(density) { 8.dp.toPx() }
                else (r.top - with(density) { 96.dp.toPx() }).coerceAtLeast(0f)
                Box(
                    modifier = Modifier.padding(
                        start = with(density) { xPx.toDp() },
                        top = with(density) { yPx.toDp() },
                    ),
                ) { Callout(instruction, accent) }
            }
        }
    }
}

@Composable
private fun Callout(instruction: AzInstruction, accent: Color) {
    Column(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
    ) {
        instruction.title?.let {
            Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
        }
        Text(instruction.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        instruction.media?.let { Box(Modifier.padding(top = 8.dp)) { it() } }
    }
}

private fun AzGuideHighlight.resolveBounds(cache: Map<String, Rect>): Rect? = when (this) {
    is AzGuideHighlight.Item -> cache[id]
    is AzGuideHighlight.Area -> Rect(left, top, left + width, top + height)
    else -> null
}
