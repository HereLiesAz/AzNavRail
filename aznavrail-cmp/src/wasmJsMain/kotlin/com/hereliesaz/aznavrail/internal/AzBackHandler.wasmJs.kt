package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

@Composable
internal actual fun AzBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Browser back is handled by history; overlays here dismiss via their explicit controls.
}
