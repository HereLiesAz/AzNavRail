package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

@Composable
internal actual fun AzBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack = rememberUpdatedState(onBack)

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val processor = java.awt.KeyEventPostProcessor { e ->
            if (e.id == KeyEvent.KEY_PRESSED && e.keyCode == KeyEvent.VK_ESCAPE) {
                currentOnBack.value()
                true
            } else {
                false
            }
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(processor)

        onDispose {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(processor)
        }
    }
}
