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
// Process-level memo: the app's own package/label/icon are fixed for the process lifetime, and
// getApplicationLabel/getApplicationIcon are synchronous PackageManager IPC calls — resolve them at
// most once rather than on every new rail/dropdown composition.
private var cachedPlatformAppMeta: AzAppMeta? = null

@Composable
internal actual fun rememberPlatformAppMeta(): AzAppMeta {
    val context = LocalContext.current
    return remember(context.packageName) {
        cachedPlatformAppMeta ?: run {
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
            AzAppMeta(name = name, icon = icon, packageId = pkg).also { cachedPlatformAppMeta = it }
        }
    }
}
