package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.Color

/**
 * The unified, stateless data model for any item in the navigation rail or menu.
 *
 * Ported from the Android sibling verbatim EXCEPT that `@Parcelize` / `Parcelable` /
 * `kotlinx.parcelize.RawValue` are dropped — those exist only on Android. The CMP copy is a plain
 * data class. All fields keep the same name and defaults.
 *
 * See the Android sibling's KDoc for per-param docs — they apply here 1:1.
 */
data class AzNavItem(
    val id: String,
    val text: String,
    val menuText: String? = null,
    val route: String? = null,
    val screenTitle: String? = null,
    val isRailItem: Boolean,
    val color: Color? = null,
    val textColor: Color? = null,
    val fillColor: Color? = null,
    /**
     * Colour for this item's **active** highlight — the one it wears when its route is the current
     * destination or one of its classifiers is active. Null takes the rail's own
     * (`azTheme(activeColor = …)`, then the rail accent). Set with `azHighlight(id, active = …)`.
     */
    val activeColor: Color? = null,
    /**
     * Colour for this item's **focus** highlight — pressed, or last tapped when it carries no route.
     * Null falls back to [activeColor], which is the library's historical behaviour: focus and
     * active looked identical until they were told apart.
     */
    val focusColor: Color? = null,
    /**
     * Colour for this item's **secondary** highlight, the one the app drives itself. Null takes the
     * rail's `azTheme(secondaryColor = …)`. Inert unless [isSecondaryActive] is set.
     */
    val secondaryColor: Color? = null,
    /**
     * Whether the secondary highlight is currently lit. Set by `azItemState(id, secondary = true)`
     * or by a classifier listed in `azConfig(secondaryClassifiers = …)`. Never set by the library.
     */
    val isSecondaryActive: Boolean = false,
    val isToggle: Boolean = false,
    val isChecked: Boolean? = null,
    val toggleOnText: String = "",
    val toggleOffText: String = "",
    val menuToggleOnText: String? = null,
    val menuToggleOffText: String? = null,
    val isCycler: Boolean = false,
    val options: List<String>? = null,
    val menuOptions: List<String>? = null,
    val selectedOption: String? = null,
    val isDivider: Boolean = false,
    val collapseOnClick: Boolean = true,
    val shape: AzButtonShape? = null,
    val disabled: Boolean = false,
    val disabledOptions: List<String>? = null,
    val isHost: Boolean = false,
    val isSubItem: Boolean = false,
    val hostId: String? = null,
    val isExpanded: Boolean = false,
    /**
     * When true, the host is auto-expanded the first time it appears (the user
     * can still collapse it).
     */
    val initiallyExpanded: Boolean = false,
    val info: String? = null,
    val isRelocItem: Boolean = false,
    val hiddenMenuItems: List<HiddenMenuItem>? = null,
    val forceHiddenMenuOpen: Boolean = false,
    val onHiddenMenuDismiss: (() -> Unit)? = null,
    val classifiers: Set<String> = emptySet(),
    val content: Any? = null,
    val isNestedRail: Boolean = false,
    val nestedRailAlignment: AzNestedRailAlignment? = null,
    val nestedRailItems: List<AzNavItem>? = null,
    val isHelpItem: Boolean = false,
    /**
     * True for the rail's About affordance — the trailing `?` button. It opens the in-app About
     * reader (or the repo URL when `inAppAbout` is false) and closes it again on a second tap. One
     * is appended to the end of the rail automatically; declaring your own with `azAboutRailItem`
     * replaces it, so its position, text, colour and shape are yours.
     */
    val isAboutItem: Boolean = false,
    val keepNestedRailOpen: Boolean = false,
    /**
     * Optional short badge text (a few characters) drawn in a small circle on the corner of the
     * item's button. Recomputed from the DSL on every recomposition, so passing a state-backed
     * value updates the badge dynamically. Null/blank hides the badge.
     */
    val badge: String? = null,
    /** Whether the badge should remain permanently visible (true) or dissolve after 1 second (false). */
    val persistentBadge: Boolean = false,
    /**
     * Per-item loading state. When true this item's button hides its content and spins its own
     * [com.hereliesaz.aznavrail.AzLoad] in place — every rail item is its own loading animation,
     * rather than the whole app being blocked by one global spinner.
     */
    val isLoading: Boolean = false,
    /**
     * Transient alert styling. Set by an [com.hereliesaz.aznavrail.AzPopupController] on the item
     * that raised a notice/warning popup, which redraws it as a yellow rounded-corner triangle
     * outline for as long as the popup is up. Null is the item's normal appearance.
     */
    val alert: AzItemAlert? = null,
    /**
     * True for a host declared with `azUnattachedHostItem`: it is a rail host that does NOT live in
     * the rail strip. It is drawn on its own at [unattachedAnchor] and unfolds its sub-items there.
     */
    val isUnattached: Boolean = false,
    /** Where an [isUnattached] host parks. Null (and ignored) for every other item. */
    val unattachedAnchor: AzUnattachedAnchor? = null,
    /**
     * True for an item declared with `azRailSlider`: tapping it unfolds an
     * [com.hereliesaz.aznavrail.AzSlider] **in the item's own slot on the rail**, rather than
     * opening a panel somewhere else. The value stays where the user was already looking.
     */
    val isSlider: Boolean = false,
    /** How an [isSlider] item's slider is shaped. Null for every other item. */
    val sliderConfig: AzSliderConfig? = null,
    /** The live value of an [isSlider] item, for every variant except `RANGE`. */
    val sliderValue: Float = 0f,
    /** The low end of an [isSlider] item's span, for the `RANGE` variant only. */
    val sliderRangeStart: Float = 0f,
    /** The high end of an [isSlider] item's span, for the `RANGE` variant only. */
    val sliderRangeEnd: Float = 1f,
    /**
     * How an [isSlider] item renders its value as the label under the track. Null prints the value
     * rounded to two decimals.
     */
    val sliderValueFormatter: ((Float) -> String)? = null,
) {
    companion object {
        /**
         * Factory method for creating an [AzNavItem] designated as a Help trigger.
         */
        fun Help(
            id: String,
            text: String = "Help",
            menuText: String? = null,
            isRailItem: Boolean = true,
            content: Any? = null,
            color: Color? = null,
            textColor: Color? = null,
            fillColor: Color? = null,
            shape: AzButtonShape? = null,
            badge: String? = null,
            persistentBadge: Boolean = false,
            isLoading: Boolean = false,
        ): AzNavItem = AzNavItem(
            id = id,
            text = text,
            menuText = menuText,
            isRailItem = isRailItem,
            isHelpItem = true,
            content = content,
            color = color,
            textColor = textColor,
            fillColor = fillColor,
            shape = shape,
            badge = badge,
            persistentBadge = persistentBadge,
            isLoading = isLoading,
        )

        /**
         * Factory method for creating an [AzNavItem] designated as the rail's About trigger — the
         * trailing `?` button.
         */
        fun About(
            id: String,
            text: String = "?",
            menuText: String? = null,
            isRailItem: Boolean = true,
            content: Any? = null,
            color: Color? = null,
            textColor: Color? = null,
            fillColor: Color? = null,
            shape: AzButtonShape? = null,
            info: String? = null,
            badge: String? = null,
            persistentBadge: Boolean = false,
            isLoading: Boolean = false,
        ): AzNavItem = AzNavItem(
            id = id,
            text = text,
            menuText = menuText,
            isRailItem = isRailItem,
            isAboutItem = true,
            info = info,
            content = content,
            color = color,
            textColor = textColor,
            fillColor = fillColor,
            shape = shape,
            badge = badge,
            persistentBadge = persistentBadge,
            isLoading = isLoading,
            // The About reader is a place you go, not a menu action; closing the drawer behind you
            // would hide the rail the reader is drawn beside.
            collapseOnClick = false,
        )
    }
}
