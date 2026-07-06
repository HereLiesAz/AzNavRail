package com.hereliesaz.aznavrail.internal

import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberDeviceRotationDegrees(): Float {
    val rotation = LocalView.current.display?.rotation ?: Surface.ROTATION_0
    return when (rotation) {
        Surface.ROTATION_90 -> 90f
        Surface.ROTATION_180 -> 180f
        Surface.ROTATION_270 -> 270f
        else -> 0f
    }
}
