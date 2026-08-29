package com.hereliesaz.aznavrail.internal

import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzOrientation
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for [AzRailLayoutHelper]'s physical-docking rotation mapping, ported from the
 * Android sibling module's `AzRailLayoutHelperTest`.
 *
 * This module previously encoded a *mirrored* version of Android's rotation table — the exact
 * mirrored mapping Android's own test class documents as a bug it once had and fixed (see that
 * class's comment). A left-docked rail at 180° rotation, for example, stayed on the LEFT in
 * Android but flipped to the RIGHT here; there was no test in this module to catch it.
 */
class AzRailLayoutHelperTest {

    @Test
    fun defaultBehaviorSticksToVisualSide() {
        val left0 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, 0f, false)
        assertEquals(AzVisualSide.LEFT, left0.visualSide)
        assertEquals(AzOrientation.Vertical, left0.orientation)

        val left90 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, 90f, false)
        assertEquals(AzVisualSide.LEFT, left90.visualSide)
        assertEquals(AzOrientation.Vertical, left90.orientation)

        val right0 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, 0f, false)
        assertEquals(AzVisualSide.RIGHT, right0.visualSide)
        assertEquals(AzOrientation.Vertical, right0.orientation)
    }

    // Docked LEFT, rotating the device clockwise puts the rail at the TOP, counter-clockwise puts
    // it at the BOTTOM, and upside-down leaves it on the LEFT — matching the Android module.
    @Test
    fun physicalDockingLeftRotationMapping() {
        // 0: Left -> Left
        val rot0 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, 0f, true)
        assertEquals(AzVisualSide.LEFT, rot0.visualSide)
        assertEquals(AzOrientation.Vertical, rot0.orientation)
        assertEquals(false, rot0.reverseLayout)

        // 90 (clockwise): Left -> Top
        val rot90 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, 90f, true)
        assertEquals(AzVisualSide.TOP, rot90.visualSide)
        assertEquals(AzOrientation.Horizontal, rot90.orientation)
        assertEquals(false, rot90.reverseLayout)

        // 180 (upside down): Left -> Left
        val rot180 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, 180f, true)
        assertEquals(AzVisualSide.LEFT, rot180.visualSide)
        assertEquals(AzOrientation.Vertical, rot180.orientation)
        assertEquals(false, rot180.reverseLayout)

        // 270 (counter-clockwise): Left -> Bottom, item order reversed
        val rot270 = AzRailLayoutHelper.calculateLayout(AzDockingSide.LEFT, 270f, true)
        assertEquals(AzVisualSide.BOTTOM, rot270.visualSide)
        assertEquals(AzOrientation.Horizontal, rot270.orientation)
        assertEquals(true, rot270.reverseLayout)
    }

    @Test
    fun physicalDockingRightRotationMapping() {
        // 0: Right -> Right
        val rot0 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, 0f, true)
        assertEquals(AzVisualSide.RIGHT, rot0.visualSide)
        assertEquals(AzOrientation.Vertical, rot0.orientation)
        assertEquals(false, rot0.reverseLayout)

        // 90 (clockwise): Right -> Bottom, item order reversed
        val rot90 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, 90f, true)
        assertEquals(AzVisualSide.BOTTOM, rot90.visualSide)
        assertEquals(AzOrientation.Horizontal, rot90.orientation)
        assertEquals(true, rot90.reverseLayout)

        // 180 (upside down): Right -> Right
        val rot180 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, 180f, true)
        assertEquals(AzVisualSide.RIGHT, rot180.visualSide)
        assertEquals(AzOrientation.Vertical, rot180.orientation)
        assertEquals(false, rot180.reverseLayout)

        // 270 (counter-clockwise): Right -> Top
        val rot270 = AzRailLayoutHelper.calculateLayout(AzDockingSide.RIGHT, 270f, true)
        assertEquals(AzVisualSide.TOP, rot270.visualSide)
        assertEquals(AzOrientation.Horizontal, rot270.orientation)
        assertEquals(false, rot270.reverseLayout)
    }
}
