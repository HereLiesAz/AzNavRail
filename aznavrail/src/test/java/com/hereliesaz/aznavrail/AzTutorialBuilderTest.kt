package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.tutorial.AzAdvanceCondition
import com.hereliesaz.aznavrail.tutorial.AzHighlight
import com.hereliesaz.aznavrail.tutorial.AzTutorialBuilder
import com.hereliesaz.aznavrail.tutorial.azTutorial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural integration test for the [AzTutorialBuilder] DSL. Covers a non-trivial tutorial with
 * multiple scenes, all three [AzAdvanceCondition] variants, all [AzHighlight] variants, branching,
 * checklists, and the top-level onComplete / onSkip callbacks.
 *
 * Failure messages here describe *which* part of the DSL plumbing is likely broken so the next
 * person can jump straight to the right builder.
 */
class AzTutorialBuilderTest {

    @Test
    fun fullBuilder_producesAllScenesWithCorrectStructure() {
        var completed = false
        var skipped = false

        val tutorial = azTutorial {
            onComplete { completed = true }
            onSkip { skipped = true }

            // Scene 1: linear, two cards, tap-target then tap-anywhere advancement.
            scene(id = "intro", content = {}) {
                card(
                    title = "Welcome",
                    text = "Tap the highlighted button.",
                    highlight = AzHighlight.Item("home"),
                    advanceCondition = AzAdvanceCondition.TapTarget,
                    actionText = "Continue",
                )
                card(
                    title = "Anywhere",
                    text = "Tap anywhere to continue.",
                    highlight = AzHighlight.FullScreen,
                    advanceCondition = AzAdvanceCondition.TapAnywhere,
                )
            }

            // Scene 2: event-driven, checklist, custom media, per-card branches.
            scene(id = "settings", content = {}) {
                card(
                    title = "Open menu",
                    text = "Open the side menu.",
                    advanceCondition = AzAdvanceCondition.Event("menu_opened"),
                    checklistItems = listOf("Tap the rail", "Find Settings"),
                    branches = mapOf("toggle-on" to "after-on", "toggle-off" to "after-off"),
                )
            }

            // Scene 3: scene-level branch, no cards.
            scene(id = "gate", content = {}) {
                branch(varName = "skill", mapOf("beginner" to "intro", "pro" to "settings"))
            }
        }

        // --- Top-level callbacks ---
        assertEquals(
            "AzTutorialBuilder must register exactly three scenes (intro, settings, gate). " +
                "Got ${tutorial.scenes.map { it.id }}.",
            3,
            tutorial.scenes.size,
        )
        assertNotNull("onComplete lambda must round-trip from the DSL to the AzTutorial.", tutorial.onComplete)
        assertNotNull("onSkip lambda must round-trip from the DSL to the AzTutorial.", tutorial.onSkip)
        tutorial.onComplete!!.invoke()
        tutorial.onSkip!!.invoke()
        assertTrue("Invoking tutorial.onComplete() must run the body registered via onComplete { } in the DSL.", completed)
        assertTrue("Invoking tutorial.onSkip() must run the body registered via onSkip { } in the DSL.", skipped)

        // --- Scene 1 ---
        val intro = tutorial.scenes[0]
        assertEquals(
            "Scene order must follow declaration order — first scene should be 'intro', got '${intro.id}'.",
            "intro",
            intro.id,
        )
        assertEquals(
            "Scene 'intro' should have two cards as declared — got ${intro.cards.size}.",
            2,
            intro.cards.size,
        )
        val card0 = intro.cards[0]
        assertEquals(
            "card[0].advanceCondition should be TapTarget; got ${card0.advanceCondition}. " +
                "Verify AzSceneBuilder.card propagates the parameter unchanged.",
            AzAdvanceCondition.TapTarget,
            card0.advanceCondition,
        )
        assertEquals(
            "card[0].highlight should be Item('home'); got ${card0.highlight}.",
            AzHighlight.Item("home"),
            card0.highlight,
        )
        assertEquals(
            "card[0].actionText should be 'Continue' (overridden from default 'Next'); got '${card0.actionText}'.",
            "Continue",
            card0.actionText,
        )

        val card1 = intro.cards[1]
        assertEquals(
            "card[1].advanceCondition should be TapAnywhere; got ${card1.advanceCondition}.",
            AzAdvanceCondition.TapAnywhere,
            card1.advanceCondition,
        )
        assertEquals(
            "card[1].highlight should be FullScreen; got ${card1.highlight}.",
            AzHighlight.FullScreen,
            card1.highlight,
        )

        // --- Scene 2 ---
        val settings = tutorial.scenes[1]
        val s2card = settings.cards[0]
        assertTrue(
            "card.advanceCondition should be Event(...); got ${s2card.advanceCondition}.",
            s2card.advanceCondition is AzAdvanceCondition.Event,
        )
        assertEquals(
            "Event name should round-trip from DSL; got ${(s2card.advanceCondition as AzAdvanceCondition.Event).name}.",
            "menu_opened",
            (s2card.advanceCondition as AzAdvanceCondition.Event).name,
        )
        assertEquals(
            "Checklist items must round-trip from the DSL; got ${s2card.checklistItems}.",
            listOf("Tap the rail", "Find Settings"),
            s2card.checklistItems,
        )
        assertEquals(
            "Per-card branches must round-trip from the DSL; got ${s2card.branches}.",
            mapOf("toggle-on" to "after-on", "toggle-off" to "after-off"),
            s2card.branches,
        )

        // --- Scene 3 ---
        val gate = tutorial.scenes[2]
        assertTrue(
            "A scene declared with only `branch { ... }` and no cards must produce an empty card list. " +
                "Got ${gate.cards.size} cards: ${gate.cards.map { it.title }}.",
            gate.cards.isEmpty(),
        )
        assertEquals(
            "Scene-level branchVar must propagate from AzSceneBuilder.branch() to AzScene.branchVar; got '${gate.branchVar}'.",
            "skill",
            gate.branchVar,
        )
        assertEquals(
            "Scene-level branches must propagate to AzScene.branches; got ${gate.branches}.",
            mapOf("beginner" to "intro", "pro" to "settings"),
            gate.branches,
        )
    }

    @Test
    fun builderWithoutCallbacks_yieldsNullCallbacks() {
        val tutorial = azTutorial {
            scene(id = "only", content = {}) {
                card(title = "T", text = "X")
            }
        }
        assertNull(
            "If the DSL never calls onComplete { }, tutorial.onComplete must remain null — " +
                "otherwise downstream code can't distinguish 'unset' from 'no-op'. Got ${tutorial.onComplete}.",
            tutorial.onComplete,
        )
        assertNull(
            "If the DSL never calls onSkip { }, tutorial.onSkip must remain null. Got ${tutorial.onSkip}.",
            tutorial.onSkip,
        )
    }

    @Test
    fun cardDefaults_areCorrect() {
        val tutorial = azTutorial {
            scene(id = "s", content = {}) {
                card(title = "T", text = "X")
            }
        }
        val c = tutorial.scenes[0].cards[0]
        assertEquals(
            "card() with no explicit advanceCondition must default to Button — got ${c.advanceCondition}.",
            AzAdvanceCondition.Button,
            c.advanceCondition,
        )
        assertEquals(
            "card() with no explicit highlight must default to None — got ${c.highlight}.",
            AzHighlight.None,
            c.highlight,
        )
        assertEquals(
            "card() with no explicit actionText must default to 'Next' — got '${c.actionText}'.",
            "Next",
            c.actionText,
        )
        assertNull("card() with no checklistItems must default to null — got ${c.checklistItems}.", c.checklistItems)
        assertNull("card() with no mediaContent must default to null — got ${c.mediaContent}.", c.mediaContent)
        assertNull("card() with no onAction must default to null — got ${c.onAction}.", c.onAction)
    }
}
