package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration constants for the AzNavRail layout.
 */
object AzLayoutConfig {
    /** The percentage of screen height reserved for the safe top zone (20%). */
    const val SafeTopPercent = 0.2f
    /** The percentage of screen height reserved for the safe bottom zone (10%). */
    const val SafeBottomPercent = 0.1f
}

/**
 * Data class representing the safe zones (padding) applied to the content.
 *
 * @param top The top safe zone height.
 * @param bottom The bottom safe zone height.
 */
data class AzSafeZones(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp
)
