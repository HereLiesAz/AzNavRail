package com.hereliesaz.aznavrail.internal

/**
 * Common abstraction for requesting overlay permissions and launching an overlay.
 */
internal actual object OverlayHelper {
    actual fun launch(context: Any?, serviceClass: Any?) {
        // Desktop does not require permission requests or Android service launching.
    }
}
