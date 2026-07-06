package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/**
 * Platform back-gesture handler.
 *
 * The Android sibling uses `androidx.activity.compose.BackHandler`, which is Android-only — it isn't
 * available in commonMain (the JetBrains activity-compose fork doesn't expose it commonly, and the
 * multiplatform back APIs live in `androidx.compose.ui.backhandler` only on newer CMP). To stay
 * compile-safe on every target without depending on that, back handling is funnelled through this
 * `expect/actual`:
 *
 *  - Android → delegates to `androidx.activity.compose.BackHandler`.
 *  - Desktop / iOS / wasmJs → no-op. Those platforms route "back" through their own chrome (window
 *    close, iOS swipe-back, browser history); the overlays and bottom sheet that call this all have
 *    explicit dismiss affordances, so a no-op is an acceptable first-port gap.
 */
@Composable
internal expect fun AzBackHandler(enabled: Boolean, onBack: () -> Unit)
