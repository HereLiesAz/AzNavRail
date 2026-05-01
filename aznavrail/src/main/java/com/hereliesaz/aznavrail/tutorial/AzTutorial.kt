package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

sealed class AzAdvanceCondition {
    object Button : AzAdvanceCondition()
    object TapTarget : AzAdvanceCondition()
    object TapAnywhere : AzAdvanceCondition()
    data class Event(val name: String) : AzAdvanceCondition()
}

sealed class AzHighlight {
    data class Area(val bounds: Rect) : AzHighlight()
    data class Item(val id: String) : AzHighlight()
    object FullScreen : AzHighlight()
    object None : AzHighlight()
}

data class AzCard(
    val title: String,
    val text: String,
    val highlight: AzHighlight = AzHighlight.None,
    val advanceCondition: AzAdvanceCondition = AzAdvanceCondition.Button,
    val actionText: String = "Next",
    val onAction: (() -> Unit)? = null,
    val branches: Map<String, String> = emptyMap(),
    val mediaContent: (@Composable () -> Unit)? = null,
    val checklistItems: List<String>? = null,
)

data class AzScene(
    val id: String,
    val content: @Composable () -> Unit,
    val cards: List<AzCard>,
    val onComplete: (() -> Unit)? = null,
    val branchVar: String? = null,
    val branches: Map<String, String> = emptyMap(),
)

data class AzTutorial(
    val scenes: List<AzScene>,
    val onComplete: (() -> Unit)? = null,
    val onSkip: (() -> Unit)? = null,
)

class AzTutorialBuilder {
    private val scenes = mutableListOf<AzScene>()
    private var onComplete: (() -> Unit)? = null
    private var onSkip: (() -> Unit)? = null

    fun onComplete(action: () -> Unit) { onComplete = action }
    fun onSkip(action: () -> Unit) { onSkip = action }

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

    fun build(): AzTutorial = AzTutorial(scenes, onComplete, onSkip)
}

class AzSceneBuilder {
    private val cards = mutableListOf<AzCard>()
    internal var branchVar: String? = null
    internal var branches: Map<String, String> = emptyMap()

    fun branch(varName: String, branches: Map<String, String>) {
        this.branchVar = varName
        this.branches = branches
    }

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

    fun buildCards(): List<AzCard> = cards
}

fun azTutorial(block: AzTutorialBuilder.() -> Unit): AzTutorial {
    val builder = AzTutorialBuilder()
    builder.block()
    return builder.build()
}
