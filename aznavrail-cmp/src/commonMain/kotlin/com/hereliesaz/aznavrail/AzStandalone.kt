package com.hereliesaz.aznavrail

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.hereliesaz.aznavrail.internal.AboutOverlay
import com.hereliesaz.aznavrail.model.AzEasing
import com.hereliesaz.aznavrail.model.AzMotion
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.MoreFromAzOverlay
import com.hereliesaz.aznavrail.internal.azWindowSafeInsets
import com.hereliesaz.aznavrail.internal.rememberEffectiveAppMeta
import com.hereliesaz.aznavrail.model.AzAdvancedConfig
import com.hereliesaz.aznavrail.service.GithubDocsRepository

/*
 * Standalone entry points for the pieces of AzNavRail an app may want without the rail itself: the
 * About reader (with More from Az over it), the rail's footer and the dropdown menu's footer. Each is
 * the same composable the rail and menu draw, so the look and behaviour stay one implementation.
 */

/**
 * The full-screen About reader, drawn in its own window-covering popup so it can be called from
 * anywhere. "More from Az" opens over it and closing that returns to About.
 *
 * @param onDismiss Called when the reader is closed (close affordance or back).
 * @param repoUrl The app repository whose docs the reader lists. Blank derives it from the package
 *   name, as the rail does.
 * @param accent Headings, links and close affordance; unspecified falls back to the theme primary.
 * @param background The reader's ground; unspecified keeps the library's near-black.
 * @param moreFromAz Whether the reader offers "More from Az".
 * @param moreFromAzJsonUrl Manifest backing the "More from Az" carousel.
 */
@Composable
fun AzAboutPage(
    onDismiss: () -> Unit,
    repoUrl: String = "",
    accent: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    moreFromAz: Boolean = true,
    moreFromAzJsonUrl: String = AzAdvancedConfig().moreFromAzJsonUrl,
) {
    val effectiveRepoUrl = rememberEffectiveRepoUrl(repoUrl)
    val scope = rememberStandaloneScope(accent, background, moreFromAz, moreFromAzJsonUrl)
    var showMoreFromAz by remember { mutableStateOf(false) }
    val insets = azWindowSafeInsets()
    Popup(
        popupPositionProvider = WindowOriginPosition,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = false),
    ) {
        CompositionLocalProvider(
            LocalAzSafeZones provides AzSafeZones(
                top = insets.top, bottom = insets.bottom, start = insets.start, end = insets.end,
            ),
        ) {
            Box(Modifier.fillMaxSize()) {
                AboutOverlay(
                    repoUrl = effectiveRepoUrl,
                    scope = scope,
                    onOpenMoreFromAz = if (moreFromAz) ({ showMoreFromAz = true }) else null,
                    onDismiss = onDismiss,
                )
                if (showMoreFromAz) {
                    MoreFromAzOverlay(
                        jsonUrl = moreFromAzJsonUrl,
                        scope = scope,
                        onDismiss = { showMoreFromAz = false },
                    )
                }
            }
        }
    }
}

/**
 * The rail's footer strip: About, Feedback and @HereLiesAz, plus Undock when [onUndock] is given.
 *
 * @param onAboutClick What About does; null opens [repoUrl] in a browser.
 * @param appName Embedded in the feedback email subject; blank uses the app's label.
 * @param color Tint of every footer item.
 * @param onSecretClick Long-press on @HereLiesAz; null disables it.
 * @param visible Drives the fold-in/out; the footer unfolds one stagger tick after [menuItemCount]
 *   items have begun their entrance.
 */
@Composable
fun AzRailFooter(
    onAboutClick: (() -> Unit)?,
    repoUrl: String = "",
    appName: String = "",
    color: Color = Color.Unspecified,
    onUndock: (() -> Unit)? = null,
    onSecretClick: (() -> Unit)? = null,
    visible: Boolean = true,
    menuItemCount: Int = 0,
    staggerMs: Int = AzMotion.ItemStaggerMs,
    durationMs: Int = AzMotion.ItemDurationMs,
    easing: Easing = AzEasing.Wp7Decelerate,
) {
    val appMeta = rememberEffectiveAppMeta()
    val name = appName.ifBlank { appMeta.name }
    val effectiveRepoUrl = rememberEffectiveRepoUrl(repoUrl)
    val scope = remember(onUndock) {
        AzNavRailScopeImpl().apply { advancedConfig = advancedConfig.copy(onUndock = onUndock) }
    }
    Footer(
        appName = name,
        onToggle = {},
        onUndock = { onUndock?.invoke() },
        onSecretClick = onSecretClick,
        scope = scope,
        repoUrl = effectiveRepoUrl,
        footerColor = color,
        onAboutClick = onAboutClick,
        visible = visible,
        menuItemCount = menuItemCount,
        staggerMs = staggerMs,
        durationMs = durationMs,
        easing = easing,
    )
}

/**
 * The dropdown menu's footer strip: About, Feedback and @HereLiesAz.
 *
 * @param onAboutClick What About does; null opens [repoUrl] in a browser.
 * @param color Tint of every footer item.
 * @param visible Drives the fold-in/out, timed after [menuItemCount] entries.
 */
@Composable
fun AzMenuFooter(
    onAboutClick: (() -> Unit)?,
    repoUrl: String = "",
    color: Color = Color.Unspecified,
    visible: Boolean = true,
    menuItemCount: Int = 0,
    staggerMs: Int = AzMotion.ItemStaggerMs,
    durationMs: Int = AzMotion.ItemDurationMs,
    easing: Easing = AzEasing.Wp7Decelerate,
) {
    val effectiveRepoUrl = rememberEffectiveRepoUrl(repoUrl)
    AzDropdownFooter(
        repoUrl = effectiveRepoUrl,
        onAboutClick = onAboutClick,
        footerColor = color,
        visible = visible,
        menuItemCount = menuItemCount,
        staggerMs = staggerMs,
        durationMs = durationMs,
        easing = easing,
    )
}

@Composable
private fun rememberEffectiveRepoUrl(repoUrl: String): String {
    val packageId = rememberEffectiveAppMeta().packageId
    return remember(repoUrl, packageId) {
        repoUrl.ifBlank { packageId?.let { GithubDocsRepository.repoUrlFromPackage(it) } ?: repoUrl }
    }
}

@Composable
private fun rememberStandaloneScope(
    accent: Color,
    background: Color,
    moreFromAz: Boolean,
    moreFromAzJsonUrl: String,
): AzNavRailScopeImpl = remember(accent, background, moreFromAz, moreFromAzJsonUrl) {
    AzNavRailScopeImpl().apply {
        azTheme(activeColor = accent, translucentBackground = background)
        advancedConfig = advancedConfig.copy(moreFromAzEnabled = moreFromAz, moreFromAzJsonUrl = moreFromAzJsonUrl)
    }
}

/** Puts a popup at the window origin so its `fillMaxSize` content covers the screen. */
private val WindowOriginPosition = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}
