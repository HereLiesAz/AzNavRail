package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

internal actual fun createAzOverlayHost(context: Any?): AzOverlayHost = WasmJsOverlayHost()

private class WasmJsOverlayHost : AzOverlayHost {
    override fun attach(content: @Composable () -> Unit) {
        platformLogE("AzOverlayHost", "Overlays are unsupported on Web/WasmJs", null)
    }

    override fun detach() {
    }

    override fun updatePosition(x: Float, y: Float) {
    }
}
