package com.hereliesaz.aznavrail.model

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * The unified, stateless data model for any item in the navigation rail or menu.
 *
 * @param id A unique identifier for this item.
 * @param text The text to display for this item.
 * @param menuText Optional alternate text to display when the item is in the expanded menu.
 * @param route The navigation route associated with this item.
 * @param screenTitle The title to display on the screen when this item is active.
 * @param isRailItem If `true`, this item will be displayed on the collapsed rail.
 * @param color The color for the rail button's border and base state.
 * @param textColor The color for the text (overrides color).
 * @param fillColor The color for the shape's translucent fill.
 * @param translucentBackgroundColor When non-null, used *verbatim* (its own alpha included) as this
 *   item's button fill, bypassing the library's hardcoded `0.12f`/`0.25f` alpha computation over
 *   [fillColor]. Distinct from the rail-level `azTheme(translucentBackground = …)`, which only
 *   styles full-panel surfaces (About reader, More-from-Az, hidden-menu box) and never an
 *   individual button; this styles one item's own button fill, alpha included. Null (the default)
 *   is today's exact behaviour.
 * @param isToggle If `true`, this item behaves like a toggle.
 * @param isChecked The current checked state of the toggle.
 * @param toggleOnText The text to display when the toggle is on.
 * @param toggleOffText The text to display when the toggle is off.
 * @param menuToggleOnText Optional alternate text to display when the toggle is on in the menu.
 * @param menuToggleOffText Optional alternate text to display when the toggle is off in the menu.
 * @param isCycler If `true`, this item behaves like a cycler.
 * @param options The list of options for a cycler.
 * @param menuOptions Optional alternate list of options for a cycler in the menu.
 * @param selectedOption The currently selected option for a cycler.
 * @param isDivider If `true`, this item is a divider.
 * @param collapseOnClick If `true`, the navigation rail will collapse after this item is clicked.
 * @param shape The shape of the button.
 * @param disabled Whether the item is disabled.
 * @param disabledOptions A list of specific options that are disabled (for cyclers).
 * @param isHost If `true`, this item is a host for sub-items.
 * @param isSubItem If `true`, this item is a child of a host.
 * @param hostId The ID of the parent host item (if this is a sub-item).
 * @param isExpanded Whether the host item is currently expanded.
 * @param info The help text for the info screen.
 * @param isRelocItem If `true`, this item is a reorderable item.
 * @param hiddenMenuItems List of items for the hidden context menu, opened by long-pressing this item. Available on any rail item, not just reloc items.
 * @param classifiers A set of strings to classify this item (for active state).
 * @param content Dynamic content (Color, Number, Image) to display on the button.
 * @param isNestedRail If `true`, this item triggers a nested rail popup.
 * @param nestedRailAlignment The alignment of the nested rail.
 * @param nestedRailItems The list of items within the nested rail.
 * @param reflectSelectionInParent When `true` on an [isNestedRail] item, the parent button's
 *   displayed `text`/`menuText`/`content` are derived from the currently selected child (see
 *   [selectedChildId]) instead of the parent's own declared label — only the label/content swap,
 *   the parent's own `color`/`shape`/`fillColor` are unaffected. A tap on the parent then fires the
 *   selected child's own action directly instead of opening the popup; a long-press opens the popup
 *   instead. Default `false` is pixel-for-pixel today's behaviour: tap always opens the popup.
 * @param selectedChildId The id of the currently "selected" child in this [isNestedRail] item's
 *   [nestedRailItems], consulted only when [reflectSelectionInParent] is `true`. Only a direct,
 *   non-host child may be selected — a host child can still be tapped to expand/collapse as normal,
 *   it just never becomes the parent's displayed selection. Null falls back to the first non-host
 *   child in [nestedRailItems]. Set here to choose the initial selection; the library then tracks
 *   subsequent taps itself (survives recomposition the same way `itemOverrides` does), so a later
 *   change to this DSL value after first appearance does not fight the user's own taps.
 * @param isHelpItem If `true`, clicking this item toggles the Help/Info overlay.
 */
@Parcelize
data class AzNavItem(
    val id: String,
    val text: String,
    val menuText: String? = null,
    val badge: String? = null,
    /** Whether the badge should remain permanently visible (true) or dissolve after 1 second (false). */
    val persistentBadge: Boolean = false,
    val route: String? = null,
    val screenTitle: String? = null,
    val isRailItem: Boolean,
    val color: @RawValue Color? = null,
    /**
     * Colour for this item's **active** highlight — the one it wears when its route is the current
     * destination or one of its classifiers is active. Null takes the rail's own
     * (`azTheme(activeColor = …)`, then the rail accent). Set with `azHighlight(id, active = …)`.
     */
    val activeColor: @RawValue Color? = null,
    /**
     * Colour for this item's **focus** highlight — pressed, or last tapped when it carries no route.
     * Null falls back to [activeColor], which is the library's historical behaviour: focus and
     * active looked identical until they were told apart.
     */
    val focusColor: @RawValue Color? = null,
    /**
     * Colour for this item's **secondary** highlight, the one the app drives itself. Null takes the
     * rail's `azTheme(secondaryColor = …)`. Inert unless [isSecondaryActive] is set.
     */
    val secondaryColor: @RawValue Color? = null,
    /**
     * Colour for this item's **tertiary** highlight — a second app-driven highlight, ranked below
     * secondary. Null takes the rail's `azTheme(tertiaryColor = …)`. Inert unless
     * [isTertiaryActive] is set.
     */
    val tertiaryColor: @RawValue Color? = null,
    /**
     * Whether the secondary highlight is currently lit. Set by `azItemState(id, secondary = true)`
     * or by a classifier listed in `azConfig(secondaryClassifiers = …)`. Never set by the library.
     */
    val isSecondaryActive: Boolean = false,
    /**
     * Whether the tertiary highlight is currently lit. Set by `azItemState(id, tertiary = true)`
     * or by a classifier listed in `azConfig(tertiaryClassifiers = …)`. Never set by the library.
     */
    val isTertiaryActive: Boolean = false,
    val textColor: @RawValue Color? = null,
    val fillColor: @RawValue Color? = null,
    val translucentBackgroundColor: @RawValue Color? = null,
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
    val content: @RawValue Any? = null,
    val isNestedRail: Boolean = false,
    val nestedRailAlignment: AzNestedRailAlignment? = null,
    val nestedRailItems: List<AzNavItem>? = null,
    val reflectSelectionInParent: Boolean = false,
    val selectedChildId: String? = null,
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
    val sliderValueFormatter: @RawValue ((Float) -> String)? = null
) : Parcelable {
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
            translucentBackgroundColor: Color? = null,
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
            translucentBackgroundColor = translucentBackgroundColor,
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
            translucentBackgroundColor: Color? = null,
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
            translucentBackgroundColor = translucentBackgroundColor,
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
