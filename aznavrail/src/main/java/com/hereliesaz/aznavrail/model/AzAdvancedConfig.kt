package com.hereliesaz.aznavrail.model

import androidx.compose.ui.geometry.Rect

/**
 * Aggregated advanced settings for the rail, populated by [com.hereliesaz.aznavrail.AzNavRailScope.azAdvanced]
 * and [com.hereliesaz.aznavrail.AzNavRailScope.azSettings].
 *
 * @param isLoading When true, a screen-centred [com.hereliesaz.aznavrail.AzLoad] draws above
 *   everything and swallows input. **Prefer `azItemState(id, isLoading = true)`**: a whole screen
 *   blacked out to say "busy" is a second element doing the work the busy element could do itself,
 *   and it takes the app away from the user while it does so. Reach for this only when genuinely
 *   nothing on screen is actionable.
 * @param helpEnabled Whether the interactive help/info overlay is enabled.
 * @param autoGuidanceEdges Whether the rail auto-generates guidance instructions ("Open the menu",
 *   "Tap Settings") for its own affordances. **Off by default.** Guidance is a last resort: a rail
 *   that has to caption its own buttons has already failed to convey them. Author `azEdge`s for
 *   transitions into your app's own domain statuses instead, and turn this on only when telemetry
 *   says people are actually getting stuck.
 * @param onDismissHelp Callback invoked when the help overlay is dismissed.
 * @param overlayService Service class used to launch a system overlay (FAB mode). Automatically
 *   sets [enableRailDragging] to true when non-null.
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
 * @param aboutRailItem When true (default), the rail ends with a built-in About (`?`) rail item,
 *   unless the developer declared their own with `azAboutRailItem`.
 * @param dedupeAbout When true (default), the library keeps track of every surface that offers an
 *   About affordance — the `?` rail item, the expanded menu's footer, a drop-down menu's footer —
 *   and draws it in exactly one of them, so About never appears twice. The most deliberate placement
 *   wins: a developer-declared `azAboutRailItem`, then the rail's menu footer, then a drop-down's
 *   footer, then the automatic `?`. Set false to draw About wherever it is configured.
 */
data class AzAdvancedConfig(
    val isLoading: Boolean = false,
    val helpEnabled: Boolean = false,
    val autoGuidanceEdges: Boolean = false,
    val onDismissHelp: (() -> Unit)? = null,
    val overlayService: Class<out android.app.Service>? = null,
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
    val moreFromAzRailItem: Boolean = false,
    val aboutRailItem: Boolean = true,
    val dedupeAbout: Boolean = true
)
