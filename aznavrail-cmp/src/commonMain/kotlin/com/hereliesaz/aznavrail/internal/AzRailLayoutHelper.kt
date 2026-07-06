package com.hereliesaz.aznavrail.internal

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
    val reverseLayout: Boolean,
)

/**
 * Helper object for calculating rail layout based on docking side and device rotation.
 *
 * Port note vs the Android sibling: `rotation` is a Float (degrees clockwise from portrait — 0f,
 * 90f, 180f, or 270f) rather than an Int matching `android.view.Surface.ROTATION_*`. Callers
 * supply the value via [rememberDeviceRotationDegrees]; on non-Android targets the helper always
 * receives 0f (no physical rotation compensation), matching the desktop/iOS/wasmJs UX where the
 * window is portrait-orientation-only or the concept doesn't apply.
 */
internal object AzRailLayoutHelper {
    /**
     * Calculates the layout configuration.
     *
     * @param dockingSide The configured logical docking side.
     * @param rotation The current display rotation, in degrees clockwise from portrait
     *   (0f / 90f / 180f / 270f).
     * @param usePhysicalDocking Whether to apply physical docking logic (adapting to rotation).
     * @return The calculated [RailLayoutConfig].
     */
    fun calculateLayout(
        dockingSide: AzDockingSide,
        rotation: Float,
        usePhysicalDocking: Boolean,
    ): RailLayoutConfig {
        val visualSide = if (usePhysicalDocking) {
            when (dockingSide) {
                AzDockingSide.LEFT -> when (rotation) {
                    0f -> AzVisualSide.LEFT
                    90f -> AzVisualSide.BOTTOM
                    180f -> AzVisualSide.RIGHT
                    270f -> AzVisualSide.TOP
                    else -> AzVisualSide.LEFT
                }
                AzDockingSide.RIGHT -> when (rotation) {
                    0f -> AzVisualSide.RIGHT
                    90f -> AzVisualSide.TOP
                    180f -> AzVisualSide.LEFT
                    270f -> AzVisualSide.BOTTOM
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
                AzDockingSide.LEFT -> (rotation == 180f || rotation == 270f)
                AzDockingSide.RIGHT -> (rotation == 180f || rotation == 90f)
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
