package com.hereliesaz.aznavrail.internal

import org.junit.Assert.assertEquals
import org.junit.Test

/** The fold cascade must take the same total time whatever the item count. */
class AzCascadeDelayTest {

    @Test
    fun totalCascadeIsCountIndependent() {
        // 22 ms at the 8-item reference → 176 ms total, literal.
        assertEquals(176L, azCascadeDelayMs(3, 3, 22))
        assertEquals(176L, azCascadeDelayMs(12, 12, 22))
        assertEquals(176L, azCascadeDelayMs(8, 8, 22))
    }

    @Test
    fun fewerItemsGetWiderGaps() {
        // 22 × 8 / 3 = 58.67 → 58; 22 × 8 / 12 = 14.67 → 14.
        assertEquals(58L, azCascadeDelayMs(1, 3, 22))
        assertEquals(14L, azCascadeDelayMs(1, 12, 22))
    }

    @Test
    fun degenerateInputsDoNotThrow() {
        assertEquals(0L, azCascadeDelayMs(0, 0, 22))
        assertEquals(0L, azCascadeDelayMs(-2, 5, 22))
        assertEquals(22L, azCascadeDelayMs(1, 0, 22))
    }
}
