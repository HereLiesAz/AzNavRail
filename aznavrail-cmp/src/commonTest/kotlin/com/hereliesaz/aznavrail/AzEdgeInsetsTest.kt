package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.internal.AzEdgeInsets
import com.hereliesaz.aznavrail.internal.azMaxEdges
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The host's safe zones clear the system bars **union** the display cutout.
 *
 * The union is not belt-and-braces. From Android 15 an app targeting SDK 35 is laid out edge-to-edge
 * with `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` in force, so a camera cutout can occlude an edge that
 * carries no system bar at all — the landscape short edge, which is exactly where the rail docks.
 * Taking only the bar insets there leaves content under the camera.
 */
class AzEdgeInsetsTest {

    private val bars = PaddingValues(start = 0.dp, top = 24.dp, end = 48.dp, bottom = 16.dp)
    private val cutout = PaddingValues(start = 36.dp, top = 8.dp, end = 0.dp, bottom = 0.dp)

    @Test
    fun takesTheLargerInsetOnEveryEdge() {
        val insets = azMaxEdges(bars, cutout, LayoutDirection.Ltr)
        assertEquals(36.dp, insets.start, "An edge inset by the cutout alone must still be cleared.")
        assertEquals(24.dp, insets.top, "The status bar is taller than the cutout here, so it wins.")
        assertEquals(48.dp, insets.end, "A bar-only edge is unaffected by a zero cutout.")
        assertEquals(16.dp, insets.bottom)
    }

    @Test
    fun keepsInsetsDirectionRelative() {
        // Both inputs are direction-relative (built with start/end), so the result must be too:
        // "start" stays the start edge under RTL instead of flipping to the other side.
        assertEquals(
            azMaxEdges(bars, cutout, LayoutDirection.Ltr),
            azMaxEdges(bars, cutout, LayoutDirection.Rtl),
        )
    }

    @Test
    fun isZeroWhenNothingOccludesTheWindow() {
        val none = PaddingValues(0.dp)
        assertEquals(
            AzEdgeInsets(),
            azMaxEdges(none, none, LayoutDirection.Ltr),
            "With no bars and no cutout the host adds no inset — content runs fully edge-to-edge.",
        )
    }
}
