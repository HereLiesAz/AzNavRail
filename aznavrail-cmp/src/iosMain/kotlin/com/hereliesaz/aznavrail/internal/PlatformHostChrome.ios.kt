package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/** iOS renders edge-to-edge under its own UIViewController; no Android-style window chrome. */
@Composable
internal actual fun PlatformHostChrome(wantDrawBehindNavBar: Boolean) {
    // no-op
}
