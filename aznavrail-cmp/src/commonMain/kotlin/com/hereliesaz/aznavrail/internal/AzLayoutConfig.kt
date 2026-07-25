package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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

/** A resolved inset on each of the four edges, in dp. */
internal data class AzEdgeInsets(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
)

/** The larger of [a] and [b] on every edge. */
internal fun azMaxEdges(a: PaddingValues, b: PaddingValues, layoutDirection: LayoutDirection) = AzEdgeInsets(
    start = max(a.calculateStartPadding(layoutDirection), b.calculateStartPadding(layoutDirection)),
    top = max(a.calculateTopPadding(), b.calculateTopPadding()),
    end = max(a.calculateEndPadding(layoutDirection), b.calculateEndPadding(layoutDirection)),
    bottom = max(a.calculateBottomPadding(), b.calculateBottomPadding()),
)

/**
 * The window insets the host must clear on each edge: the system bars **union** the display cutout.
 *
 * The cutout is not redundant with the bars. From Android 15 an app targeting SDK 35 is laid out
 * edge-to-edge with `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` in force, so a camera cutout can occlude
 * an edge that carries no system bar at all — most visibly the short edge in landscape, where the
 * rail and the onscreen content used to run straight under the camera.
 *
 * `WindowInsets.safeDrawing` would cover both, but it also folds in `ime`, which would make the
 * whole layout jump every time the keyboard opens. Hence the explicit per-edge union.
 */
@Composable
internal fun azWindowSafeInsets(): AzEdgeInsets = azMaxEdges(
    WindowInsets.systemBars.asPaddingValues(),
    WindowInsets.displayCutout.asPaddingValues(),
    LocalLayoutDirection.current,
)

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
 * prevent content from overlapping system bars, a display cutout, or the screen's forbidden
 * top/bottom 10 % strip.
 *
 * @param top The top inset in dp (max of 20 % screen height and the system-bar / cutout top inset).
 * @param bottom The bottom inset in dp (max of 10 % screen height and the system-bar / cutout bottom inset).
 * @param start The start inset in dp — the system-bar / cutout inset on that edge, 0 when there is
 *   none. Non-zero mainly in landscape: a button navigation bar or a camera cutout on the short edge.
 * @param end The end inset in dp, resolved the same way as [start].
 */
data class AzSafeZones(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val start: Dp = 0.dp,
    val end: Dp = 0.dp,
)
