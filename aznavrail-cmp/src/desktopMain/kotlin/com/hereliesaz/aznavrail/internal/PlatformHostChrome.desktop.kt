package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/** Desktop renders edge-to-edge in its own window; no Android-style window chrome to manage. */
@Composable
internal actual fun PlatformHostChrome(wantDrawBehindNavBar: Boolean) {
    // no-op
}
