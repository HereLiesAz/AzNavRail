package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration constants for the AzNavRail layout.
 */
object AzLayoutConfig {
    const val ContentSafeTopPercent = 0.2f
    const val ContentSafeBottomPercent = 0.1f
    const val RailSafeTopPercent = 0.1f
    const val RailSafeBottomPercent = 0.1f
}

/**
 * Hard-guaranteed bounds injected into the layout tree to prevent 
 * catastrophic overlapping with system bars.
 */
data class AzSafeZones(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp
)
