package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AzLayoutConfig {
    const val SafeTopPercent = 0.2f
    const val SafeBottomPercent = 0.1f
}

data class AzSafeZones(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp
)
