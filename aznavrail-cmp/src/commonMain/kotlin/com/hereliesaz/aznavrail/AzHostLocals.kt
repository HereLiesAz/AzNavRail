package com.hereliesaz.aznavrail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that signals whether [AzNavRail] is correctly nested inside [AzHostActivityLayout].
 *
 * [AzHostActivityLayout] sets this to `true` (it performs the safe-zone / rotation / docking setup the
 * rail relies on). A rail mounted without the host — or with this left at its `false` default — renders
 * a red "Configuration Error" placeholder, matching the Android sibling.
 */
val LocalAzNavHostPresent = compositionLocalOf { false }

/**
 * CompositionLocal giving composables access to the active [AzNavHostScopeImpl] — navigation, the
 * background/onscreen/bottom-sheet registrations, and overlay-visibility control (About / Help /
 * More-from-Az). Populated by [AzHostActivityLayout]; `null` when the rail/dropdown are used
 * standalone (they then own their overlays locally and skip route-based navigation).
 */
val LocalAzNavHostScope = staticCompositionLocalOf<AzNavHostScopeImpl?> { null }

/**
 * Convenience helper mirroring the Android sibling's `rememberAzNavHostScope()`. Consumers who mount
 * [AzNavRail] without [AzHostActivityLayout] can create a scope, provide it via [LocalAzNavHostScope],
 * and get the overlay toggles (About / Help / More-from-Az) working. [AzHostActivityLayout] creates
 * and wires its own scope, so callers using the host do not need this.
 */
@Composable
fun rememberAzNavHostScope(): AzNavHostScopeImpl = remember { AzNavHostScopeImpl() }
