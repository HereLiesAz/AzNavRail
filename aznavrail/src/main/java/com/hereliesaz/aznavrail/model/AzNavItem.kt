package com.hereliesaz.aznavrail.model

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.Collections.emptySet

/**
 * The unified, stateless data model for any item in the navigation rail or menu.
 *
 * @param id A unique identifier for this item.
 * @param text The text to display for this item.
 * @param route The navigation route associated with this item.
 * @param screenTitle The title to display on the screen when this item is active.
 * @param isRailItem If `true`, this item will be displayed on the collapsed rail.
 * @param color The color for the rail button's text and border.
 * @param isToggle If `true`, this item behaves like a toggle.
 * @param isChecked The current checked state of the toggle.
 * @param toggleOnText The text to display when the toggle is on.
 * @param toggleOffText The text to display when the toggle is off.
 * @param isCycler If `true`, this item behaves like a cycler.
 * @param options The list of options for a cycler.
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
 * @param hiddenMenuItems List of items for the hidden context menu (for reloc items).
 * @param classifiers A set of strings to classify this item (for active state).
 * @param content Dynamic content (Color, Number, Image) to display on the button.
 * @param isNestedRail If `true`, this item triggers a nested rail popup.
 * @param nestedRailAlignment The alignment of the nested rail.
 * @param nestedRailItems The list of items within the nested rail.
 */
@Parcelize
data class AzNavItem(
    val id: String,
    val text: String,
    val route: String? = null,
    val screenTitle: String? = null,
    val isRailItem: Boolean,
    val color: @RawValue Color? = null,
    val isToggle: Boolean = false,
    val isChecked: Boolean? = null,
    val toggleOnText: String = "",
    val toggleOffText: String = "",
    val isCycler: Boolean = false,
    val options: List<String>? = null,
    val selectedOption: String? = null,
    val isDivider: Boolean = false,
    val collapseOnClick: Boolean = true,
    val shape: AzButtonShape = AzButtonShape.CIRCLE,
    val disabled: Boolean = false,
    val disabledOptions: List<String>? = null,
    val isHost: Boolean = false,
    val isSubItem: Boolean = false,
    val hostId: String? = null,
    val isExpanded: Boolean = false,
    val info: String? = null,
    val isRelocItem: Boolean = false,
    val hiddenMenuItems: List<HiddenMenuItem>? = null,
    val classifiers: Set<String> = emptySet(),
    val content: @RawValue Any? = null,
    val isNestedRail: Boolean = false,
    val nestedRailAlignment: AzNestedRailAlignment? = null,
    val nestedRailItems: List<AzNavItem>? = null
) : Parcelable
