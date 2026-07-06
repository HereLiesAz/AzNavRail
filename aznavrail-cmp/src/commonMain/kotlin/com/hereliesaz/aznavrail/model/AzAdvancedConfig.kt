package com.hereliesaz.aznavrail.model

import androidx.compose.ui.geometry.Rect

/**
 * Aggregated advanced settings for the rail, populated by [com.hereliesaz.aznavrail.AzNavRailScope.azAdvanced]
 * and [com.hereliesaz.aznavrail.AzNavRailScope.azSettings].
 *
 * @param isLoading When true, the rail content is replaced by a full-screen [com.hereliesaz.aznavrail.AzLoad] spinner.
 * @param helpEnabled Whether the interactive help/info overlay is enabled.
 * @param onDismissHelp Callback invoked when the help overlay is dismissed.
 * @param overlayService Service class used to launch a system overlay (FAB mode) on Android.
 *   Automatically sets [enableRailDragging] to true when non-null. Typed `Any?` in the CMP port —
 *   foreground-service overlays are Android-only, so on non-Android targets this is an inert handle.
 * @param onUndock Callback invoked when the rail is undocked to FAB mode.
 * @param enableRailDragging Whether the user can drag the rail to detach it (FAB mode).
 * @param onRailDrag Callback reporting `(dx, dy)` during in-app drag events.
 * @param onOverlayDrag Callback reporting `(dx, dy)` during system-overlay drag events.
 * @param onItemGloballyPositioned Reports the window-space [Rect] of an item by its ID after layout;
 *   primarily used by the tutorial and help systems.
 * @param secLoc Developer configuration key that unlocks the Secret Screens debug menu.
 *   Long-pressing the `@HereLiesAz` footer item prompts for this key.
 * @param secLocPort TCP port used by the location history sync server. Defaults to 10203.
 * @param helpList Map of item ID → help text (String or string resource Int) shown in the help overlay.
 * @param onInteraction Callback invoked whenever any rail item is interacted with (click, toggle,
 *   cycler advance, nested rail open, reloc drag). Receives the item's `id` and the [AzNavItem] itself.
 * @param inAppAbout When true (default), the footer "About" item opens the in-app About reader overlay
 *   (auto-generated from the repo's markdown docs) instead of opening [com.hereliesaz.aznavrail.AzNavRailScopeImpl.appRepositoryUrl]
 *   in a browser.
 * @param moreFromAzEnabled When true (default), the About overlay offers a "More from Az" entry that
 *   opens a carousel of the library author's other apps, fetched from [moreFromAzJsonUrl].
 * @param moreFromAzJsonUrl Raw URL of the JSON manifest backing the "More from Az" carousel. Its
 *   `version` integer is CI-managed and used to invalidate the local cache.
 * @param moreFromAzRailItem When true, a "More" item is pinned at the bottom of the collapsed rail
 *   that opens the "More from Az" carousel directly (independent of the About screen).
 */
data class AzAdvancedConfig(
    val isLoading: Boolean = false,
    val helpEnabled: Boolean = false,
    val onDismissHelp: (() -> Unit)? = null,
    val overlayService: Any? = null,
    val onUndock: (() -> Unit)? = null,
    val enableRailDragging: Boolean = false,
    val onRailDrag: ((Float, Float) -> Unit)? = null,
    val onOverlayDrag: ((Float, Float) -> Unit)? = null,
    val onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    val secLoc: String? = null,
    val secLocPort: Int = 10203,
    val helpList: Map<String, Any> = emptyMap(),
    val onInteraction: ((String, AzNavItem) -> Unit)? = null,
    val inAppAbout: Boolean = true,
    val moreFromAzEnabled: Boolean = true,
    val moreFromAzJsonUrl: String = "https://raw.githubusercontent.com/HereLiesAz/AzNavRail/main/more-from-az.json",
    val moreFromAzRailItem: Boolean = false
)
