package com.hereliesaz.aznavrail

import org.junit.Assert.assertEquals
import org.junit.Test
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import androidx.compose.ui.graphics.Color

class AzTextBoxDefaultsTest {

    @Test
    fun `setSuggestionLimit should coerce values correctly`() {
        AzTextBoxDefaults.setSuggestionLimit(-1)
        assertEquals(0, AzTextBoxDefaults.getSuggestionLimit())

        AzTextBoxDefaults.setSuggestionLimit(3)
        assertEquals(3, AzTextBoxDefaults.getSuggestionLimit())

        AzTextBoxDefaults.setSuggestionLimit(10)
        assertEquals(5, AzTextBoxDefaults.getSuggestionLimit())
    }

    @Test
    fun `setBackgroundOpacity should coerce values correctly`() {
        AzTextBoxDefaults.setBackgroundOpacity(-0.5f)
        assertEquals(0f, AzTextBoxDefaults.getBackgroundOpacity(), 0.01f)

        AzTextBoxDefaults.setBackgroundOpacity(0.5f)
        assertEquals(0.5f, AzTextBoxDefaults.getBackgroundOpacity(), 0.01f)

        AzTextBoxDefaults.setBackgroundOpacity(1.5f)
        assertEquals(1f, AzTextBoxDefaults.getBackgroundOpacity(), 0.01f)
    }

    @Test
    fun `setBackgroundColor should set color`() {
        AzTextBoxDefaults.setBackgroundColor(Color.Red)
        assertEquals(Color.Red, AzTextBoxDefaults.getBackgroundColor())
    }
}
