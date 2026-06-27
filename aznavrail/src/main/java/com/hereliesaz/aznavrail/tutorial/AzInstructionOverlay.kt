package com.hereliesaz.aznavrail.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val DIM_ALPHA = 0.7f

/**
 * Renders the current guidance [resolved] instructions as callouts, **each placed next to its own
 * highlight target** (so the user sees, in place, how to accomplish each active goal), over a single
 * dim layer with a spotlight punch-out per target. Targets may be rail items (`itemBoundsCache`), the
 * active item ([activeItemId]), or host-registered arbitrary shapes ([targets], re-resolved live in the
 * draw scope so a moving on-screen object tracks every frame).
 *
 * A **paged** edge (more than one step) reveals one step at a time; an informational step (no
 * `advanceWhen`, not the last step) is tap-advanceable — tapping its callout calls
 * [AzGuidanceController.advance]. Reactive/actionable steps still advance only when their status flips,
 * so the overlay never blocks the app during a real interaction. When [renderSlot] is non-null the host
 * draws the callout body itself (the dim/punch-out still draws).
 */
@Composable
internal fun AzInstructionOverlay(
    resolved: List<ResolvedInstruction>,
    itemBoundsCache: Map<String, Rect>,
    accent: Color,
    activeItemId: String? = null,
    targets: Map<String, () -> AzGuideShape?> = emptyMap(),
    controller: AzGuidanceController? = null,
    renderSlot: (@Composable (AzGuidanceSnapshot, Rect?) -> Unit)? = null,
) {
    if (resolved.isEmpty()) return
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val calloutWidthPx = with(density) { 240.dp.toPx() }
    val gapPx = with(density) { 8.dp.toPx() }
    val belowReservePx = with(density) { 96.dp.toPx() }
    val cornerPx = with(density) { 16.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        // One dim layer, punched out at every resolved target. Targets are re-resolved INSIDE the draw
        // scope, so a moving `azGuidanceTarget` shape repaints without a recomposition.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = DIM_ALPHA))
                    resolved.forEach { r ->
                        r.instruction.highlight.resolveShape(itemBoundsCache, activeItemId, targets)
                            ?.let { punchShape(it, cornerPx) }
                    }
                },
        )

        resolved.forEach { r ->
            val instruction = r.instruction
            val shape = instruction.highlight.resolveShape(itemBoundsCache, activeItemId, targets)
            val tappable = r.stepTotal > 1 && r.stepIndex < r.stepTotal - 1 &&
                r.edge.steps.getOrNull(r.stepIndex)?.advanceWhen == null
            val stepKey = r.edge.stepKey()
            val onTap: (() -> Unit)? =
                if (tappable && controller != null) ({ controller.advance(stepKey) }) else null

            // Position adjacent to the (live) target, or float bottom-centre when there is no anchor.
            val boundsProvider: () -> Rect? = {
                instruction.highlight.resolveShape(itemBoundsCache, activeItemId, targets)?.bounds()
            }
            PlacedCallout(
                anchored = shape != null,
                boundsProvider = boundsProvider,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                calloutWidthPx = calloutWidthPx,
                gapPx = gapPx,
                belowReservePx = belowReservePx,
            ) {
                if (renderSlot != null) {
                    renderSlot(r.toSnapshot(shape, activeItemId), shape?.bounds())
                } else {
                    Callout(instruction, accent, r.stepIndex, r.stepTotal, onTap)
                }
            }
        }
    }
}

/** Places [content] adjacent to the live target bounds (re-read in the draw phase) or floats it. */
@Composable
private fun PlacedCallout(
    anchored: Boolean,
    boundsProvider: () -> Rect?,
    screenWidthPx: Float,
    screenHeightPx: Float,
    calloutWidthPx: Float,
    gapPx: Float,
    belowReservePx: Float,
    content: @Composable () -> Unit,
) {
    if (!anchored) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) { content() }
    } else {
        // Offset in the draw phase via graphicsLayer so re-positioning during rail animation / a moving
        // target never triggers a parent layout pass or recomposition.
        Box(
            modifier = Modifier.graphicsLayer {
                // A moving/dynamic target can be momentarily unmeasured; hide (alpha 0) rather than let
                // the callout flash at (0,0) with the default translation for that frame.
                val b = boundsProvider()
                if (b == null) { alpha = 0f; return@graphicsLayer }
                alpha = 1f
                translationX = b.left.coerceIn(0f, (screenWidthPx - calloutWidthPx).coerceAtLeast(0f))
                translationY = if (b.bottom + belowReservePx < screenHeightPx) b.bottom + gapPx
                else (b.top - belowReservePx).coerceAtLeast(0f)
            },
        ) { content() }
    }
}

@Composable
private fun Callout(
    instruction: AzInstruction,
    accent: Color,
    stepIndex: Int,
    stepTotal: Int,
    onTap: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
            .padding(12.dp),
    ) {
        instruction.title?.let {
            Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
        }
        Text(instruction.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        instruction.media?.let { Box(Modifier.padding(top = 8.dp)) { it() } }
        if (stepTotal > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${stepIndex + 1} / $stepTotal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onTap != null) {
                    Text("Tap to continue ▸", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
        }
    }
}

/** Punches a transparent hole the shape's geometry out of the dim layer (window-space px). */
private fun DrawScope.punchShape(shape: AzGuideShape, defaultCornerPx: Float) {
    when (shape) {
        is AzGuideShape.Circle -> drawCircle(
            color = Color.Transparent,
            radius = shape.radius + shape.padding,
            center = Offset(shape.cx, shape.cy),
            blendMode = BlendMode.Clear,
        )
        is AzGuideShape.Rect -> drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(shape.left - shape.padding, shape.top - shape.padding),
            size = Size(shape.width + 2f * shape.padding, shape.height + 2f * shape.padding),
            cornerRadius = CornerRadius(if (shape.cornerRadius > 0f) shape.cornerRadius else defaultCornerPx),
            blendMode = BlendMode.Clear,
        )
        is AzGuideShape.Path -> drawPath(
            path = shape.toComposePath(),
            color = Color.Transparent,
            blendMode = BlendMode.Clear,
        )
    }
}

/** Converts an [AzGuideShape.Path]'s window-space commands into a Compose [Path]. */
private fun AzGuideShape.Path.toComposePath(): Path {
    val path = Path()
    commands.forEach { cmd ->
        when (cmd) {
            is AzPathCmd.MoveTo -> path.moveTo(cmd.x, cmd.y)
            is AzPathCmd.LineTo -> path.lineTo(cmd.x, cmd.y)
            is AzPathCmd.QuadTo -> path.quadraticBezierTo(cmd.x1, cmd.y1, cmd.x, cmd.y)
            is AzPathCmd.CubicTo -> path.cubicTo(cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x, cmd.y)
            AzPathCmd.Close -> path.close()
        }
    }
    return path
}
