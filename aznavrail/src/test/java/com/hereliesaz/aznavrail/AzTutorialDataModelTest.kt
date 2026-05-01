package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.tutorial.*
import org.junit.Assert.*
import org.junit.Test

class AzTutorialDataModelTest {

    @Test
    fun `azTutorial DSL builds scenes and cards with defaults`() {
        val tutorial = azTutorial {
            scene(id = "s1", content = {}) {
                card(title = "T1", text = "Body1")
            }
        }
        assertEquals(1, tutorial.scenes.size)
        assertEquals(1, tutorial.scenes[0].cards.size)
        assertEquals("T1", tutorial.scenes[0].cards[0].title)
        assertEquals(AzAdvanceCondition.Button, tutorial.scenes[0].cards[0].advanceCondition)
        assertEquals(emptyMap<String, String>(), tutorial.scenes[0].cards[0].branches)
        assertNull(tutorial.scenes[0].cards[0].mediaContent)
        assertNull(tutorial.scenes[0].cards[0].checklistItems)
    }

    @Test
    fun `card stores advanceCondition and branches`() {
        val tutorial = azTutorial {
            scene(id = "s1", content = {}) {
                card(
                    title = "Choose",
                    text = "Pick one",
                    advanceCondition = AzAdvanceCondition.TapTarget,
                    branches = mapOf("btn-a" to "scene-a", "btn-b" to "scene-b")
                )
            }
        }
        val card = tutorial.scenes[0].cards[0]
        assertEquals(AzAdvanceCondition.TapTarget, card.advanceCondition)
        assertEquals(mapOf("btn-a" to "scene-a", "btn-b" to "scene-b"), card.branches)
    }

    @Test
    fun `card stores Event advanceCondition`() {
        val tutorial = azTutorial {
            scene(id = "s1", content = {}) {
                card(title = "T", text = "X", advanceCondition = AzAdvanceCondition.Event("menu_opened"))
            }
        }
        val cond = tutorial.scenes[0].cards[0].advanceCondition
        assertTrue(cond is AzAdvanceCondition.Event)
        assertEquals("menu_opened", (cond as AzAdvanceCondition.Event).name)
    }

    @Test
    fun `card stores checklistItems`() {
        val items = listOf("Step 1", "Step 2")
        val tutorial = azTutorial {
            scene(id = "s1", content = {}) {
                card(title = "Check", text = "Do these:", checklistItems = items)
            }
        }
        assertEquals(items, tutorial.scenes[0].cards[0].checklistItems)
    }

    @Test
    fun `branch DSL sets branchVar and branches on scene`() {
        val tutorial = azTutorial {
            scene(id = "gate", content = {}) {
                branch(varName = "level", mapOf("advanced" to "scene-a", "basic" to "scene-b"))
            }
        }
        val scene = tutorial.scenes[0]
        assertEquals("level", scene.branchVar)
        assertEquals(mapOf("advanced" to "scene-a", "basic" to "scene-b"), scene.branches)
        assertTrue(scene.cards.isEmpty())
    }

    @Test
    fun `AzTutorial onComplete and onSkip DSL functions work`() {
        var completed = false
        var skipped = false
        val tutorial = azTutorial {
            onComplete { completed = true }
            onSkip { skipped = true }
            scene(id = "s1", content = {}) { card(title = "T", text = "X") }
        }
        tutorial.onComplete?.invoke()
        tutorial.onSkip?.invoke()
        assertTrue(completed)
        assertTrue(skipped)
    }
}
