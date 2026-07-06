package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

/**
 * Resolves the bottom safe-zone inset for on-screen content.
 *
 * In gesture navigation the library imposes **no** bottom margin (content runs edge-to-edge —
 * there is no button bar to clear). In button navigation it returns the larger of the 10%
 * content safe-zone and the system navigation-bar inset.
 *
 * @param gestureNav Whether the device is in gesture-navigation mode (see [AzNavMode]).
 * @param contentSafeZone The 10%-of-screen content safe-zone candidate.
 * @param systemBarInset The system navigation-bar bottom inset candidate.
 */
internal fun azResolveSafeBottom(gestureNav: Boolean, contentSafeZone: Dp, systemBarInset: Dp): Dp =
    if (gestureNav) 0.dp else max(contentSafeZone, systemBarInset)

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
