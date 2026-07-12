package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.browser.window
import org.w3c.dom.events.Event

// Fallback to window.orientation for older iOS Safari, otherwise screen.orientation.angle
private fun getRawScreenAngle(): Float = js("window.screen && window.screen.orientation ? window.screen.orientation.angle : (window.orientation || 0)")

private fun getScreenAngle(): Float {
    return try {
        getRawScreenAngle()
    } catch (e: Throwable) {
        0f
    }
}

@Composable
internal actual fun rememberDeviceRotationDegrees(): Float {
    val angle = remember { mutableStateOf(getScreenAngle()) }

    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = {
            angle.value = getScreenAngle()
        }

        // Standard orientation change event for most mobile browsers
        window.addEventListener("orientationchange", listener)

        onDispose {
            window.removeEventListener("orientationchange", listener)
        }
    }

    return angle.value
}
