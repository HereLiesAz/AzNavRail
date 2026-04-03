package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

/**
 * Defines a highlight method for a tutorial scene or card.
 */
sealed class AzHighlight {
    /** Highlights a specific area defined by the given [Rect] bounds. */
    data class Area(val bounds: Rect) : AzHighlight()
    /** Highlights an item based on its ID. The system must find the item's globally positioned bounds. */
    data class Item(val id: String) : AzHighlight()
    /** Highlights the entire screen or provides a general highlight without a specific bounds. */
    object FullScreen : AzHighlight()
    /** No highlight. */
    object None : AzHighlight()
}

/**
 * Represents a textual or interactive card shown during a scene.
 */
data class AzCard(
    val title: String,
    val text: String,
    val highlight: AzHighlight = AzHighlight.None,
    val actionText: String = "Next",
    val onAction: (() -> Unit)? = null
)

/**
 * A scene is a scripted screen, taken directly from the app.
 * It may have different conditions for completing its loop than the app has for its scene.
 *
 * @param id Unique identifier for the scene.
 * @param content The composable content of the scene. This should display a copy of a screen
 *                with defined initial conditions.
 * @param cards The series of cards to display during this scene.
 * @param onComplete Callback invoked when the scene completes all its cards or is manually finished.
 */
data class AzScene(
    val id: String,
    val content: @Composable () -> Unit,
    val cards: List<AzCard>,
    val onComplete: (() -> Unit)? = null
)

/**
 * A complete tutorial comprising multiple scenes.
 */
data class AzTutorial(
    val scenes: List<AzScene>
)

/**
 * Custom scripting language / DSL to build tutorials.
 */
class AzTutorialBuilder {
    private val scenes = mutableListOf<AzScene>()

    fun scene(id: String, onComplete: (() -> Unit)? = null, content: @Composable () -> Unit, block: AzSceneBuilder.() -> Unit) {
        val builder = AzSceneBuilder()
        builder.block()
        scenes.add(AzScene(id, content, builder.build(), onComplete))
    }

    fun build(): AzTutorial = AzTutorial(scenes)
}

class AzSceneBuilder {
    private val cards = mutableListOf<AzCard>()

    fun card(title: String, text: String, highlight: AzHighlight = AzHighlight.None, actionText: String = "Next", onAction: (() -> Unit)? = null) {
        cards.add(AzCard(title, text, highlight, actionText, onAction))
    }

    fun build(): List<AzCard> = cards
}

/**
 * DSL entry point to build an AzTutorial.
 */
fun azTutorial(block: AzTutorialBuilder.() -> Unit): AzTutorial {
    val builder = AzTutorialBuilder()
    builder.block()
    return builder.build()
}
