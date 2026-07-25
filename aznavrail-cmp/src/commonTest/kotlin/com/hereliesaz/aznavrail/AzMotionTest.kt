package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.model.AzMotion
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the motion scale against drifting back to where it was.
 *
 * These are not aesthetic assertions. The values these replaced — a 60 ms stagger and a 720 ms item
 * duration, copy-pasted as literals into six places — meant an eight-item drawer took 1.2 seconds
 * to finish arriving, and because the turnstile entrance starts each item edge-on, the panel sat
 * there empty for the first stretch of it. A budget is the only thing that stops that recurring.
 */
class AzMotionTest {

    /**
     * A twelve-item drawer must be completely settled inside two-thirds of a second, counting both
     * the stagger to the last item and that item's own entrance. Anything slower stops reading as a
     * cascade and starts reading as a wait.
     */
    @Test
    fun fullCascadeSettlesWithinBudget() {
        val worstCaseItems = 12
        val settleMs = AzMotion.ItemStaggerMs * worstCaseItems + AzMotion.ItemDurationMs
        assertTrue(
            settleMs <= 650,
            "A $worstCaseItems-item cascade settles in ${settleMs}ms; the budget is 650ms. " +
                "Motion is a guide, not a toll.",
        )
    }

    /**
     * The stagger is a tick between neighbours, not a duration in its own right. Once it approaches
     * the item duration the cascade stops overlapping and becomes a queue.
     */
    @Test
    fun staggerIsMuchShorterThanTheItemItStaggers() {
        assertTrue(
            AzMotion.ItemStaggerMs * 4 <= AzMotion.ItemDurationMs,
            "Stagger ${AzMotion.ItemStaggerMs}ms is not comfortably shorter than the " +
                "${AzMotion.ItemDurationMs}ms item entrance it offsets, so items arrive one at a " +
                "time instead of as one gesture.",
        )
    }

    /**
     * A container arriving must never outlast the items inside it — otherwise the panel is still
     * fading in after its contents have finished, which is exactly the "empty panel hanging there"
     * the scale exists to prevent.
     */
    @Test
    fun panelNeverOutlastsItsContents() {
        assertTrue(
            AzMotion.PanelDurationMs <= AzMotion.ItemDurationMs,
            "Panel ${AzMotion.PanelDurationMs}ms outlasts its items' ${AzMotion.ItemDurationMs}ms.",
        )
    }

    /** Every value is a real, positive duration — a zero here silently disables an animation. */
    @Test
    fun everyDurationIsPositive() {
        val all = mapOf(
            "ItemDurationMs" to AzMotion.ItemDurationMs,
            "ItemStaggerMs" to AzMotion.ItemStaggerMs,
            "PanelDurationMs" to AzMotion.PanelDurationMs,
            "SettleDurationMs" to AzMotion.SettleDurationMs,
            "IndicatorStepMs" to AzMotion.IndicatorStepMs,
        )
        all.forEach { (name, value) ->
            assertTrue(value > 0, "AzMotion.$name is $value; durations must be positive.")
        }
    }
}
