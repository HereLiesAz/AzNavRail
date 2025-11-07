package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.Job

internal object AzNavRailLogger {
    var enabled = true
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            android.util.Log.e(tag, message, throwable)
        }
    }
}

/** Default values used by the [AzNavRail] composable. */
internal object AzNavRailDefaults {
    const val SWIPE_THRESHOLD_PX = 20f
    const val SNAP_BACK_RADIUS_PX = 50f
    val HeaderPadding = 8.dp
    val HeaderIconSize = 72.dp
    val HeaderTextSpacer = 8.dp
    val RailContentHorizontalPadding = 4.dp
    val RailContentVerticalArrangement = 8.dp
    val RailContentSpacerHeight = 72.dp
    val MenuItemHorizontalPadding = 24.dp
    val MenuItemVerticalPadding = 12.dp
    val FooterDividerHorizontalPadding = 16.dp
    val FooterDividerVerticalPadding = 8.dp
    val FooterSpacerHeight = 12.dp
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