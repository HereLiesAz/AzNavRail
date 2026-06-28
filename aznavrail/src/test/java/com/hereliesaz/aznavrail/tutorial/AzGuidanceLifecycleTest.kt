package com.hereliesaz.aznavrail.tutorial

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies skip/dismiss persistence, replay reset, and the no-repeat (consumed-status) routing. */
class AzGuidanceLifecycleTest {

    private fun edge(from: String, to: String?, text: String) = AzEdge(from, to, AzInstruction(text))

    @Test
    fun `a consumed hop is not re-shown`() {
        val edges = listOf(edge("a", "b", "Open menu"), edge("b", "c", "Tap X"))
        val goals = mapOf("g" to AzGoal("g", "c"))
        // From {a}: the first hop "Open menu" shows.
        assertEquals("Open menu", routeInstructions(edges, goals, listOf("g"), setOf("a")).instructions.single().text)
        // Once "b" is consumed (its hop was shown + taken), routing skips it even though we're back at {a}.
        val f = routeInstructions(edges, goals, listOf("g"), setOf("a"), consumedStatuses = setOf("b"))
        assertEquals("Tap X", f.instructions.single().text)
    }

    @Test
    fun `skip deactivates, persists dismissal, and blocks re-activation until reset`() {
        val c = AzGuidanceController()
        c.activate("g")
        assertTrue("g" in c.activeGoals)
        assertTrue(c.enabled)

        c.skip("g")
        assertFalse("g" in c.activeGoals)
        assertTrue(c.isDismissed("g"))

        c.activate("g") // respected: a skipped tutorial does not re-show
        assertFalse("g" in c.activeGoals)

        c.resetGuidance("g")
        assertFalse(c.isDismissed("g"))
        c.activate("g") // now it can replay
        assertTrue("g" in c.activeGoals)
    }

    @Test
    fun `global skip cancels tutorial mode`() {
        val c = AzGuidanceController()
        c.activate("g1"); c.activate("g2")
        c.skip()
        assertTrue(c.activeGoals.isEmpty())
        assertFalse(c.enabled)
        assertTrue(c.isDismissed("g1") && c.isDismissed("g2"))
    }

    @Test
    fun `completed goals are not re-activated until reset`() {
        val c = AzGuidanceController()
        c.activate("g"); c.markReached("g")
        assertTrue(c.isCompleted("g"))
        c.activate("g")
        assertFalse("g" in c.activeGoals)
        c.resetGuidance()
        c.activate("g")
        assertTrue("g" in c.activeGoals)
    }

    @Test
    fun `consume accumulates and disable clears it`() {
        val c = AzGuidanceController()
        c.consume("x")
        assertTrue("x" in c.consumedStatuses)
        c.disable()
        assertTrue(c.consumedStatuses.isEmpty())
    }
}
