package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Controller for managing tutorial states, allowing external systems to initiate,
 * end, and track read status for tutorials.
 */
class AzTutorialController(
    initialActiveTutorialId: String? = null,
    initialReadTutorials: List<String> = emptyList()
) {
    private val _activeTutorialId = mutableStateOf<String?>(initialActiveTutorialId)

    /** The ID of the currently active tutorial. Null if no tutorial is active. */
    val activeTutorialId: State<String?> get() = _activeTutorialId

    private val _readTutorials = mutableStateListOf<String>().apply {
        addAll(initialReadTutorials)
    }

    /** A list of tutorial IDs that have been marked as read. */
    val readTutorials: List<String> get() = _readTutorials

    /**
     * Starts a tutorial with the given [id].
     */
    fun startTutorial(id: String) {
        _activeTutorialId.value = id
    }

    /**
     * Ends the currently active tutorial.
     */
    fun endTutorial() {
        _activeTutorialId.value = null
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

    companion object {
        val Saver: Saver<AzTutorialController, List<Any?>> = Saver(
            save = { listOf(it.activeTutorialId.value, ArrayList(it._readTutorials)) },
            restore = { list ->
                @Suppress("UNCHECKED_CAST")
                AzTutorialController(
                    initialActiveTutorialId = list[0] as String?,
                    initialReadTutorials = list[1] as ArrayList<String>
                )
            }
        )
    }
}

/**
 * CompositionLocal to provide the [AzTutorialController] to the tree.
 *
 * A controller is expected to be provided by a parent composable; the default
 * fails fast to surface missing providers during development.
 */
val LocalAzTutorialController = compositionLocalOf<AzTutorialController> {
    error("AzTutorialController not provided")
}

@Composable
fun rememberAzTutorialController(): AzTutorialController {
    return rememberSaveable(saver = AzTutorialController.Saver) {
        AzTutorialController()
    }
}
