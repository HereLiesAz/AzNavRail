package com.hereliesaz.aznavrail.internal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

/**
 * Android: enable edge-to-edge on the enclosing Activity, and (for sheets that opt into
 * `drawBehindNavBar` under button navigation) force the system nav bar transparent, restoring the
 * previous window values on dispose. Ported from the Android `AzHostActivityLayout`.
 */
@Composable
internal actual fun PlatformHostChrome(wantDrawBehindNavBar: Boolean) {
    val context = LocalContext.current
    val activity = remember(context) {
        var c: Context = context
        var found: Activity? = null
        while (c is ContextWrapper) {
            if (c is Activity) { found = c; break }
            c = c.baseContext
        }
        found
    }
    LaunchedEffect(activity) {
        activity?.let { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
    }
    val gestureNav = rememberIsGestureNav()
    DisposableEffect(activity, wantDrawBehindNavBar, gestureNav) {
        val window = activity?.window
        if (window != null && wantDrawBehindNavBar && !gestureNav) {
            @Suppress("DEPRECATION")
            val previousColor = window.navigationBarColor
            val previousContrast =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                    window.isNavigationBarContrastEnforced
                else null
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            onDispose {
                @Suppress("DEPRECATION")
                window.navigationBarColor = previousColor
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && previousContrast != null) {
                    window.isNavigationBarContrastEnforced = previousContrast
                }
            }
        } else {
            onDispose { }
        }
    }
}
