package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

@Composable
internal actual fun AzBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No system back gesture on desktop — overlays are dismissed via their explicit controls.
}
