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

/**
 * Pixels by which the system-overlay sheet window should grow *downward*, into the
 * navigation-bar region, when [AzSheetConfig.drawBehindNavBar] is enabled.
 *
 * The window stays anchored `Gravity.BOTTOM` with `y = 0`, so adding this to the window height
 * extends it behind the bar without moving the top edge of any detent. The growth only applies in
 * button navigation, where the measured navigation-bar inset is non-zero; in gesture navigation the
 * inset is ~0, making this a no-op.
 *
 * Crucially this is *not* added to the consumer-visible detent height — the exposed (above-bar)
 * height stays the value the consumer sized its detents to. Only the actual window grows; the extra
 * band is where content flows behind the nav-bar buttons.
 *
 * @param drawBehindNavBar [AzSheetConfig.drawBehindNavBar].
 * @param buttonNav Whether the device uses button (3-button / 2-button) navigation.
 * @param navBarInsetPx The measured navigation-bar inset in pixels (0 in gesture navigation).
 */
internal fun navBarExtensionPx(
    drawBehindNavBar: Boolean,
    buttonNav: Boolean,
    navBarInsetPx: Int,
): Int = if (drawBehindNavBar && buttonNav && navBarInsetPx > 0) navBarInsetPx else 0
