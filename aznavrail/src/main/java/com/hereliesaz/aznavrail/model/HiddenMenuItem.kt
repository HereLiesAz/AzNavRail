package com.hereliesaz.aznavrail.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

/**
 * Represents a single item in the hidden context menu of a relocatable rail item.
 *
 * Items are created via [com.hereliesaz.aznavrail.HiddenMenuScope] and stored on [AzNavItem.hiddenMenuItems].
 *
 * @param id Auto-generated unique ID scoped to the parent item.
 * @param text Display text for list items; empty for input items.
 * @param route Navigation route to trigger on click, or null for callback-only items.
 * @param isInput If true, renders a text-input field instead of a label.
 * @param hint Placeholder text shown in the input field when [isInput] is true.
 * @param initialValue Pre-filled value for input items.
 */
@Parcelize
data class HiddenMenuItem(
    val id: String,
    val text: String,
    val route: String? = null,
    val isInput: Boolean = false,
    val hint: String? = null,
    val initialValue: String = ""
) : Parcelable
