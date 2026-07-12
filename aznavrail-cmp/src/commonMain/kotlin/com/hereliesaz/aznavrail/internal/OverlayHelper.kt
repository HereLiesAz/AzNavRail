package com.hereliesaz.aznavrail.internal

/**
 * Common abstraction for requesting overlay permissions and launching an overlay.
 */
internal expect object OverlayHelper {
    /**
     * Launch the specified overlay service/host.
     * @param context Provide native context if required (e.g. android.content.Context).
     * @param serviceClass Provide the native service class if required.
     */
    fun launch(context: Any? = null, serviceClass: Any? = null)
}
