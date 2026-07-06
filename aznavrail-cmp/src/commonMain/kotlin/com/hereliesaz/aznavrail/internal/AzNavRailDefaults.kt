package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.dp

/**
 * Default dimension and sentinel-value constants used throughout the AzNavRail rendering system.
 *
 * Ported verbatim from the Android module's `internal/RailState.kt`. Kept in its own file here so it
 * can live in `commonMain` — the Android original shares a file with `AzNavRailLogger` (which uses
 * `android.util.Log`), which is a Tier-2 file expect/actual will unblock later. Because the values
 * are compile-time constants that never diverge across platforms, the small duplication is safe.
 */
internal object AzNavRailDefaults {
    /** Minimum horizontal drag distance (px) before a swipe-to-open gesture is recognised. */
    const val SWIPE_THRESHOLD_PX = 20f
    /** FAB-mode: distance from origin (px) within which the rail snaps back to its docked position. */
    const val SNAP_BACK_RADIUS_PX = 50f
    /** Padding applied to the header row on all sides. */
    val HeaderPadding = 8.dp

    /** Strict unified button width shared by all rail items, app icons, and nested-rail items. */
    val ButtonWidth = 72.dp
    /** Reduced button width used when a vertical nested rail popup is open. */
    val ShrunkButtonWidth = 56.dp

    val HeaderTextSpacer = 8.dp
    val RailContentHorizontalPadding = 4.dp
    /** Vertical gap between rail buttons when not in packed mode. */
    val RailContentVerticalArrangement = 8.dp
    /** Height of the spacer rendered in place of a divider item in the collapsed rail. */
    val RailContentSpacerHeight = 72.dp
    val MenuItemHorizontalPadding = 24.dp
    val MenuItemVerticalPadding = 12.dp
    val FooterDividerHorizontalPadding = 16.dp
    val FooterDividerVerticalPadding = 8.dp
    val FooterSpacerHeight = 12.dp
    /** Fixed height of the header row (matching the standard button width for alignment). */
    val HeaderHeightDp = 72.dp

    /** Sentinel value for `AzNavItem.screenTitle` meaning "do not show a title". */
    const val NO_TITLE = "NO_TITLE_AZ_NAV_RAIL"
    /** Auto-generated ID used for the implicit help button injected when no explicit help item is present. */
    const val AUTO_HELP_ID = "AZ_AUTO_HELP_ID_INTERNAL"
}
