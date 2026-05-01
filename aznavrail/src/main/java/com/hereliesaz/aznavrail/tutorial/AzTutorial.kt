package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

/** Defines the condition that must be met for a tutorial card to advance. */
sealed class AzAdvanceCondition {
    /** The user taps an explicit "next" button rendered on the card. */
    object Button : AzAdvanceCondition()

    /** The user taps the highlighted target area on screen. */
    object TapTarget : AzAdvanceCondition()

    /** The user taps anywhere on screen. */
    object TapAnywhere : AzAdvanceCondition()

    /**
     * Advancement is triggered programmatically by firing a named event.
     *
     * @param name The name of the event that triggers advancement.
     */
    data class Event(val name: String) : AzAdvanceCondition()
}

/** Describes what area of the screen should be highlighted during a tutorial card. */
sealed class AzHighlight {
    /**
     * Highlights a specific rectangular region on screen.
     *
     * @param bounds The bounding rectangle to highlight.
     */
    data class Area(val bounds: Rect) : AzHighlight()

    /**
     * Highlights a nav-rail item identified by its string ID.
     *
     * @param id The ID of the nav-rail item to highlight.
     */
    data class Item(val id: String) : AzHighlight()

    /** Highlights the entire screen. */
    object FullScreen : AzHighlight()

    /** No highlight; the card is displayed without any overlay. */
    object None : AzHighlight()
}

// Note: lambda fields (onAction, mediaContent) use reference equality; equals/copy behave accordingly.
/** A single tutorial card displayed to the user within a scene. */
data class AzCard(
    /** The bold title text shown at the top of the card. */
    val title: String,
    /** The body text explaining this step of the tutorial. */
    val text: String,
    /** The highlight applied to the screen while this card is shown. */
    val highlight: AzHighlight = AzHighlight.None,
    /** The condition that must be satisfied before advancing past this card. */
    val advanceCondition: AzAdvanceCondition = AzAdvanceCondition.Button,
    /** The label for the advance action button. */
    val actionText: String = "Next",
    /** Optional callback invoked when the advance action is triggered. */
    val onAction: (() -> Unit)? = null,
    /** Branch destinations keyed by branch-variable value, used for conditional navigation. */
    val branches: Map<String, String> = emptyMap(),
    /** Optional composable rendered inside the card as media content. */
    val mediaContent: (@Composable () -> Unit)? = null,
    /** Optional list of checklist item labels to display on the card. */
    val checklistItems: List<String>? = null,
)

// Note: lambda fields (content, onComplete) use reference equality; equals/copy behave accordingly.
/** A single scene within a tutorial, containing a composable background and one or more cards. */
data class AzScene(
    /** Unique identifier for this scene, used for branching and navigation. */
    val id: String,
    /** The composable content rendered as the scene background. */
    val content: @Composable () -> Unit,
    /** The ordered list of cards to display in this scene. */
    val cards: List<AzCard>,
    /** Optional callback invoked when this scene completes. */
    val onComplete: (() -> Unit)? = null,
    /** The name of the variable whose value controls scene branching. */
    val branchVar: String? = null,
    /** Branch destinations keyed by branch-variable value. */
    val branches: Map<String, String> = emptyMap(),
)

// Note: lambda fields (onComplete, onSkip) use reference equality; equals/copy behave accordingly.
/** The top-level tutorial model containing all scenes and completion callbacks. */
data class AzTutorial(
    /** The ordered list of scenes that make up the tutorial. */
    val scenes: List<AzScene>,
    /** Optional callback invoked when the tutorial completes normally. */
    val onComplete: (() -> Unit)? = null,
    /** Optional callback invoked when the user skips the tutorial. */
    val onSkip: (() -> Unit)? = null,
)

/** DSL builder for constructing an [AzTutorial]. Use [azTutorial] to create instances. */
class AzTutorialBuilder {
    private val scenes = mutableListOf<AzScene>()
    private var onComplete: (() -> Unit)? = null
    private var onSkip: (() -> Unit)? = null

    /** Sets the callback invoked when the tutorial finishes normally. */
    fun onComplete(action: () -> Unit) { onComplete = action }

    /** Sets the callback invoked when the user skips the tutorial. */
    fun onSkip(action: () -> Unit) { onSkip = action }

    /**
     * Adds a scene to the tutorial.
     *
     * @param id Unique identifier for the scene.
     * @param onComplete Optional callback invoked when this scene completes.
     * @param content The composable rendered as the scene background.
     * @param block Lambda with [AzSceneBuilder] receiver for configuring cards and branches.
     */
    fun scene(
        id: String,
        onComplete: (() -> Unit)? = null,
        content: @Composable () -> Unit,
        block: AzSceneBuilder.() -> Unit,
    ) {
        val builder = AzSceneBuilder()
        builder.block()
        scenes.add(AzScene(
            id = id,
            content = content,
            cards = builder.buildCards(),
            onComplete = onComplete,
            branchVar = builder.branchVar,
            branches = builder.branches,
        ))
    }

    /** Builds and returns the completed [AzTutorial]. */
    fun build(): AzTutorial = AzTutorial(scenes, onComplete, onSkip)
}

/** DSL builder for constructing the cards and branch configuration of an [AzScene]. */
class AzSceneBuilder {
    private val cards = mutableListOf<AzCard>()
    internal var branchVar: String? = null
    internal var branches: Map<String, String> = emptyMap()

    /**
     * Configures conditional branching for this scene.
     *
     * @param varName The name of the variable whose value determines the branch taken.
     * @param branches Map of variable values to destination scene IDs.
     */
    fun branch(varName: String, branches: Map<String, String>) {
        this.branchVar = varName
        this.branches = branches
    }

    /**
     * Adds a card to this scene.
     *
     * @param title The bold title displayed on the card.
     * @param text The body text displayed on the card.
     * @param highlight The screen highlight applied while this card is shown.
     * @param advanceCondition The condition required to advance past this card.
     * @param actionText The label for the advance action button.
     * @param onAction Optional callback invoked when the advance action fires.
     * @param branches Branch destinations keyed by branch-variable value.
     * @param mediaContent Optional composable rendered as media inside the card.
     * @param checklistItems Optional list of checklist item labels for the card.
     */
    fun card(
        title: String,
        text: String,
        highlight: AzHighlight = AzHighlight.None,
        advanceCondition: AzAdvanceCondition = AzAdvanceCondition.Button,
        actionText: String = "Next",
        onAction: (() -> Unit)? = null,
        branches: Map<String, String> = emptyMap(),
        mediaContent: (@Composable () -> Unit)? = null,
        checklistItems: List<String>? = null,
    ) {
        cards.add(AzCard(title, text, highlight, advanceCondition, actionText, onAction, branches, mediaContent, checklistItems))
    }

    /** Returns the list of cards added to this scene. */
    fun buildCards(): List<AzCard> = cards
}

/**
 * DSL entry point for constructing an [AzTutorial].
 *
 * @param block Lambda with [AzTutorialBuilder] receiver for configuring scenes and callbacks.
 * @return The fully constructed [AzTutorial].
 */
fun azTutorial(block: AzTutorialBuilder.() -> Unit): AzTutorial {
    val builder = AzTutorialBuilder()
    builder.block()
    return builder.build()
}
