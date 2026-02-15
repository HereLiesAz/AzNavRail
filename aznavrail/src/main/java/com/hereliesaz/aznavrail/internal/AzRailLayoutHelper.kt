package com.hereliesaz.aznavrail.internal

import android.view.Surface
import androidx.compose.ui.Alignment
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzOrientation

enum class AzVisualSide { LEFT, RIGHT, TOP, BOTTOM }

data class RailLayoutConfig(
    val visualSide: AzVisualSide,
    val orientation: AzOrientation,
    val alignment: Alignment,
    val reverseLayout: Boolean
)

internal object AzRailLayoutHelper {
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
