package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The rail's haptic vocabulary, in one place.
 *
 * The rule used to be "only FAB activation vibrates", which left the interactions people actually
 * perform — tapping an item, flipping a toggle, landing a cycler — speaking through sight alone. A
 * physical reaction is the cheapest possible conveyance that the system registered what you did, and
 * withholding it is exactly the "aesthetics without function" trap.
 *
 * Still deliberately quiet where repetition would make it noise: nothing fires per drag frame, and
 * a cycler speaks once when it *commits*, not on each tap through the options.
 */
internal class AzHaptics(
    private val enabled: Boolean,
    private val perform: (HapticFeedbackType) -> Unit,
) {
    /** A tap that did something — item selected, host expanded, menu entry chosen. */
    fun commit() {
        if (enabled) perform(HapticFeedbackType.TextHandleMove)
    }

    /** A mode change the user should feel distinctly: FAB in/out, a state toggle landing. */
    fun modeChange() {
        if (enabled) perform(HapticFeedbackType.LongPress)
    }
}

/** Builds the [AzHaptics] handle for the current composition. */
@Composable
internal fun rememberAzHaptics(enabled: Boolean): AzHaptics {
    val haptic = LocalHapticFeedback.current
    return AzHaptics(enabled) { haptic.performHapticFeedback(it) }
}
