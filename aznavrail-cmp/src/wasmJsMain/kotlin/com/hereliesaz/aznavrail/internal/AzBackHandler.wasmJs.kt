package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
internal actual fun AzBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack = rememberUpdatedState(onBack)

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val listener: (Event) -> Unit = {
            currentOnBack.value()
        }

        window.history.pushState(null, "", window.location.href)
        window.addEventListener("popstate", listener)

        onDispose {
            window.removeEventListener("popstate", listener)
        }
    }
}
