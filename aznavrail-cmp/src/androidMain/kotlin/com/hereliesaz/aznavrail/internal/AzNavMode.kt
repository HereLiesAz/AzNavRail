// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/AzNavMode.kt
package com.hereliesaz.aznavrail.internal

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * System-navigation-mode detection used to decide whether the library should reserve a bottom
 * margin and whether the "draw behind the navigation bar" sheet option applies.
 *
 * Backed by the `Settings.Secure` key `navigation_mode`:
 *  - `0` = 3-button navigation
 *  - `1` = 2-button navigation
 *  - `2` = gesture navigation
 *
 * Reading a `Settings.Secure` value needs no permission. The key only exists on API 29+; on
 * older releases (which have no gesture navigation) and whenever the key is missing, we treat
 * the device as button-navigation — the conservative default that preserves the library's
 * existing margins.
 */
internal object AzNavMode {

    private const val NAVIGATION_MODE = "navigation_mode"
    private const val MODE_GESTURAL = 2

    /** `true` when the device is in gesture-navigation mode. */
    fun isGestureNav(context: Context): Boolean =
        runCatching { Settings.Secure.getInt(context.contentResolver, NAVIGATION_MODE) }
            .getOrDefault(0) == MODE_GESTURAL

    /** `true` when the device uses 3-button / 2-button navigation (or the mode is unknown). */
    fun isButtonNav(context: Context): Boolean = !isGestureNav(context)
}

