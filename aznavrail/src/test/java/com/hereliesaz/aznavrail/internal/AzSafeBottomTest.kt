package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AzSafeBottomTest {

    @Test
    fun gestureNav_imposesZeroBottomMargin() {
        assertEquals(
            "Gesture navigation must drop ALL library bottom margin — both the 10% content " +
                "safe-zone and the nav-bar inset — so content runs edge-to-edge.",
            0.dp,
            azResolveSafeBottom(gestureNav = true, contentSafeZone = 80.dp, systemBarInset = 24.dp),
        )
    }

    @Test
    fun buttonNav_keepsLargerOfSafeZoneAndInset() {
        assertEquals(
            "Button navigation keeps the larger of the content safe-zone and the nav-bar inset.",
            80.dp,
            azResolveSafeBottom(gestureNav = false, contentSafeZone = 80.dp, systemBarInset = 48.dp),
        )
        assertEquals(
            "When the nav-bar inset is larger it wins.",
            96.dp,
            azResolveSafeBottom(gestureNav = false, contentSafeZone = 80.dp, systemBarInset = 96.dp),
        )
    }
}
