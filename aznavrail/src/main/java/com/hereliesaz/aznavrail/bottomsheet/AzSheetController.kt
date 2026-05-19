// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/bottomsheet/AzSheetController.kt
package com.hereliesaz.aznavrail.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.hereliesaz.aznavrail.model.AzSheetDetent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State holder for an [AzBottomSheet] or [AzBottomSheetWindowHost].
 *
 * The controller carries two channels per piece of state:
 *
 * - a Compose [androidx.compose.runtime.mutableStateOf] (exposed via [detent] / [isEnabled]) that
 *   drives recomposition for in-tree consumers; and
 * - a [StateFlow] ([detentFlow] / [enabledFlow]) consumed by the system-overlay flavor's coroutine
 *   collector to resize the `WindowManager` window and toggle focusability.
 *
 * Both channels stay in sync; mutate either of the two `var` properties from the main thread.
 *
 * @property detent The current detent. Setter pushes through [detentFlow].
 * @property isEnabled When `false`, [stepUp] and [stepDown] are ignored and the sheet collapses to
 *   [AzSheetDetent.HIDDEN]. The system-overlay flavor uses this as the launcher-pass-through gate
 *   that mirrors LogKitty's accessibility-driven behavior.
 */
@Stable
class AzSheetController internal constructor(initial: AzSheetDetent) {

    private var _detent by mutableStateOf(initial)
    private val _detentFlow = MutableStateFlow(initial)
    private var _isEnabled by mutableStateOf(true)
    private val _enabledFlow = MutableStateFlow(true)

    var detent: AzSheetDetent
        get() = _detent
        set(value) {
            _detent = value
            _detentFlow.value = value
        }

    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            _isEnabled = value
            _enabledFlow.value = value
            if (!value) detent = AzSheetDetent.HIDDEN
        }

    /** Emits whenever [detent] is mutated. */
    val detentFlow: StateFlow<AzSheetDetent> get() = _detentFlow.asStateFlow()

    /** Emits whenever [isEnabled] is mutated. */
    val enabledFlow: StateFlow<Boolean> get() = _enabledFlow.asStateFlow()

    /** Steps up one detent (HIDDEN → PEEK → HALF → FULL), no-op past FULL or when disabled. */
    fun stepUp() {
        if (!_isEnabled) return
        detent = when (_detent) {
            AzSheetDetent.HIDDEN -> AzSheetDetent.PEEK
            AzSheetDetent.PEEK -> AzSheetDetent.HALF
            AzSheetDetent.HALF -> AzSheetDetent.FULL
            AzSheetDetent.FULL -> AzSheetDetent.FULL
        }
    }

    /** Steps down one detent (FULL → HALF → PEEK → HIDDEN), no-op past HIDDEN. */
    fun stepDown() {
        if (!_isEnabled && _detent != AzSheetDetent.HIDDEN) {
            detent = AzSheetDetent.HIDDEN
            return
        }
        detent = when (_detent) {
            AzSheetDetent.FULL -> AzSheetDetent.HALF
            AzSheetDetent.HALF -> AzSheetDetent.PEEK
            AzSheetDetent.PEEK -> AzSheetDetent.HIDDEN
            AzSheetDetent.HIDDEN -> AzSheetDetent.HIDDEN
        }
    }

    /** Jumps directly to [target]. Ignores [isEnabled] when [target] is HIDDEN; otherwise gated. */
    fun snapTo(target: AzSheetDetent) {
        if (target != AzSheetDetent.HIDDEN && !_isEnabled) return
        detent = target
    }

    companion object {
        /**
         * Constructs an [AzSheetController] directly. Prefer [rememberAzSheetController] inside Compose
         * so the detent survives recomposition and configuration changes.
         */
        operator fun invoke(initial: AzSheetDetent = AzSheetDetent.HIDDEN): AzSheetController =
            AzSheetController(initial)

        internal val Saver: Saver<AzSheetController, String> = Saver(
            save = { it.detent.name },
            restore = { AzSheetController(AzSheetDetent.valueOf(it)) },
        )
    }
}

/**
 * Remembers an [AzSheetController] that survives recomposition and configuration changes.
 *
 * Positional scoping: if you create multiple controllers in the same composition, simply call
 * this function multiple times — each call gets its own positionally-scoped saved state.
 *
 * @param initial The detent to use when the controller is first created.
 */
@Composable
fun rememberAzSheetController(
    initial: AzSheetDetent = AzSheetDetent.HIDDEN,
): AzSheetController = rememberSaveable(saver = AzSheetController.Saver) {
    AzSheetController(initial)
}
