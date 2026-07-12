package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/**
 * Common abstraction for an overlay host (e.g. SYSTEM_ALERT_WINDOW on Android, Desktop Window).
 */
internal interface AzOverlayHost {
    /**
     * Attaches the overlay to the screen.
     */
    fun attach(content: @Composable () -> Unit)

    /**
     * Detaches the overlay from the screen.
     */
    fun detach()

    /**
     * Updates the position of the overlay.
     */
    fun updatePosition(x: Float, y: Float)
}

/**
 * Creates an instance of [AzOverlayHost].
 * @param context Provide the native context if required (e.g., android.content.Context on Android).
 */
internal expect fun createAzOverlayHost(context: Any? = null): AzOverlayHost
