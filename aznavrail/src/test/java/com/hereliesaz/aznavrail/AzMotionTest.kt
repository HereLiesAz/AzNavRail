package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.internal.azCascadeDelayMs
import com.hereliesaz.aznavrail.model.AzMotion
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the motion scale against drifting back to where it was.
 *
 * The values these replaced — a 60 ms stagger and a 720 ms item duration — meant an eight-item
 * drawer took 1.2 seconds to arrive. A budget is the only thing that stops that recurring. Since
 * cascades are count-normalized ([AzMotion.CascadeReferenceCount]), the budget must hold for any
 * item count, not just a worst case.
 */
class AzMotionTest {

    /** Every rail from 1 to 40 items settles inside two-thirds of a second. */
    @Test
    fun fullCascadeSettlesWithinBudgetAtAnyCount() {
        for (n in 1..40) {
            val settleMs = azCascadeDelayMs(n - 1, n, AzMotion.ItemStaggerMs) + AzMotion.ItemDurationMs
            assertTrue("$n items settle in ${settleMs}ms; budget is 650ms.", settleMs <= 650)
        }
    }

    /** The last item's start never reaches the fixed span (22 ms × 8 = 176 ms, written literally). */
    @Test
    fun lastItemStartsInsideTheFixedSpan() {
        for (n in 1..40) {
            val lastStart = azCascadeDelayMs(n - 1, n, AzMotion.ItemStaggerMs)
            assertTrue("$n items: last start ${lastStart}ms is not under 176ms.", lastStart < 176)
        }
    }

    /** A stagger approaching the item duration turns a cascade into a queue. */
    @Test
    fun staggerIsMuchShorterThanTheItemItStaggers() {
        assertTrue(AzMotion.ItemStaggerMs * 4 <= AzMotion.ItemDurationMs)
    }

    /** A panel must never outlast the items inside it. */
    @Test
    fun panelNeverOutlastsItsContents() {
        assertTrue(AzMotion.PanelDurationMs <= AzMotion.ItemDurationMs)
    }

    /** A zero here silently disables an animation. */
    @Test
    fun everyValueIsPositive() {
        listOf(
            AzMotion.ItemDurationMs, AzMotion.ItemStaggerMs, AzMotion.PanelDurationMs,
            AzMotion.SettleDurationMs, AzMotion.IndicatorStepMs, AzMotion.CascadeReferenceCount,
        ).forEach { assertTrue(it > 0) }
    }
}
