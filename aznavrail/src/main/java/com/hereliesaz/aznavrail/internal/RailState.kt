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

    /**
     * Logs an error message.
     *
     * @param tag The log tag.
     * @param message The log message.
     * @param throwable The optional throwable to log.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            android.util.Log.e(tag, message, throwable)
        }
    }
}

/** Default values used by the [com.hereliesaz.aznavrail.AzNavRail] composable. */
internal object AzNavRailDefaults {
    /** Threshold in pixels to trigger a swipe action (open/close). */
    const val SWIPE_THRESHOLD_PX = 20f
    /** Radius in pixels within which the floating rail snaps back to origin. */
    const val SNAP_BACK_RADIUS_PX = 50f
    /** Padding around the header. */
    val HeaderPadding = 8.dp
    /** Size of the header icon. */
    val HeaderIconSize = 72.dp
    /** Spacer between header icon and text. */
    val HeaderTextSpacer = 8.dp
    /** Horizontal padding for rail content. */
    val RailContentHorizontalPadding = 4.dp
    /** Vertical arrangement spacing between rail items. */
    val RailContentVerticalArrangement = 8.dp
    /** Height of the spacer at the bottom of the rail content. */
    val RailContentSpacerHeight = 72.dp
    /** Horizontal padding for menu items. */
    val MenuItemHorizontalPadding = 24.dp
    /** Vertical padding for menu items. */
    val MenuItemVerticalPadding = 12.dp
    /** Horizontal padding for the footer divider. */
    val FooterDividerHorizontalPadding = 16.dp
    /** Vertical padding for the footer divider. */
    val FooterDividerVerticalPadding = 8.dp
    /** Height of the spacer in the footer. */
    val FooterSpacerHeight = 12.dp
    val HeaderHeightDp = 56.dp
    val ButtonSize = 48.dp
}

/**
 * Represents the transient state of a cycler item, tracking the displayed
 * option and the coroutine job for the delayed action.
 *
 * @param displayedOption The currently displayed option in the UI.
 * @param job The coroutine job for handling the delayed click action. This
 *    is cancelled and restarted on each click.
 */
internal data class CyclerTransientState(
    val displayedOption: String,
    val job: Job? = null
)

/**
 * A [PopupPositionProvider] that positions the popup in the center of the window.
 */
internal object CenteredPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (windowSize.width - popupContentSize.width) / 2
        val y = (windowSize.height - popupContentSize.height) / 2
        return IntOffset(x, y)
    }
}
