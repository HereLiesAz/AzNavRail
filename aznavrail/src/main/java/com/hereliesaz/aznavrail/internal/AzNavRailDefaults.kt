package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Default, non-configurable values for AzNavRail's visual styling.
 * Restored to 6.99 style.
 */
object AzNavRailDefaults {
    // Header
    val HeaderPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
    val HeaderHeightDp = 64.dp
    val HeaderIconSize = 56.dp
    val HeaderTextSpacer = 12.dp

    // Buttons (Rail)
    val RailButtonBorderWidth = 1.5.dp
    val RailButtonContentPadding = PaddingValues(4.dp)
    val RailButtonHorizontalPadding = 4.dp

    // Menu Items (Expanded)
    val MenuItemHorizontalPadding = 16.dp
    val MenuItemVerticalPadding = 12.dp

    // General
    const val SNAP_BACK_RADIUS_PX = 150
}
