package com.hereliesaz.aznavrail.tutorial

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the guidance-framework expansion: paged steps + reactive/manual advance, arbitrary-shape
 * highlight resolution, the highlight-directive precedence, and the suppression predicate combiner.
 */
class AzGuidanceExpansionTest {

    // a --(3 paged steps)--> done
    private val stepped = AzEdge(
        from = "a",
        to = "done",
        instruction = AzInstruction("step0"),
        steps = listOf(
            AzInstructionStep("step0"),
            AzInstructionStep("step1", highlightTargetId = "ball", advanceWhen = "x"),
            AzInstructionStep("step2"),
        ),
    )
    private val goals = mapOf("g" to AzGoal(id = "g", target = "done"))

    @Test
    fun `paged edge shows the step at the cursor`() {
        val f0 = routeInstructions(listOf(stepped), goals, listOf("g"), setOf("a"), stepIndexOf = { 0 })
        assertEquals("step0", f0.instructions.single().text)
        assertEquals(0, f0.resolved.single().stepIndex)
        assertEquals(3, f0.resolved.single().stepTotal)

        val f1 = routeInstructions(listOf(stepped), goals, listOf("g"), setOf("a"), stepIndexOf = { 1 })
        assertEquals("step1", f1.instructions.single().text)
        assertEquals(AzGuideHighlight.Target("ball"), f1.resolved.single().instruction.highlight)
    }

    @Test
    fun `reactive advanceWhen wins over the tap cursor`() {
        // Cursor parked on step1, whose advanceWhen ("x") is satisfied -> the overlay shows step2.
        val f = routeInstructions(listOf(stepped), goals, listOf("g"), setOf("a", "x"), stepIndexOf = { 1 })
        assertEquals("step2", f.instructions.single().text)
        assertEquals(2, f.resolved.single().stepIndex)
    }

    @Test
    fun `an informational step is not skipped when a later step's status is already true`() {
        // step0 has no advanceWhen, so it stays even though "x" (step1's trigger) is true.
        val f = routeInstructions(listOf(stepped), goals, listOf("g"), setOf("a", "x"), stepIndexOf = { 0 })
        assertEquals("step0", f.instructions.single().text)
    }

    @Test
    fun `a paged goal is reached when its target status becomes true`() {
        val f = routeInstructions(listOf(stepped), goals, listOf("g"), setOf("a", "done"), stepIndexOf = { 1 })
        assertTrue("g" in f.reachedGoals)
        assertTrue(f.instructions.isEmpty())
    }

    @Test
    fun `single-callout edges still route unchanged (stepTotal 1)`() {
        val edge = AzEdge("a", "b", AzInstruction("Do A", highlight = AzGuideHighlight.Item("ia")))
        val g = mapOf("g" to AzGoal("g", "b"))
        val f = routeInstructions(listOf(edge), g, listOf("g"), setOf("a"))
        assertEquals("Do A", f.instructions.single().text)
        assertEquals(1, f.resolved.single().stepTotal)
    }

    @Test
    fun `resolveAzHighlight precedence is target, selector, active token, item, none`() {
        assertEquals(AzGuideHighlight.Target("t"), resolveAzHighlight("item", { "sel" }, "t"))
        assertTrue(resolveAzHighlight("item", { "sel" }, null) is AzGuideHighlight.Dynamic)
        assertEquals(AzGuideHighlight.ActiveItem, resolveAzHighlight(AZ_ITEM_ACTIVE, null, null))
        assertEquals(AzGuideHighlight.Item("item"), resolveAzHighlight("item", null, null))
        assertEquals(AzGuideHighlight.None, resolveAzHighlight(null, null, null))
    }

    @Test
    fun `resolveItemId folds ActiveItem and Dynamic against the live active id`() {
        assertEquals("home", AzGuideHighlight.ActiveItem.resolveItemId("home"))
        assertEquals("layer.1", AzGuideHighlight.Dynamic { "layer.1" }.resolveItemId(null))
        assertEquals("home", AzGuideHighlight.Dynamic { AZ_ITEM_ACTIVE }.resolveItemId("home"))
        assertNull(AzGuideHighlight.Target("t").resolveItemId("home"))
    }

    @Test
    fun `resolveShape resolves targets, areas and items, degrading to null`() {
        val circle = AzGuideShape.Circle(1f, 2f, 3f)
        val targets = mapOf<String, () -> AzGuideShape?>("t" to { circle }, "gone" to { null })
        assertEquals(circle, AzGuideHighlight.Target("t").resolveShape(emptyMap(), null, targets))
        assertNull(AzGuideHighlight.Target("gone").resolveShape(emptyMap(), null, targets))
        assertNull(AzGuideHighlight.Target("missing").resolveShape(emptyMap(), null, targets))
        assertTrue(AzGuideHighlight.Area(5f, 6f, 7f, 8f).resolveShape(emptyMap(), null, emptyMap()) is AzGuideShape.Rect)

        val cache = mapOf("home" to Rect(0f, 0f, 10f, 10f))
        assertTrue(AzGuideHighlight.Item("home").resolveShape(cache, null, emptyMap()) is AzGuideShape.Rect)
        assertNull(AzGuideHighlight.Item("nope").resolveShape(cache, null, emptyMap()))
        assertTrue(AzGuideHighlight.ActiveItem.resolveShape(cache, "home", emptyMap()) is AzGuideShape.Rect)
        assertNull(AzGuideHighlight.ActiveItem.resolveShape(cache, null, emptyMap()))
    }

    @Test
    fun `AzGuideShape bounds wraps each geometry with padding`() {
        val c = AzGuideShape.Circle(100f, 50f, 10f, padding = 5f).bounds()
        assertEquals(Rect(85f, 35f, 115f, 65f), c)
        val r = AzGuideShape.Rect(10f, 20f, 30f, 40f).bounds()
        assertEquals(Rect(10f, 20f, 40f, 60f), r)
        val p = AzGuideShape.Path(
            listOf(AzPathCmd.MoveTo(0f, 0f), AzPathCmd.QuadTo(5f, 30f, 10f, 20f), AzPathCmd.Close),
        ).bounds()
        assertEquals(Rect(0f, 0f, 10f, 30f), p)
    }

    @Test
    fun `anySuppressorActive ORs predicates and treats a throw as false`() {
        assertFalse(anySuppressorActive(emptyList()))
        assertTrue(anySuppressorActive(listOf(700L to { true })))
        assertFalse(anySuppressorActive(listOf(700L to { false })))
        assertTrue(anySuppressorActive(listOf(0L to { false }, 0L to { true })))
        assertFalse(anySuppressorActive(listOf(0L to { throw RuntimeException("boom") })))
    }
}
