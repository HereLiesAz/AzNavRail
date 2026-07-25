package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/**
 * Returns the callback that starts the developer-supplied system-overlay service when the rail
 * undocks, or a no-op on targets that have no such concept.
 *
 * `azSettings(overlayService = …)` has always been collected into the config, and [OverlayHelper]
 * has always known how to start it — but nothing ever connected the two, so the documented
 * "rail as a system-wide overlay" mode never actually started anything. This is that connection,
 * expressed per-platform because only Android can hand [OverlayHelper] the `Context` it needs.
 */
@Composable
internal expect fun rememberAzOverlayLauncher(): (Any?) -> Unit
