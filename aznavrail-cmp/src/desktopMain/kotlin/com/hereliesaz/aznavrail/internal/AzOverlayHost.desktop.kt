package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposePanel
import java.awt.Color
import javax.swing.JWindow

internal actual fun createAzOverlayHost(context: Any?): AzOverlayHost = DesktopOverlayHost()

private class DesktopOverlayHost : AzOverlayHost {
    private var window: JWindow? = null

    override fun attach(content: @Composable () -> Unit) {
        if (window != null) return
        window = JWindow().apply {
            isAlwaysOnTop = true
            background = Color(0, 0, 0, 0)
            val panel = ComposePanel().apply {
                background = Color(0, 0, 0, 0)
                setContent {
                    content()
                }
            }
            add(panel)
            pack()
            isVisible = true
        }
    }

    override fun detach() {
        window?.dispose()
        window = null
    }

    override fun updatePosition(x: Float, y: Float) {
        window?.let {
            val loc = it.location
            it.setLocation(loc.x + x.toInt(), loc.y + y.toInt())
        }
    }
}
