package com.hereliesaz.aznavrail.internal

import android.view.Surface
import androidx.compose.ui.Alignment
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzOrientation

/**
 * Enumeration representing the visual side of the screen where the rail is docked.
 * This takes into account physical device rotation.
 */
enum class AzVisualSide { LEFT, RIGHT, TOP, BOTTOM }

/**
 * Data class holding the calculated layout configuration for the rail.
 *
 * @param visualSide The effective visual side of the rail.
 * @param orientation The orientation of the rail items.
 * @param alignment The alignment of the rail within the root container.
 * @param reverseLayout Whether the item order should be reversed (e.g., for bottom docking).
 */
data class RailLayoutConfig(
    val visualSide: AzVisualSide,
    val orientation: AzOrientation,
    val alignment: Alignment,
    val reverseLayout: Boolean
)

/**
 * Helper object for calculating rail layout based on docking side and device rotation.
 */
internal object AzRailLayoutHelper {
    /**
     * Calculates the layout configuration.
     *
     * @param dockingSide The configured logical docking side.
     * @param rotation The current display rotation.
     * @param usePhysicalDocking Whether to apply physical docking logic (adapting to rotation).
     * @return The calculated [RailLayoutConfig].
     */
    fun calculateLayout(
        dockingSide: AzDockingSide,
        rotation: Int,
        usePhysicalDocking: Boolean
    ): RailLayoutConfig {
        val visualSide = if (usePhysicalDocking) {
            when (dockingSide) {
                AzDockingSide.LEFT -> when (rotation) {
                    Surface.ROTATION_0 -> AzVisualSide.LEFT
                    Surface.ROTATION_90 -> AzVisualSide.BOTTOM
                    Surface.ROTATION_180 -> AzVisualSide.RIGHT
                    Surface.ROTATION_270 -> AzVisualSide.TOP
                    else -> AzVisualSide.LEFT
                }
                AzDockingSide.RIGHT -> when (rotation) {
                    Surface.ROTATION_0 -> AzVisualSide.RIGHT
                    Surface.ROTATION_90 -> AzVisualSide.TOP
                    Surface.ROTATION_180 -> AzVisualSide.LEFT
                    Surface.ROTATION_270 -> AzVisualSide.BOTTOM
                    else -> AzVisualSide.RIGHT
                }
            }
        } else {
            // Stick to view side (Classic behavior)
            if (dockingSide == AzDockingSide.LEFT) AzVisualSide.LEFT else AzVisualSide.RIGHT
        }

        val orientation = if (visualSide == AzVisualSide.TOP || visualSide == AzVisualSide.BOTTOM)
            AzOrientation.Horizontal
        else
            AzOrientation.Vertical

        val reverseLayout = if (usePhysicalDocking) {
            when (dockingSide) {
                AzDockingSide.LEFT -> (rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270)
                AzDockingSide.RIGHT -> (rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_90)
            }
        } else {
            false
        }

        val alignment = when (visualSide) {
            AzVisualSide.LEFT -> Alignment.TopStart
            AzVisualSide.RIGHT -> Alignment.TopEnd
            AzVisualSide.TOP -> Alignment.TopStart
            AzVisualSide.BOTTOM -> Alignment.BottomStart
        }

        return RailLayoutConfig(visualSide, orientation, alignment, reverseLayout)
    }
}
