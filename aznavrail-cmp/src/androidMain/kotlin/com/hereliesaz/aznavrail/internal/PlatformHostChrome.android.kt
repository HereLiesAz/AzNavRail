package com.hereliesaz.aznavrail.internal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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
 *
 * Forcing the bar transparent is a no-op on Android 15+, where the properties it leans on are
 * deprecated and the bar is already transparent under enforced edge-to-edge — see
 * [azForceTransparentNavBar].
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
        // `enableEdgeToEdge()` is the forward-looking call (it also sets up the system bar icon
        // appearance, and matches what Android 15 enforces for apps targeting SDK 35);
        // `setDecorFitsSystemWindows` is the fallback for a host that isn't a ComponentActivity.
        val host = activity
        if (host is ComponentActivity) {
            host.enableEdgeToEdge()
        } else if (host != null) {
            WindowCompat.setDecorFitsSystemWindows(host.window, false)
        }
    }
    val gestureNav = rememberIsGestureNav()
    DisposableEffect(activity, wantDrawBehindNavBar, gestureNav) {
        val window = activity?.window
        if (window != null && wantDrawBehindNavBar && !gestureNav) {
            val restore = azForceTransparentNavBar(window)
            onDispose { restore() }
        } else {
            onDispose { }
        }
    }
}
