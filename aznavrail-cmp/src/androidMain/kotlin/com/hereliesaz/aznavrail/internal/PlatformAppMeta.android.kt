package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.aznavrail.AzAppMeta

/**
 * Android: resolve the launcher icon + label + package name from `PackageManager`, exactly like the
 * Android `aznavrail` library (`AzNavRail.kt`). The icon is returned as an `android.graphics.drawable
 * .Drawable`, which Coil3 renders directly on Android.
 */
@Composable
internal actual fun rememberPlatformAppMeta(): AzAppMeta {
    val context = LocalContext.current
    return remember(context.packageName) {
        val pm = context.packageManager
        val pkg = context.packageName
        val name = try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            AzAppMeta().name
        }
        val icon = try {
            pm.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }
        AzAppMeta(name = name, icon = icon, packageId = pkg)
    }
}
