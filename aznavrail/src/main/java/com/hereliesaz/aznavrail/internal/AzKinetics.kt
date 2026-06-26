package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzEntrance
import com.hereliesaz.aznavrail.model.AzExit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Returns a [Modifier] that plays a Windows-Phone-7 entrance/exit and an optional [tiltOnPress] 3D
 * tilt, all written through a single [graphicsLayer] block so they never clobber each other (or an
 * existing [baseRotationZ]).
 *
 * The animation is driven by [visible]: when it is `true` the item plays its [entrance] (staggered by
 * [index]); when it flips `false` it plays its [exit] (reverse-staggered, last item first). Callers
 * that want an exit must keep the item composed until the exit finishes — see the "closing state" in
 * `AzDropdownMenu`/`AzNavRail`.
 *
 * When [floating] is true (the rail in FAB mode) there is no docked edge to hinge a turnstile from, so
 * the cascade degrades to a vertical up/down slide while keeping the per-item stagger.
 *
 * The tilt gesture only *observes* pointer events (never consumes them), so the item's own
 * `clickable`/`onClick` still fires.
 *
 * @param index Stable positional index of the item; drives the [staggerMs] cascade.
 * @param count Total item count, used to reverse the stagger on exit.
 * @param dockingSide Which edge the panel is docked to — the turnstile hinges on that edge.
 * @param baseRotationZ A pre-existing Z rotation to preserve (e.g. the rail's landscape upright text).
 */
@Composable
internal fun rememberAzKineticModifier(
    index: Int,
    count: Int,
    visible: Boolean,
    entrance: AzEntrance,
    exit: AzExit,
    staggerMs: Int,
    durationMs: Int,
    easing: Easing,
    startAngle: Float,
    tiltOnPress: Boolean,
    maxTiltDegrees: Float,
    dockingSide: AzDockingSide,
    floating: Boolean = false,
    baseRotationZ: Float = 0f,
): Modifier {
    val density = LocalDensity.current.density
    val cascadeDist = 20f * density

    // Hidden (pre-entrance) pose. In FAB mode, or for SlideUp, the cascade is a vertical slide.
    val hiddenRotY = if (!floating && entrance == AzEntrance.Turnstile) startAngle else 0f
    val hiddenAlpha = if (entrance == AzEntrance.None) 1f else 0f
    val hiddenTransY =
        if (entrance != AzEntrance.None && (floating || entrance == AzEntrance.SlideUp)) cascadeDist else 0f

    // Exit pose. None means "do not animate out" (the parent just unmounts).
    val exitRotY = if (!floating && exit == AzExit.Turnstile) startAngle else 0f
    val exitAlpha = if (exit == AzExit.None) 1f else 0f
    val exitTransY = if (exit != AzExit.None && floating) -cascadeDist else 0f

    val rotY = remember { Animatable(hiddenRotY) }
    val alpha = remember { Animatable(hiddenAlpha) }
    val transY = remember { Animatable(hiddenTransY) }

    LaunchedEffect(visible) {
        val spec = tween<Float>(durationMillis = durationMs, easing = easing)
        if (visible) {
            if (entrance == AzEntrance.None) {
                launch { rotY.snapTo(0f) }
                launch { alpha.snapTo(1f) }
                launch { transY.snapTo(0f) }
                return@LaunchedEffect
            }
            delay(index.toLong() * staggerMs)
            launch { alpha.animateTo(1f, spec) }
            launch { rotY.animateTo(0f, spec) }
            launch { transY.animateTo(0f, spec) }
        } else {
            if (exit == AzExit.None) return@LaunchedEffect
            delay((count - 1 - index).coerceAtLeast(0).toLong() * staggerMs)
            launch { alpha.animateTo(exitAlpha, spec) }
            launch { rotY.animateTo(exitRotY, spec) }
            launch { transY.animateTo(exitTransY, spec) }
        }
    }

    // Tilt-on-press state.
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    val tiltRotX = remember { Animatable(0f) }
    val tiltRotY = remember { Animatable(0f) }

    val hingeX = if (dockingSide == AzDockingSide.LEFT) 0f else 1f

    var modifier = Modifier.graphicsLayer {
        rotationZ = baseRotationZ
        rotationY = rotY.value + tiltRotY.value
        rotationX = tiltRotX.value
        this.alpha = alpha.value
        translationY = transY.value
        transformOrigin = TransformOrigin(hingeX, 0.5f)
        cameraDistance = 14f * density
    }

    if (tiltOnPress) {
        modifier = modifier.pointerInput(tiltOnPress, maxTiltDegrees) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val nx = (down.position.x / size.width) * 2f - 1f
                val ny = (down.position.y / size.height) * 2f - 1f
                tiltY = nx * maxTiltDegrees
                tiltX = -ny * maxTiltDegrees
                waitForUpOrCancellation()
                tiltY = 0f
                tiltX = 0f
            }
        }
        LaunchedEffect(tiltX, tiltY) {
            launch { tiltRotX.animateTo(tiltX, if (tiltX == 0f) spring() else tween(110)) }
            launch { tiltRotY.animateTo(tiltY, if (tiltY == 0f) spring() else tween(110)) }
        }
    }

    return modifier
}

/**
 * Drives the "closing state" for a panel that wants an [AzExit]: returns whether the items should be
 * **rendered** (kept composed) given the [open] target. When [open] flips false it stays true for the
 * length of the staggered exit ([durationMs] + ([count]-1)·[staggerMs]) so the items can animate out,
 * then flips false to let the caller tear the panel down. With [exit] == [AzExit.None] it tracks
 * [open] exactly (immediate teardown, the legacy behavior).
 */
@Composable
internal fun rememberAzClosingState(
    open: Boolean,
    exit: AzExit,
    count: Int,
    staggerMs: Int,
    durationMs: Int,
): Boolean {
    var rendered by remember { mutableStateOf(open) }
    LaunchedEffect(open) {
        if (open) {
            rendered = true
        } else if (rendered) {
            if (exit != AzExit.None) {
                delay(durationMs.toLong() + (count - 1).coerceAtLeast(0).toLong() * staggerMs)
            }
            rendered = false
        }
    }
    return rendered
}
