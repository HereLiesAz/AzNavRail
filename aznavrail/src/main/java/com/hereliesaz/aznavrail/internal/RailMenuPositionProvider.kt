package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

internal class RailMenuPositionProvider(
    private val visualSide: AzVisualSide
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return when (visualSide) {
            AzVisualSide.LEFT -> {
                // Open to the right of the anchor
                IntOffset(
                    x = anchorBounds.right,
                    y = anchorBounds.top
                )
            }
            AzVisualSide.RIGHT -> {
                // Open to the left of the anchor
                IntOffset(
                    x = anchorBounds.left - popupContentSize.width,
                    y = anchorBounds.top
                )
            }
            AzVisualSide.TOP -> {
                // Open below the anchor
                IntOffset(
                    x = anchorBounds.left,
                    y = anchorBounds.bottom
                )
            }
            AzVisualSide.BOTTOM -> {
                // Open above the anchor
                IntOffset(
                    x = anchorBounds.left,
                    y = anchorBounds.top - popupContentSize.height
                )
            }
        }
    }
}
