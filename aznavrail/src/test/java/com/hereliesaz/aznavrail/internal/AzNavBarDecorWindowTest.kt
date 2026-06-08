package com.hereliesaz.aznavrail.internal

import com.hereliesaz.aznavrail.model.AzSheetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AzNavBarDecorWindowTest {

    @Test
    fun decorAlpha_isFullBackgroundAlpha_whenNotDrawingBehindNavBar() {
        val config = AzSheetConfig(backgroundAlpha = 0.92f, drawBehindNavBar = false)
        assertEquals(
            "Without drawBehindNavBar the decoration must paint at the full backgroundAlpha.",
            0.92f,
            decorAlphaFor(config, buttonNav = true),
            0.0001f,
        )
    }

    @Test
    fun decorAlpha_isCappedSemiTransparent_whenDrawingBehindNavBarUnderButtonNav() {
        val config = AzSheetConfig(backgroundAlpha = 0.92f, drawBehindNavBar = true)
        val alpha = decorAlphaFor(config, buttonNav = true)
        assertTrue(
            "drawBehindNavBar + button nav must cap the decoration alpha to a semi-transparent " +
                "value (<= 0.5) so the sheet shows through behind the nav bar. Got $alpha.",
            alpha <= 0.5f,
        )
    }

    @Test
    fun decorAlpha_ignoresDrawBehindNavBar_underGestureNav() {
        val config = AzSheetConfig(backgroundAlpha = 0.92f, drawBehindNavBar = true)
        assertEquals(
            "In gesture navigation (buttonNav=false) drawBehindNavBar has no effect; the " +
                "decoration paints at the full backgroundAlpha.",
            0.92f,
            decorAlphaFor(config, buttonNav = false),
            0.0001f,
        )
    }

    @Test
    fun decorAlpha_neverExceedsBackgroundAlpha_whenBackgroundAlreadyTranslucent() {
        // When backgroundAlpha is already below the 0.5 cap, the cap must not raise it.
        val config = AzSheetConfig(backgroundAlpha = 0.3f, drawBehindNavBar = true)
        assertEquals(
            "minOf(backgroundAlpha, 0.5) must keep an already-translucent backgroundAlpha.",
            0.3f,
            decorAlphaFor(config, buttonNav = true),
            0.0001f,
        )
    }

    @Test
    fun drawBehindNavBar_defaultsToFalse() {
        assertFalse(
            "AzSheetConfig.drawBehindNavBar must default to false to preserve existing behavior.",
            AzSheetConfig().drawBehindNavBar,
        )
    }
}
