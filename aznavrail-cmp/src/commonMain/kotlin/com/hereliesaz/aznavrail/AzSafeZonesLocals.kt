package com.hereliesaz.aznavrail

import androidx.compose.runtime.compositionLocalOf
import com.hereliesaz.aznavrail.internal.AzSafeZones

/**
 * CompositionLocal providing the computed safe-zone insets for the current layout.
 *
 * The Android sibling defines this in `AzNavHost.kt`, which drags in AndroidX Navigation and
 * ContextWrappers. The CMP module keeps just this CompositionLocal — safe-zone insets are pure
 * layout data and don't need any of the navigation machinery.
 */
val LocalAzSafeZones = compositionLocalOf { AzSafeZones() }
