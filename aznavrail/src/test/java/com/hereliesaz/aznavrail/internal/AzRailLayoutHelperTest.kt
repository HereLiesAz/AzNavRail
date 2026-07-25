package com.hereliesaz.aznavrail.internal

import android.view.Surface
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class AzRailLayoutHelperTest {

    @Test
    fun `default behavior sticks to visual side`() {
        // Left Dock
        val left0 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, Surface.ROTATION_0, false)
        assertEquals(AzVisualSide.LEFT, left0.visualSide)
        assertEquals(AzOrientation.Vertical, left0.orientation)

        val left90 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, Surface.ROTATION_90, false)
        assertEquals(AzVisualSide.LEFT, left90.visualSide)
        assertEquals(AzOrientation.Vertical, left90.orientation)

        // Right Dock
        val right0 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, Surface.ROTATION_0, false)
        assertEquals(AzVisualSide.RIGHT, right0.visualSide)
        assertEquals(AzOrientation.Vertical, right0.orientation)
    }

    // The mapping below is the one AGENTS.md specifies: docked LEFT, rotating the device clockwise
    // puts the rail at the TOP, counter-clockwise puts it at the BOTTOM, and upside-down leaves it
    // on the LEFT. These expectations previously encoded a different (mirrored) mapping and had been
    // failing against the implementation.
    @Test
    fun `physical docking LEFT rotation mapping`() {
        // 0: Left -> Left
        val rot0 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, Surface.ROTATION_0, true)
        assertEquals(AzVisualSide.LEFT, rot0.visualSide)
        assertEquals(AzOrientation.Vertical, rot0.orientation)
        assertEquals(false, rot0.reverseLayout)

        // 90 (clockwise): Left -> Top
        val rot90 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, Surface.ROTATION_90, true)
        assertEquals(AzVisualSide.TOP, rot90.visualSide)
        assertEquals(AzOrientation.Horizontal, rot90.orientation)
        assertEquals(false, rot90.reverseLayout)

        // 180 (upside down): Left -> Left
        val rot180 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, Surface.ROTATION_180, true)
        assertEquals(AzVisualSide.LEFT, rot180.visualSide)
        assertEquals(AzOrientation.Vertical, rot180.orientation)
        assertEquals(false, rot180.reverseLayout)

        // 270 (counter-clockwise): Left -> Bottom, item order reversed
        val rot270 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, Surface.ROTATION_270, true)
        assertEquals(AzVisualSide.BOTTOM, rot270.visualSide)
        assertEquals(AzOrientation.Horizontal, rot270.orientation)
        assertEquals(true, rot270.reverseLayout)
    }

    @Test
    fun `physical docking RIGHT rotation mapping`() {
        // 0: Right -> Right
        val rot0 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, Surface.ROTATION_0, true)
        assertEquals(AzVisualSide.RIGHT, rot0.visualSide)
        assertEquals(AzOrientation.Vertical, rot0.orientation)
        assertEquals(false, rot0.reverseLayout)

        // 90 (clockwise): Right -> Bottom, item order reversed
        val rot90 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, Surface.ROTATION_90, true)
        assertEquals(AzVisualSide.BOTTOM, rot90.visualSide)
        assertEquals(AzOrientation.Horizontal, rot90.orientation)
        assertEquals(true, rot90.reverseLayout)

        // 180 (upside down): Right -> Right
        val rot180 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, Surface.ROTATION_180, true)
        assertEquals(AzVisualSide.RIGHT, rot180.visualSide)
        assertEquals(AzOrientation.Vertical, rot180.orientation)
        assertEquals(false, rot180.reverseLayout)

        // 270 (counter-clockwise): Right -> Top
        val rot270 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, Surface.ROTATION_270, true)
        assertEquals(AzVisualSide.TOP, rot270.visualSide)
        assertEquals(AzOrientation.Horizontal, rot270.orientation)
        assertEquals(false, rot270.reverseLayout)
    }
}
