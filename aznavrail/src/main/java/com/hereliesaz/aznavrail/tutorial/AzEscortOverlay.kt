package com.hereliesaz.aznavrail.tutorial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Renders guidance as an **Escort** ([the framework's §5.2](https://github.com/HereLiesAz/Conveyance/blob/main/docs/CONVEYANCE-FRAMEWORK.md#52-the-escort)):
 * the next control the user needs carries their attention to it, instead of a caption describing it
 * from nearby.
 *
 * This is the alternative to [AzInstructionOverlay] selected via
 * `azAdvanced(guidanceStyle = AzGuidanceStyle.Escort)`. Same engine, same graph, same
 * [ResolvedInstruction]s — only the rendering changes. Where the callout overlay draws a static thin
 * outline plus a floating card connected by an arrow, this overlay draws one thing: a breathing halo
 * that sits directly on the target and **travels** — animates its position — from wherever it was
 * guiding a moment ago to wherever it needs to guide next, whenever the resolved hop changes (the menu
 * opens, then the halo glides from the menu toggle to the item inside it). There is no separate card
 * competing for attention with the control itself.
 *
 * The instruction text never appears on screen. It is published as a polite live-region announcement
 * anchored to the target's own bounds, so it reaches assistive tech the moment the halo lands there —
 * the framework's "focus travel" (§6.2) approximated without a per-item `FocusRequester` registry,
 * which would be the fuller version of this if the rail ever exposes one.
 *
 * A text-only instruction (no resolvable target — an [AzGuideHighlight.None]/[AzGuideHighlight.FullScreen]
 * highlight, or a target/item with no measured bounds yet) has nothing to escort to and is not shown;
 * an Escort with nowhere to go is not an Escort. It will appear the moment its target is measured.
 */
@Composable
internal fun AzEscortOverlay(
    resolved: List<ResolvedInstruction>,
    itemBoundsCache: Map<String, Rect>,
    accent: Color,
    activeItemId: String? = null,
    targets: Map<String, () -> AzGuideShape?> = emptyMap(),
    controller: AzGuidanceController? = null,
) {
    if (resolved.isEmpty()) return
    val density = LocalDensity.current
    val strokePx = with(density) { 3.dp.toPx() }
    val swipeThresholdPx = with(density) { 48.dp.toPx() }
    val breathPx = with(density) { 6.dp.toPx() }

    // One travelling center per stable hop-key, so each active goal's halo moves independently and a
    // key's halo glides from its previous target to its new one rather than popping.
    val centers = remember { mutableStateMapOf<String, Animatable<Offset, AnimationVector2D>>() }

    Box(modifier = Modifier.fillMaxSize()) {
        resolved.forEach { r ->
            val shape = r.instruction.highlight.resolveShape(itemBoundsCache, activeItemId, targets) ?: return@forEach
            val bounds = shape.bounds()
            val key = r.edge.stepKey()
            val target = bounds.center

            val anim = centers.getOrPut(key) { Animatable(target, Offset.VectorConverter) }
            LaunchedEffect(key, target) { anim.animateTo(target, spring(dampingRatio = 0.78f, stiffness = 220f)) }
            val center = anim.value

            val infinite = rememberInfiniteTransition(label = "az-escort-breathe")
            val breathe by infinite.animateFloat(
                initialValue = 0f,
                targetValue = breathPx,
                animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "az-escort-breathe-radius",
            )

            val stepKey = r.edge.stepKey()
            val tappable = r.stepTotal > 1 && r.stepIndex < r.stepTotal - 1 &&
                r.edge.steps.getOrNull(r.stepIndex)?.advanceWhen == null
            val announcement = listOfNotNull(r.instruction.title, r.instruction.text).joinToString(". ")

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind { drawEscortHalo(shape, center, breathe, accent, strokePx) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = announcement
                        liveRegion = LiveRegionMode.Polite
                    }
                    .pointerInput(controller, stepKey) {
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
            )
        }
    }
}

/**
 * Draws the halo: [shape]'s own outline, offset so it is centered on the (possibly still-travelling)
 * [center], inflated by the current [breathe] amount. One continuous, moving, breathing accent — the
 * whole visual, replacing outline + connector + card.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEscortHalo(
    shape: AzGuideShape,
    center: Offset,
    breathe: Float,
    color: Color,
    strokeWidth: Float,
) {
    val bounds = shape.bounds()
    val dx = center.x - bounds.center.x
    val dy = center.y - bounds.center.y
    val style = Stroke(width = strokeWidth)
    when (shape) {
        is AzGuideShape.Circle -> drawCircle(
            color = color,
            radius = shape.radius + shape.padding + breathe,
            center = Offset(shape.cx + dx, shape.cy + dy),
            style = style,
        )
        is AzGuideShape.Rect -> drawRoundRect(
            color = color,
            topLeft = Offset(shape.left + dx - shape.padding - breathe, shape.top + dy - shape.padding - breathe),
            size = Size(
                shape.width + 2f * (shape.padding + breathe),
                shape.height + 2f * (shape.padding + breathe),
            ),
            cornerRadius = CornerRadius(if (shape.cornerRadius > 0f) shape.cornerRadius else 12f),
            style = style,
        )
        is AzGuideShape.Path -> drawRoundRect(
            color = color,
            topLeft = Offset(bounds.left + dx - breathe, bounds.top + dy - breathe),
            size = Size(bounds.width + 2f * breathe, bounds.height + 2f * breathe),
            cornerRadius = CornerRadius(12f),
            style = style,
        )
    }
}
