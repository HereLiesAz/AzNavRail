package com.hereliesaz.aznavrail

import androidx.compose.ui.Alignment
import com.hereliesaz.aznavrail.model.AzDropdownAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure mapping checks for [AzDropdownAlignment] — no Compose runtime, just the enum→alignment and
 * unfold-direction helpers that drive drop-down placement.
 */
class AzDropdownAlignmentTest {

    @Test
    fun `toAlignment maps every anchor to the matching Compose alignment`() {
        assertEquals(Alignment.TopStart, AzDropdownAlignment.TOP_START.toAlignment())
        assertEquals(Alignment.TopCenter, AzDropdownAlignment.TOP_CENTER.toAlignment())
        assertEquals(Alignment.TopEnd, AzDropdownAlignment.TOP_END.toAlignment())
        assertEquals(Alignment.CenterStart, AzDropdownAlignment.CENTER_START.toAlignment())
        assertEquals(Alignment.Center, AzDropdownAlignment.CENTER.toAlignment())
        assertEquals(Alignment.CenterEnd, AzDropdownAlignment.CENTER_END.toAlignment())
        assertEquals(Alignment.BottomStart, AzDropdownAlignment.BOTTOM_START.toAlignment())
        assertEquals(Alignment.BottomCenter, AzDropdownAlignment.BOTTOM_CENTER.toAlignment())
        assertEquals(Alignment.BottomEnd, AzDropdownAlignment.BOTTOM_END.toAlignment())
    }

    @Test
    fun `horizontal alignment follows the start-center-end column`() {
        assertEquals(Alignment.Start, AzDropdownAlignment.TOP_START.toHorizontalAlignment())
        assertEquals(Alignment.Start, AzDropdownAlignment.BOTTOM_START.toHorizontalAlignment())
        assertEquals(Alignment.CenterHorizontally, AzDropdownAlignment.CENTER.toHorizontalAlignment())
        assertEquals(Alignment.CenterHorizontally, AzDropdownAlignment.TOP_CENTER.toHorizontalAlignment())
        assertEquals(Alignment.End, AzDropdownAlignment.TOP_END.toHorizontalAlignment())
        assertEquals(Alignment.End, AzDropdownAlignment.CENTER_END.toHorizontalAlignment())
    }

    @Test
    fun `only bottom anchors unfold upward`() {
        assertTrue(AzDropdownAlignment.BOTTOM_START.isBottom)
        assertTrue(AzDropdownAlignment.BOTTOM_CENTER.isBottom)
        assertTrue(AzDropdownAlignment.BOTTOM_END.isBottom)
        assertFalse(AzDropdownAlignment.TOP_START.isBottom)
        assertFalse(AzDropdownAlignment.CENTER.isBottom)
        assertFalse(AzDropdownAlignment.CENTER_END.isBottom)
    }
}
