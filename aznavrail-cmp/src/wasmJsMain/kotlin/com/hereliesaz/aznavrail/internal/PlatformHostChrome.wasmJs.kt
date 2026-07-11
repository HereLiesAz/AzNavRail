package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/** The browser canvas has no Android-style window chrome to manage. */
@Composable
internal actual fun PlatformHostChrome(wantDrawBehindNavBar: Boolean) {
    // no-op
}
