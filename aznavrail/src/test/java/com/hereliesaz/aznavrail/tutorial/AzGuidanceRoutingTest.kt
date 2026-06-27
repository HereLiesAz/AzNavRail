package com.hereliesaz.aznavrail.tutorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies BFS next-hop routing and the multi-goal instruction frame. */
class AzGuidanceRoutingTest {

    private fun edge(from: String, to: String?, text: String, item: String? = null) = AzEdge(
        from = from,
        to = to,
        instruction = AzInstruction(
            text = text,
            highlight = item?.let { AzGuideHighlight.Item(it) } ?: AzGuideHighlight.None,
        ),
    )

    // a --tapA--> b --tapB--> c
    private val chain = listOf(
        edge("a", "b", "Do A", "ia"),
        edge("b", "c", "Do B", "ib"),
    )

    @Test
    fun `nextHop returns the first edge on the shortest path`() {
        val hop = nextHop(chain, activeStatuses = setOf("a"), target = "c")
        assertEquals("Do A", hop?.instruction?.text)
    }

    @Test
    fun `nextHop re-routes as the user advances`() {
        val hop = nextHop(chain, activeStatuses = setOf("b"), target = "c")
        assertEquals("Do B", hop?.instruction?.text)
    }

    @Test
    fun `nextHop is null when the target is already true`() {
        assertNull(nextHop(chain, activeStatuses = setOf("c"), target = "c"))
    }

    @Test
    fun `nextHop is null when the target is unreachable`() {
        assertNull(nextHop(chain, activeStatuses = setOf("x"), target = "c"))
    }

    @Test
    fun `nextHop prefers the shorter of two paths`() {
        // a -> c directly (1 hop) vs a -> b -> c (2 hops).
        val edges = chain + edge("a", "c", "Shortcut", "sc")
        val hop = nextHop(edges, activeStatuses = setOf("a"), target = "c")
        assertEquals("Shortcut", hop?.instruction?.text)
    }

    @Test
    fun `routeInstructions reports a goal already at its target as reached`() {
        val goals = mapOf("g" to AzGoal(id = "g", target = "c"))
        val frame = routeInstructions(chain, goals, listOf("g"), activeStatuses = setOf("c"))
        assertTrue("g" in frame.reachedGoals)
        assertTrue(frame.instructions.isEmpty())
    }

    @Test
    fun `routeInstructions surfaces a next hop per active goal simultaneously`() {
        // Two independent goals from the same root, each one hop away on a different control.
        val edges = listOf(
            edge("root", "p", "Tap P", "ip"),
            edge("root", "q", "Tap Q", "iq"),
        )
        val goals = mapOf(
            "gp" to AzGoal(id = "gp", target = "p"),
            "gq" to AzGoal(id = "gq", target = "q"),
        )
        val frame = routeInstructions(edges, goals, listOf("gp", "gq"), activeStatuses = setOf("root"))
        val texts = frame.instructions.map { it.text }.toSet()
        assertEquals(setOf("Tap P", "Tap Q"), texts)
        assertTrue(frame.reachedGoals.isEmpty())
    }

    @Test
    fun `routeInstructions dedupes a hop shared by two goals`() {
        // Both goals route through the same first edge a -> b.
        val goals = mapOf(
            "g1" to AzGoal(id = "g1", target = "b"),
            "g2" to AzGoal(id = "g2", target = "c"),
        )
        val frame = routeInstructions(chain, goals, listOf("g1", "g2"), activeStatuses = setOf("a"))
        assertEquals(listOf("Do A"), frame.instructions.map { it.text })
    }

    @Test
    fun `routeInstructions includes passive tips while their from holds`() {
        val edges = chain + edge("a", null, "Heads up", "ip")
        val goals = mapOf("g" to AzGoal(id = "g", target = "c"))
        val frame = routeInstructions(edges, goals, listOf("g"), activeStatuses = setOf("a"))
        val texts = frame.instructions.map { it.text }.toSet()
        assertTrue("Do A" in texts)
        assertTrue("Heads up" in texts)
    }
}
