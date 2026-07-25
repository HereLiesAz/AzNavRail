package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Android: start the service (requesting `SYSTEM_ALERT_WINDOW` first when it isn't granted). */
@Composable
internal actual fun rememberAzOverlayLauncher(): (Any?) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { serviceClass: Any? ->
            if (serviceClass != null) {
                runCatching { OverlayHelper.launch(context, serviceClass) }
                    .onFailure { AzNavRailLogger.e("AzNavRail", "Failed to start overlay service", it) }
            }
        }
    }
}
