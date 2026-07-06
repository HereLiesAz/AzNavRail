package com.hereliesaz.aznavrail

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.hereliesaz.aznavrail.internal.AzSafeZones

/**
 * CompositionLocal providing the computed safe-zone insets for the current layout.
 *
 * The Android sibling defines this in `AzNavHost.kt`, which drags in AndroidX Navigation and
 * ContextWrappers. The CMP module keeps just this CompositionLocal — safe-zone insets are pure
 * layout data and don't need any of the navigation machinery.
 */
val LocalAzSafeZones = compositionLocalOf { AzSafeZones() }

/**
 * App metadata surfaced to the rail (previously the Android sibling scraped these from
 * `context.packageName` / `context.packageManager` at composition time — an approach that has no
 * CMP analogue).
 *
 * On Android, `AzHostActivityLayout` provides this from `LocalContext`. On other targets consumers
 * pass it explicitly through a `CompositionLocalProvider(LocalAzAppMeta provides AzAppMeta(...))`
 * before mounting `AzNavRail`. When left at the default the rail falls back to placeholder values
 * ("App", no icon, no auto-repo derivation).
 *
 * @param name The app display name shown in About / footer.
 * @param icon The app icon (any Coil3-compatible model — usually a URL string, ByteArray, drawable
 *   resource id, or `androidx.compose.ui.graphics.painter.Painter`). Null suppresses the icon tile.
 * @param packageId The app's package identifier (Android package name, iOS bundle id, etc.). Only
 *   used to derive a default repository URL via
 *   `GithubDocsRepository.repoUrlFromPackage(packageId)` when the DSL leaves
 *   `scope.appRepositoryUrl` blank.
 */
data class AzAppMeta(
    val name: String = "App",
    val icon: Any? = null,
    val packageId: String? = null,
)

val LocalAzAppMeta = staticCompositionLocalOf { AzAppMeta() }

/**
 * The two localized guidance strings the Android sibling reads from `R.string.az_guide_open_menu`
 * and `R.string.az_guide_tap_item`. The CMP module ships English defaults; consumers who need
 * localization override this local with translated values.
 */
data class AzGuideStrings(
    val openMenu: String = "Open the menu",
    val tapItem: String = "Tap %s",
)

val LocalAzGuideStrings = compositionLocalOf { AzGuideStrings() }
