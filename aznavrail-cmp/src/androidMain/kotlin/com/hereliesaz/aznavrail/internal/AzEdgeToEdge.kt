package com.hereliesaz.aznavrail.internal

import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowManager

/**
 * Edge-to-edge window plumbing, kept in one place because Android 15 (API 35) changed the rules.
 *
 * An app targeting SDK 35 is laid out edge-to-edge whether it opts in or not, and the knobs that
 * used to shape that layout were deprecated in the same release:
 *
 * * `LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` and
 *   `LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER` are deprecated;
 *   [WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS] is the surviving value. A
 *   window that never assigns the field inherits the deprecated `DEFAULT`, so every window this
 *   library adds to the [WindowManager] assigns `ALWAYS` explicitly — see [azAllowDisplayCutout].
 * * `Window.setStatusBarColor`, `setNavigationBarColor`, `isStatusBarContrastEnforced` and
 *   `isNavigationBarContrastEnforced` are deprecated and ignored for apps targeting SDK 35, because
 *   under enforced edge-to-edge the bars are already fully transparent. [azForceTransparentNavBar]
 *   therefore only touches them below API 35, where they still have an effect.
 */
internal fun WindowManager.LayoutParams.azAllowDisplayCutout(): WindowManager.LayoutParams = apply {
    // The constant (and the field) landed in P; below that there are no cutouts to lay out into.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}

/**
 * Makes [window]'s system navigation bar see-through so content drawn behind it shows through, and
 * returns the undo.
 *
 * On API 35+ this is a no-op that returns a no-op: the window properties involved are deprecated
 * there and ignored for apps targeting SDK 35 — the bar is already transparent under enforced
 * edge-to-edge, so there is nothing to force and nothing to restore.
 */
internal fun azForceTransparentNavBar(window: Window): () -> Unit {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) return {}
    @Suppress("DEPRECATION")
    val previousColor = window.navigationBarColor
    val previousContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced
    } else {
        null
    }
    @Suppress("DEPRECATION")
    window.navigationBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
    return {
        @Suppress("DEPRECATION")
        window.navigationBarColor = previousColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && previousContrast != null) {
            window.isNavigationBarContrastEnforced = previousContrast
        }
    }
}
