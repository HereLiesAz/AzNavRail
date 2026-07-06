package com.hereliesaz.aznavrail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that signals whether [AzNavRail] is correctly nested inside `AzHostActivityLayout`.
 *
 * The Android sibling errors out visibly when this is `false` because the Android host performs
 * critical safe-zone / rotation / docking setup. The CMP module doesn't ship an Activity host —
 * `AzNavRail` can be dropped into any Compose root and the check just short-circuits to `true` if
 * the caller wires this local themselves. Default is `false`, matching the Android sibling.
 */
val LocalAzNavHostPresent = compositionLocalOf { false }

/**
 * CompositionLocal giving composables access to the active [AzNavHostScopeImpl] for overlay
 * visibility control (About / Help / More-from-Az). Null when there's no host scope in play — the
 * rail then falls back to owning those overlays locally.
 */
val LocalAzNavHostScope = staticCompositionLocalOf<AzNavHostScopeImpl?> { null }

/**
 * A minimal, CMP-friendly host scope carrying only the overlay-visibility state the rail's
 * composables read (About / Help / More-from-Az).
 *
 * Port note vs the Android sibling: the Android `AzNavHostScope` interface also carries navigation
 * (`navController`), background/onscreen/bottom-sheet DSL registration, and a lot of layout state
 * — all bound to `AzHostActivityLayout`, which doesn't port. The CMP module exposes only the pieces
 * that the rail's composables read directly; consumers who want the full DSL host it themselves.
 */
class AzNavHostScopeImpl {
    var helpVisible: Boolean by mutableStateOf(false)
        private set
    var aboutVisible: Boolean by mutableStateOf(false)
        private set
    var moreFromAzVisible: Boolean by mutableStateOf(false)
        private set

    /** Track the id of the rail item currently owning the help overlay. */
    var helpOwnerId: String? by mutableStateOf(null)
        private set

    fun showHelp(id: String) {
        helpOwnerId = id
        helpVisible = true
    }

    fun hideHelp() {
        helpVisible = false
        helpOwnerId = null
    }

    fun showAbout() {
        aboutVisible = true
    }

    fun hideAbout() {
        aboutVisible = false
    }

    fun showMoreFromAz() {
        moreFromAzVisible = true
    }

    fun hideMoreFromAz() {
        moreFromAzVisible = false
    }
}

/**
 * Convenience helper that mirrors the Android sibling's `rememberAzNavHostScope()` — CMP consumers
 * mount this at the root of their composition, provide it via `LocalAzNavHostScope`, and get the
 * three overlay toggles for free.
 */
@Composable
fun rememberAzNavHostScope(): AzNavHostScopeImpl {
    // Kept simple — no `rememberSaveable` because the Saver would require a platform-specific
    // Bundle/NSCoder path. Consumers who need state persistence can subclass and add it.
    return androidx.compose.runtime.remember { AzNavHostScopeImpl() }
}
