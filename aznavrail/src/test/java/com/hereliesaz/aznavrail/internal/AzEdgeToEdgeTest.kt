package com.hereliesaz.aznavrail.internal

import android.view.WindowManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android 15 (API 35) deprecated every `layoutInDisplayCutoutMode` value except
 * `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`, and a window that never assigns the field silently
 * inherits the deprecated `DEFAULT`. These tests pin the migration: `azAllowDisplayCutout()` assigns
 * `ALWAYS`, and it is chained onto every set of window params this library builds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzAllowDisplayCutoutTest {

    @Test
    fun azAllowDisplayCutout_assignsTheOnlyNonDeprecatedMode() {
        val params = WindowManager.LayoutParams().azAllowDisplayCutout()
        assertEquals(
            "Only LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS survived Android 15's deprecation of " +
                "DEFAULT / SHORT_EDGES / NEVER, so it is the value every window must carry.",
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            params.layoutInDisplayCutoutMode,
        )
    }

    @Test
    fun azAllowDisplayCutout_configuresTheReceiverInPlace() {
        val params = WindowManager.LayoutParams()
        assertSame(
            "The helper must configure the receiver in place so it can be chained onto a " +
                "LayoutParams constructor call.",
            params,
            params.azAllowDisplayCutout(),
        )
    }

    @Test
    fun azAllowDisplayCutout_overwritesADeprecatedMode() {
        val params = WindowManager.LayoutParams().apply {
            @Suppress("DEPRECATION")
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        assertEquals(
            "A window already carrying a deprecated mode must be migrated, not left alone.",
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            params.azAllowDisplayCutout().layoutInDisplayCutoutMode,
        )
    }
}

/**
 * The host's safe zones must clear the display cutout as well as the system bars: under Android 15's
 * enforced edge-to-edge a cutout can occlude an edge that carries no system bar, which is exactly
 * the landscape short edge the rail docks against.
 */
class AzMaxEdgesTest {

    private val bars = PaddingValues(start = 0.dp, top = 24.dp, end = 48.dp, bottom = 16.dp)
    private val cutout = PaddingValues(start = 36.dp, top = 8.dp, end = 0.dp, bottom = 0.dp)

    @Test
    fun takesTheLargerInsetOnEveryEdge() {
        val insets = azMaxEdges(bars, cutout, LayoutDirection.Ltr)
        assertEquals(
            "An edge inset by the cutout alone — no system bar on it — must still be cleared. " +
                "This is the case Android 15 introduced and the bars-only math missed.",
            36.dp,
            insets.start,
        )
        assertEquals("The status bar is taller than the cutout here, so it wins.", 24.dp, insets.top)
        assertEquals("A bar-only edge is unaffected by a zero cutout.", 48.dp, insets.end)
        assertEquals(16.dp, insets.bottom)
    }

    @Test
    fun keepsInsetsDirectionRelative() {
        // Both inputs are direction-relative (built with start/end), so the resolved insets must be
        // too: "start" stays the start edge under RTL rather than flipping to the other side. The
        // mapping to physical left/right belongs to whoever needs physical coordinates.
        assertEquals(
            azMaxEdges(bars, cutout, LayoutDirection.Ltr),
            azMaxEdges(bars, cutout, LayoutDirection.Rtl),
        )
    }

    @Test
    fun isZeroWhenNothingOccludesTheWindow() {
        val none = PaddingValues(0.dp)
        assertEquals(
            "With no bars and no cutout the host must add no inset at all — content runs fully " +
                "edge-to-edge.",
            AzEdgeInsets(),
            azMaxEdges(none, none, LayoutDirection.Ltr),
        )
    }
}
