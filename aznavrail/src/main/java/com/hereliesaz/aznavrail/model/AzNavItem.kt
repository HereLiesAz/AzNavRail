package com.hereliesaz.aznavrail.model

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * The unified, stateless data model for any item in the navigation rail or menu.
 * This is a text-only component; icons are not supported.
 *
 * @param id A unique identifier for this item.
 * @param text The text to display for this item. This is not used for toggle and cycler items.
 * @param isRailItem If `true`, this item will be displayed on the collapsed rail. All items are displayed in the expanded menu.
 * @param color The color for the rail button's text and border. Only applies if `isRailItem` is `true`.
 * @param isToggle If `true`, this item behaves like a toggle.
 * @param isChecked The current checked state of the toggle.
 * @param toggleOnText The text to display when the toggle is on.
 * @param toggleOffText The text to display when the toggle is off.
 * @param isCycler If `true`, this item behaves like a cycler.
 * @param options The list of options for a cycler.
 * @param selectedOption The currently selected option for a cycler.
 * @param isDivider If `true`, this item is a divider.
 * @param collapseOnClick If `true`, the navigation rail will collapse after this item is clicked. This only applies to normal items (not toggles or cyclers).
 * @param onClick The lambda to be executed when the item is clicked. For toggles and cyclers, this is where you should update your state.
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
    val hiddenMenuItems: List<HiddenMenuItem>? = null
) : Parcelable
