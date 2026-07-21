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
    val keepNestedRailOpen: Boolean = false,
    /**
     * Optional short badge text (a few characters) drawn in a small circle on the corner of the
     * item's button. Recomputed from the DSL on every recomposition, so passing a state-backed
     * value updates the badge dynamically. Null/blank hides the badge.
     */
    val badge: String? = null,
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
        )
    }
}
