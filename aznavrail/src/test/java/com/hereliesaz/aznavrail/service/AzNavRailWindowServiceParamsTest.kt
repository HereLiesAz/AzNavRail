package com.hereliesaz.aznavrail.service

import android.view.WindowManager
import androidx.compose.runtime.Composable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The default overlay window must lay out edge-to-edge, and must say so with the one
 * `layoutInDisplayCutoutMode` value Android 15 did not deprecate. Leaving the field unset inherits
 * the deprecated `DEFAULT`, which is what the Play Console flags.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzNavRailWindowServiceParamsTest {

    /** Exposes the `protected` default params. The lazy needs no Context, so no controller either. */
    private class TestService : AzNavRailWindowService() {
        @Composable
        override fun OverlayContent() = Unit

        fun params(): WindowManager.LayoutParams = windowParams
    }

    @Test
    fun defaultOverlayParams_layOutIntoTheDisplayCutout() {
        assertEquals(
            "The overlay must request LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS — the only mode Android " +
                "15 left non-deprecated — rather than inheriting the deprecated DEFAULT.",
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            TestService().params().layoutInDisplayCutoutMode,
        )
    }

    @Test
    fun defaultOverlayParams_areNotClampedToTheSystemBarArea() {
        assertTrue(
            "FLAG_LAYOUT_NO_LIMITS is what lets a dragged overlay reach the screen edges; the " +
                "cutout mode above only covers the notch.",
            (TestService().params().flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) != 0,
        )
    }
}
