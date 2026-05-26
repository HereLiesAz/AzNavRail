// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/AzSheetGestures.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.hereliesaz.aznavrail.bottomsheet.AzSheetController
import com.hereliesaz.aznavrail.model.AzSheetDetent

/**
 * Vertical drag detector that mirrors LogKitty's accumulated-delta gesture.
 *
 * `dragAmount` from [detectVerticalDragGestures] is per-frame; we sum it across the gesture and
 * step the [controller] exactly once when the cumulative displacement crosses [thresholdDp].
 * The accumulator and the "fired" latch reset on `onDragStart`, `onDragCancel`, and `onDragEnd`
 * so the next gesture starts fresh.
 *
 * Up-drag (negative accumulator) calls [AzSheetController.stepUp]; down-drag calls
 * [AzSheetController.snapTo] with [AzSheetDetent.HIDDEN] to dismiss the sheet entirely.
 * When [AzSheetController.isEnabled] is `false`, drags are ignored.
 */
internal fun Modifier.azSheetVerticalDrag(
    controller: AzSheetController,
    density: Density,
    thresholdDp: Dp,
): Modifier = this.pointerInput(controller, thresholdDp) {
    val thresholdPx = with(density) { thresholdDp.toPx() }
    var accumulated = 0f
    var fired = false
    detectVerticalDragGestures(
        onDragStart = { accumulated = 0f; fired = false },
        onDragCancel = { accumulated = 0f; fired = false },
        onDragEnd = { accumulated = 0f; fired = false },
    ) { change, dragAmount ->
        if (fired || !controller.isEnabled) return@detectVerticalDragGestures
        accumulated += dragAmount
        when {
            accumulated <= -thresholdPx -> {
                controller.stepUp()
                fired = true
                change.consume()
            }
            accumulated >= thresholdPx -> {
                controller.snapTo(AzSheetDetent.HIDDEN)
                fired = true
                change.consume()
            }
        }
    }
}

/**
 * Horizontal drag detector for tab-style navigation. Fires [onSwipeLeft] or [onSwipeRight]
 * exactly once per gesture when the cumulative horizontal displacement exceeds [thresholdDp].
 */
internal fun Modifier.azSheetHorizontalSwipe(
    density: Density,
    thresholdDp: Dp,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier = this.pointerInput(thresholdDp) {
    val thresholdPx = with(density) { thresholdDp.toPx() }
    var accumulated = 0f
    var fired = false
    detectHorizontalDragGestures(
        onDragStart = { accumulated = 0f; fired = false },
        onDragCancel = { accumulated = 0f; fired = false },
        onDragEnd = { accumulated = 0f; fired = false },
    ) { change, dragAmount ->
        if (fired) return@detectHorizontalDragGestures
        accumulated += dragAmount
        when {
            accumulated <= -thresholdPx -> { onSwipeLeft(); fired = true; change.consume() }
            accumulated >= thresholdPx -> { onSwipeRight(); fired = true; change.consume() }
        }
    }
}
