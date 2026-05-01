// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/RailState.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.Job

/**
 * Logger for AzNavRail internal events.
 */
internal object AzNavRailLogger {
    /**
     * Whether logging is enabled.
     */
    var enabled = true

    /** Logs an error via [android.util.Log.e]. No-ops when [enabled] is false. */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            android.util.Log.e(tag, message, throwable)
        }
    }
}

/** Default dimension and sentinel-value constants used throughout the AzNavRail rendering system. */
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

    /** Sentinel value for [AzNavItem.screenTitle] meaning "do not show a title". */
    const val NO_TITLE = "NO_TITLE_AZ_NAV_RAIL"
    /** Auto-generated ID used for the implicit help button injected when no explicit help item is present. */
    const val AUTO_HELP_ID = "AZ_AUTO_HELP_ID_INTERNAL"
}

/**
 * Transient UI state for a cycler item in the rail.
 *
 * [displayedOption] is the option currently shown on the button, which may be ahead of the
 * committed [AzNavItem.selectedOption] during the 1-second debounce window. [job] is the
 * coroutine that fires the actual [com.hereliesaz.aznavrail.AzNavRailScopeImpl.onClickMap]
 * callback after inactivity; cancelling it aborts a pending commit.
 */
internal data class CyclerTransientState(
    val displayedOption: String,
    val job: Job? = null
)

/**
 * Positions a vertical NestedRail popup at the exact center of the screen vertically,
 * anchored to the left or right of the parent item's bounds.
 */
internal class DockedCenteredPopupPositionProvider(
    private val isRightDocked: Boolean,
    private val anchorWidthPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val y = (windowSize.height - popupContentSize.height) / 2
        val x = if (isRightDocked) {
            anchorBounds.left - popupContentSize.width
        } else {
            anchorBounds.right
        }
        return IntOffset(x, y)
    }
}

/**
 * Positions a horizontal NestedRail popup vertically aligned with the parent item,
 * anchored strictly to the left or right edge of the parent item's bounds.
 */
internal class DockedHorizontalPopupPositionProvider(
    private val isRightDocked: Boolean,
    private val marginPx: Int = 0
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Vertically center the row relative to the parent button
        val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
        val x = if (isRightDocked) {
            anchorBounds.left - popupContentSize.width - marginPx // Left of anchor
        } else {
            anchorBounds.right + marginPx // Right of anchor
        }
        return IntOffset(x, y)
    }
}