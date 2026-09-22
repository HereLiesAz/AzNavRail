package com.hereliesaz.aznavrail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that signals whether [AzNavRail] is correctly configured for this platform.
 *
 * Unlike the Android artifact, this CMP module has no `AzHostActivityLayout` (it's built on
 * `Activity`/`Window`, which don't exist off-Android) — you provide this local yourself, typically
 * `true` unconditionally at your composition root. A rail mounted with this left at its `false`
 * default renders a red "Configuration Error" placeholder, matching the Android sibling.
 */
val LocalAzNavHostPresent = compositionLocalOf { false }

/**
 * CompositionLocal giving composables access to the active [AzNavHostScopeImpl] — navigation, the
 * background/onscreen/bottom-sheet registrations, and overlay-visibility control (About / Help /
 * More-from-Az). `null` when the rail/dropdown are used standalone (they then own their overlays
 * locally and skip route-based navigation); provide one via [rememberAzNavHostScope] to opt in.
 */
val LocalAzNavHostScope = staticCompositionLocalOf<AzNavHostScopeImpl?> { null }

/**
 * Convenience helper mirroring the Android sibling's `rememberAzNavHostScope()`. Create a scope,
 * provide it via [LocalAzNavHostScope], and get the overlay toggles (About / Help / More-from-Az)
 * working — there is no host composable in this module that wires one up for you.
 */
@Composable
fun rememberAzNavHostScope(): AzNavHostScopeImpl = remember { AzNavHostScopeImpl() }
