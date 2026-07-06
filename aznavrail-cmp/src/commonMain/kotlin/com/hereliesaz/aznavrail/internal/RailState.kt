package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.Job

// Port note vs Android sibling: the Android RailState.kt also declares an `AzNavRailLogger` object
// (using `android.util.Log.e`) and `AzNavRailDefaults`. The CMP module already provides both from
// dedicated files (`internal/AzNavRailLogger.kt` with the expect/actual and
// `internal/AzNavRailDefaults.kt`), so this file only carries the transient-state + popup
// providers.

/**
 * Transient UI state for a cycler item in the rail.
 *
 * [displayedOption] is the option currently shown on the button, which may be ahead of the
 * committed selected option during the 1-second debounce window. [job] is the coroutine that fires
 * the actual click callback after inactivity; cancelling it aborts a pending commit.
 */
internal data class CyclerTransientState(
    val displayedOption: String,
    val job: Job? = null,
)

/**
 * Positions a vertical NestedRail popup at the exact center of the screen vertically, anchored to
 * the left or right of the parent item's bounds.
 */
internal class DockedCenteredPopupPositionProvider(
    private val isRightDocked: Boolean,
    private val anchorWidthPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
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
 * Positions a horizontal NestedRail popup vertically aligned with the parent item, anchored
 * strictly to the left or right edge of the parent item's bounds.
 */
internal class DockedHorizontalPopupPositionProvider(
    private val isRightDocked: Boolean,
    private val marginPx: Int = 0,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
        val x = if (isRightDocked) {
            anchorBounds.left - popupContentSize.width - marginPx
        } else {
            anchorBounds.right + marginPx
        }
        return IntOffset(x, y)
    }
}
