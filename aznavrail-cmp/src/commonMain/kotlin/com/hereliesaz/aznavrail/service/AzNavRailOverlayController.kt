package com.hereliesaz.aznavrail.service

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset

/**
 * CompositionLocal to provide the [AzNavRailOverlayController] to the overlay content.
 */
val LocalAzNavRailOverlayController = compositionLocalOf<AzNavRailOverlayController?> { null }

/**
 * Controller interface for managing the overlay window's state and behavior.
 */
interface AzNavRailOverlayController {
    /**
     * The current offset of the overlay content (window position).
     */
    val contentOffset: State<IntOffset>

    /**
     * Called when a drag operation starts on the overlay rail.
     */
    fun onDragStart()

    /**
     * Called when the overlay rail is dragged.
     * @param dragAmount The amount dragged.
     */
    fun onDrag(dragAmount: Offset)

    /**
     * Called when the drag operation ends.
     */
    fun onDragEnd()
}
