package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

@Composable
internal actual fun AzBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS routes "back" through its own navigation chrome / swipe-back — overlays here dismiss via
    // their explicit controls.
}
