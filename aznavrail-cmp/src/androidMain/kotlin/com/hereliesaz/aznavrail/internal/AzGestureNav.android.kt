package com.hereliesaz.aznavrail.internal

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Android: `Settings.Secure "navigation_mode"` == 2 (MODE_GESTURAL). Missing key ⇒ button nav. */
@Composable
internal actual fun rememberIsGestureNav(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Secure.getInt(context.contentResolver, "navigation_mode", 0) == 2
        } catch (e: Exception) {
            false
        }
    }
}
