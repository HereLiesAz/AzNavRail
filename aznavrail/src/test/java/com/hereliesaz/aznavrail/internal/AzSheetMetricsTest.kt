package com.hereliesaz.aznavrail.internal

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [navBarExtensionPx] — the px by which the system-overlay sheet window grows
 * downward into the navigation-bar region when [com.hereliesaz.aznavrail.model.AzSheetConfig.drawBehindNavBar]
 * is enabled.
 */
class AzSheetMetricsTest {

    @Test
    fun extension_isInset_whenDrawBehindNavBarUnderButtonNavWithInset() {
        assertEquals(
            "drawBehindNavBar + button nav + measured inset must grow the window by exactly the inset.",
            48,
            navBarExtensionPx(drawBehindNavBar = true, buttonNav = true, navBarInsetPx = 48),
        )
    }

    @Test
    fun extension_isZero_whenDrawBehindNavBarDisabled() {
        assertEquals(
            "Without drawBehindNavBar the window never grows behind the bar.",
            0,
            navBarExtensionPx(drawBehindNavBar = false, buttonNav = true, navBarInsetPx = 48),
        )
    }

    @Test
    fun extension_isZero_underGestureNav() {
        assertEquals(
            "In gesture nav (buttonNav=false) drawBehindNavBar is a no-op.",
            0,
            navBarExtensionPx(drawBehindNavBar = true, buttonNav = false, navBarInsetPx = 48),
        )
    }

    @Test
    fun extension_isZero_whenInsetIsZero() {
        assertEquals(
            "A zero measured inset (gesture-style thin bar) means nothing to grow into.",
            0,
            navBarExtensionPx(drawBehindNavBar = true, buttonNav = true, navBarInsetPx = 0),
        )
    }
}
