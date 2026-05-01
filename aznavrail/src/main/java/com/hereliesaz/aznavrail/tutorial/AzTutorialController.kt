package com.hereliesaz.aznavrail.tutorial

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

private const val PREFS_NAME = "az_tutorial_prefs"
private const val PREF_KEY = "az_navrail_read_tutorials"

/** Controls tutorial lifecycle: start/end, event signaling, and read-state persistence. */
class AzTutorialController(
    initialActiveTutorialId: String? = null,
    initialReadTutorials: List<String> = emptyList(),
    private val prefs: SharedPreferences? = null,
) {
    private val _activeTutorialId = mutableStateOf<String?>(initialActiveTutorialId)
    /** The ID of the currently active tutorial, or null if none. */
    val activeTutorialId: State<String?> get() = _activeTutorialId

    private val _readTutorials = mutableStateListOf<String>().apply { addAll(initialReadTutorials) }
    /** Tutorial IDs that have been marked as read. */
    val readTutorials: List<String> get() = _readTutorials

    private val _currentVariables = mutableStateOf<Map<String, Any>>(emptyMap())
    /** Variables provided when the current tutorial was started; used for variable-based branching. */
    val currentVariables: Map<String, Any> get() = _currentVariables.value

    private val _pendingEvent = mutableStateOf<String?>(null)
    /** The last event name fired via [fireEvent], not yet consumed by the overlay. */
    val pendingEvent: State<String?> get() = _pendingEvent

    /** Starts a tutorial, optionally passing [variables] for variable-based scene branching. */
    fun startTutorial(id: String, variables: Map<String, Any> = emptyMap()) {
        _currentVariables.value = variables
        _activeTutorialId.value = id
    }

    /** Ends the active tutorial and clears all transient state. */
    fun endTutorial() {
        _activeTutorialId.value = null
        _currentVariables.value = emptyMap()
        _pendingEvent.value = null
    }

    /** Signals that a named event occurred; the overlay advances if the current card awaits this event. */
    fun fireEvent(name: String) {
        _pendingEvent.value = name
    }

    /** Called by the overlay when it processes a pending event; clears [pendingEvent]. */
    fun consumeEvent() {
        _pendingEvent.value = null
    }

    /** Marks [id] as read and persists the set to SharedPreferences if available. */
    fun markTutorialRead(id: String) {
        if (!_readTutorials.contains(id)) {
            _readTutorials.add(id)
            prefs?.edit()?.putStringSet(PREF_KEY, _readTutorials.toSet())?.apply()
        }
    }

    /** Returns true if tutorial [id] has been marked as read. */
    fun isTutorialRead(id: String): Boolean = _readTutorials.contains(id)

    companion object {
        /**
         * Returns a [Saver] that captures [context] so SharedPreferences can be re-opened
         * when restoring from a config change or process death.
         * Pass an Application Context to avoid retaining an Activity.
         */
        fun Saver(context: Context): Saver<AzTutorialController, List<Any?>> = Saver(
            save = { listOf(it.activeTutorialId.value, ArrayList(it._readTutorials)) },
            restore = { list ->
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                AzTutorialController(
                    initialActiveTutorialId = list[0] as String?,
                    initialReadTutorials = (list[1] as List<*>).filterIsInstance<String>(),
                    prefs = prefs,
                )
            }
        )
    }
}

/**
 * CompositionLocal providing the [AzTutorialController] to the composition tree.
 * Fails fast if not provided to surface missing providers during development.
 */
val LocalAzTutorialController = compositionLocalOf<AzTutorialController> {
    error("AzTutorialController not provided")
}

/** Remembers an [AzTutorialController] across recompositions and config changes, backed by SharedPreferences. */
@Composable
fun rememberAzTutorialController(): AzTutorialController {
    val context = LocalContext.current.applicationContext
    return rememberSaveable(saver = AzTutorialController.Saver(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet(PREF_KEY, emptySet())?.toList() ?: emptyList()
        AzTutorialController(initialReadTutorials = savedIds, prefs = prefs)
    }
}
