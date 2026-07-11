package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hereliesaz.aznavrail.AzAppMeta
import platform.Foundation.NSBundle

/**
 * iOS: read the app's display name and bundle id from `NSBundle.mainBundle`. The app icon lives in the
 * asset catalog and isn't loadable as a file at runtime, so the icon is left null (consumers can pass a
 * Coil3 model via `LocalAzAppMeta` if they want one).
 */
@Composable
internal actual fun rememberPlatformAppMeta(): AzAppMeta = remember {
    val bundle = NSBundle.mainBundle
    val name = (bundle.objectForInfoDictionaryKey("CFBundleDisplayName") as? String)
        ?: (bundle.objectForInfoDictionaryKey("CFBundleName") as? String)
        ?: AzAppMeta().name
    AzAppMeta(name = name, icon = null, packageId = bundle.bundleIdentifier)
}
