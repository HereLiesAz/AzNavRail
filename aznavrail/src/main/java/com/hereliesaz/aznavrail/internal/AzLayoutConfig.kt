package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen-percentage constants used to compute safe zones in [com.hereliesaz.aznavrail.AzHostActivityLayout].
 *
 * The content area starts below the top safe zone and ends above the bottom safe zone.
 * The rail itself also enforces inner safe zones to avoid overlapping system bars.
 */
object AzLayoutConfig {
    /** The content area begins this far down from the top of the screen (20 %). */
    const val ContentSafeTopPercent = 0.2f
    /** The content area ends this far from the bottom of the screen (10 %). */
    const val ContentSafeBottomPercent = 0.1f
    /** The rail surface begins this far from the top (10 %). */
    const val RailSafeTopPercent = 0.1f
    /** The rail surface ends this far from the bottom (10 %). */
    const val RailSafeBottomPercent = 0.1f
}

/**
 * Computed safe-zone insets injected via [com.hereliesaz.aznavrail.LocalAzSafeZones] to
 * prevent content from overlapping system bars or the screen's forbidden top/bottom 10 % strip.
 *
 * @param top The top inset in dp (max of 20 % screen height and the system-bar top padding).
 * @param bottom The bottom inset in dp (max of 10 % screen height and the system-bar bottom padding).
 */
data class AzSafeZones(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp
)
