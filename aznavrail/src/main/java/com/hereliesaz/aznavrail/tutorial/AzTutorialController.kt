package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Controller for managing tutorial states, allowing external systems to initiate,
 * end, and track read status for tutorials.
 */
class AzTutorialController {
    /** The ID of the currently active tutorial. Null if no tutorial is active. */
    private val _activeTutorialId = mutableStateOf<String?>(null)
    val activeTutorialId: androidx.compose.runtime.State<String?> get() = _activeTutorialId

    private val _readTutorials = mutableStateListOf<String>()

    /** A list of tutorial IDs that have been marked as read. */
    val readTutorials: List<String> get() = _readTutorials

    /**
     * Starts a tutorial with the given [id].
     */
    fun startTutorial(id: String) {
        activeTutorialId.value = id
    }

    /**
     * Ends the currently active tutorial.
     */
    fun endTutorial() {
        activeTutorialId.value = null
    }

    /**
     * Marks a tutorial as read.
     */
    fun markTutorialRead(id: String) {
        if (!_readTutorials.contains(id)) {
            _readTutorials.add(id)
        }
    }

    /**
     * Checks if a tutorial has been marked as read.
     */
    fun isTutorialRead(id: String): Boolean {
        return _readTutorials.contains(id)
    }
}

/**
 * CompositionLocal to provide the [AzTutorialController] to the tree.
 */
val LocalAzTutorialController = androidx.compose.runtime.staticCompositionLocalOf<AzTutorialController> {
    error("No AzTutorialController provided. Ensure your content is wrapped in AzHostActivityLayout.")
}

@Composable
fun rememberAzTutorialController(): AzTutorialController {
    return remember { AzTutorialController() }
}
