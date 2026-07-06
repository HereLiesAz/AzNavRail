package com.hereliesaz.aznavrail.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.LocalAzSafeZones
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Renders guidance as a **non-blocking coach**: a thin accent **outline** around each step's target and
 * a small **callout** placed *near* (never on) that target, with a **pointer/arrow** connecting them.
 *
 * It never dims the screen and never intercepts input outside a callout — the app stays fully usable.
 * Callouts avoid covering the target, other known UI (rail items / targets), each other, and the
 * screen/safe-area edges. **Swiping a callout cancels tutorial mode** ([AzGuidanceController.skip]);
 * tapping an informational step advances it ([AzGuidanceController.advance]). When [renderSlot] is
 * non-null the host draws the callout body itself.
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
    val safeZones = LocalAzSafeZones.current
    // Window size in px (multiplatform-safe: LocalConfiguration is Android-only).
    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidthPx = containerSize.width.toFloat()
    val screenHeightPx = containerSize.height.toFloat()
    val marginPx = with(density) { 8.dp.toPx() }
    val safe = Rect(
        left = marginPx,
        top = with(density) { safeZones.top.toPx() } + marginPx,
        right = screenWidthPx - marginPx,
        bottom = screenHeightPx - with(density) { safeZones.bottom.toPx() } - marginPx,
    )
    val maxCalloutWidthPx = with(density) { 240.dp.toPx() }
    val defaultCalloutHeightPx = with(density) { 84.dp.toPx() }
    val gapPx = with(density) { 12.dp.toPx() }
    val strokePx = with(density) { 2.dp.toPx() }
    val outlinePadPx = with(density) { 4.dp.toPx() }
    val arrowPx = with(density) { 12.dp.toPx() }
    val swipeThresholdPx = with(density) { 48.dp.toPx() }

    // Measured callout sizes (filled after first layout); a second pass re-places with the real size.
    val sizes = remember { mutableStateMapOf<Int, Size>() }

    // Resolve every step's target shape + bounds (re-runs if a target moves and is Compose-backed).
    val targetRects: List<Rect?> = resolved.map {
        it.instruction.highlight.resolveShape(itemBoundsCache, activeItemId, targets)?.bounds()
    }
    val itemRects = itemBoundsCache.values.toList()

    // Greedy placement: each callout near its own target, off obstacles (items + other targets +
    // already-placed callouts), clamped fully into the safe area.
    val placed = ArrayList<Rect>(resolved.size)
    val placements: List<Rect> = resolved.indices.map { i ->
        val size = sizes[i] ?: Size(maxCalloutWidthPx, defaultCalloutHeightPx)
        val obstacles = ArrayList<Rect>(itemRects.size + resolved.size)
        obstacles.addAll(itemRects)
        targetRects.forEachIndexed { j, r -> if (j != i && r != null) obstacles.add(r) }
        obstacles.addAll(placed)
        val rect = placeCallout(targetRects[i], size, obstacles, safe, gapPx)
        placed.add(rect)
        rect
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Draw-only layer: outline each target and the connector to its callout. Re-resolved live in the
        // draw scope so a moving target's outline tracks every frame. NO dim, NO input.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    resolved.forEachIndexed { i, r ->
                        val shape = r.instruction.highlight.resolveShape(itemBoundsCache, activeItemId, targets)
                        if (shape != null) {
                            strokeOutline(shape, accent, strokePx, outlinePadPx)
                            drawConnector(placements[i], shape.bounds(), accent, strokePx, arrowPx)
                        }
                    }
                },
        )

        resolved.forEachIndexed { i, r ->
            val rect = placements[i]
            val measured = sizes.containsKey(i)
            val stepKey = r.edge.stepKey()
            val tappable = r.stepTotal > 1 && r.stepIndex < r.stepTotal - 1 &&
                r.edge.steps.getOrNull(r.stepIndex)?.advanceWhen == null

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = rect.left
                        translationY = rect.top
                        alpha = if (measured) 1f else 0f // hide until measured to avoid a first-frame jump
                    }
                    .widthIn(max = 240.dp)
                    .onSizeChanged { sizes[i] = Size(it.width.toFloat(), it.height.toFloat()) }
                    // Swipe a callout away → cancel tutorial mode (persisted skip). The callout is the
                    // only input-consuming element; everything else on screen passes through.
                    .pointerInput(controller) {
                        var total = Offset.Zero
                        var fired = false
                        detectDragGestures(
                            onDragStart = { total = Offset.Zero; fired = false },
                            onDragEnd = { total = Offset.Zero; fired = false },
                            onDragCancel = { total = Offset.Zero; fired = false },
                        ) { change, drag ->
                            change.consume()
                            total += drag
                            if (!fired && hypot(total.x, total.y) > swipeThresholdPx) {
                                fired = true
                                controller?.skip()
                            }
                        }
                    }
                    .then(
                        if (tappable && controller != null) {
                            Modifier.pointerInput(stepKey) { detectTapGestures { controller.advance(stepKey) } }
                        } else Modifier,
                    ),
            ) {
                if (renderSlot != null) {
                    val shape = r.instruction.highlight.resolveShape(itemBoundsCache, activeItemId, targets)
                    renderSlot(r.toSnapshot(shape, activeItemId), rect)
                } else {
                    Callout(r.instruction, accent, r.stepIndex, r.stepTotal, tappable)
                }
            }
        }
    }
}

@Composable
private fun Callout(
    instruction: AzInstruction,
    accent: Color,
    stepIndex: Int,
    stepTotal: Int,
    tappable: Boolean,
) {
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (stepTotal > 1) "${stepIndex + 1} / $stepTotal" else "swipe to dismiss",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tappable) {
                Text("Tap to continue ▸", style = MaterialTheme.typography.labelSmall, color = accent)
            }
        }
    }
}

/** Strokes the target's geometry (no fill, no dim) so the control stays visible and usable. */
private fun DrawScope.strokeOutline(shape: AzGuideShape, color: Color, strokeWidth: Float, pad: Float) {
    val style = Stroke(width = strokeWidth)
    when (shape) {
        is AzGuideShape.Circle -> drawCircle(
            color = color,
            radius = shape.radius + shape.padding + pad,
            center = Offset(shape.cx, shape.cy),
            style = style,
        )
        is AzGuideShape.Rect -> drawRoundRect(
            color = color,
            topLeft = Offset(shape.left - shape.padding - pad, shape.top - shape.padding - pad),
            size = Size(shape.width + 2f * (shape.padding + pad), shape.height + 2f * (shape.padding + pad)),
            cornerRadius = CornerRadius(if (shape.cornerRadius > 0f) shape.cornerRadius else 12f),
            style = style,
        )
        is AzGuideShape.Path -> drawPath(shape.toComposePath(), color, style = style)
    }
}

/** Draws a line from the [callout]'s edge to the [target]'s edge with a small arrowhead at the target. */
private fun DrawScope.drawConnector(callout: Rect, target: Rect, color: Color, strokeWidth: Float, arrow: Float) {
    val start = edgePoint(callout, target.center)
    val end = edgePoint(target, callout.center)
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = hypot(dx, dy)
    if (len < 1f) return // callout overlaps the target (shouldn't happen) — skip the connector
    drawLine(color, start, end, strokeWidth = strokeWidth)
    val ux = dx / len
    val uy = dy / len
    val leftWing = Offset(end.x - arrow * (ux * 0.86f - uy * 0.5f), end.y - arrow * (uy * 0.86f + ux * 0.5f))
    val rightWing = Offset(end.x - arrow * (ux * 0.86f + uy * 0.5f), end.y - arrow * (uy * 0.86f - ux * 0.5f))
    drawLine(color, end, leftWing, strokeWidth = strokeWidth)
    drawLine(color, end, rightWing, strokeWidth = strokeWidth)
}

/** The point on [rect]'s border along the ray from its centre toward [towards]. */
private fun edgePoint(rect: Rect, towards: Offset): Offset {
    val c = rect.center
    val dx = towards.x - c.x
    val dy = towards.y - c.y
    if (dx == 0f && dy == 0f) return c
    val hw = rect.width / 2f
    val hh = rect.height / 2f
    val sx = if (dx != 0f) hw / abs(dx) else Float.MAX_VALUE
    val sy = if (dy != 0f) hh / abs(dy) else Float.MAX_VALUE
    val scale = min(sx, sy).coerceIn(0f, 1f)
    return Offset(c.x + dx * scale, c.y + dy * scale)
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
