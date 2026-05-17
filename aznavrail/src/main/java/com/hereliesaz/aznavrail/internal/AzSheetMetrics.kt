// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/AzSheetMetrics.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.Dp
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent

/**
 * Resolves the target [Dp] height for a given [detent] inside a container of [parentHeight].
 *
 * The fractions for HALF/FULL come from [AzSheetConfig.halfFraction] / [AzSheetConfig.fullFraction];
 * PEEK and HIDDEN use absolute values from [AzSheetConfig.peekDp] / [AzSheetConfig.hiddenStripDp].
 */
internal fun heightForDetent(
    detent: AzSheetDetent,
    parentHeight: Dp,
    config: AzSheetConfig,
): Dp = when (detent) {
    AzSheetDetent.HIDDEN -> config.hiddenStripDp
    AzSheetDetent.PEEK -> config.peekDp
    AzSheetDetent.HALF -> parentHeight * config.halfFraction
    AzSheetDetent.FULL -> parentHeight * config.fullFraction
}
