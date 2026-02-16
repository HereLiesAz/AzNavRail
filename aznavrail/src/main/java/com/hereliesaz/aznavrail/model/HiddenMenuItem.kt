package com.hereliesaz.aznavrail.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing an item in the hidden context menu of a relocatable rail item.
 *
 * @param id Unique identifier.
 * @param text Display text.
 * @param route Navigation route.
 * @param isInput If true, this item renders as a text input field.
 * @param hint Hint text for the input field.
 */
@Parcelize
data class HiddenMenuItem(
    val id: String,
    val text: String,
    val route: String? = null,
    val isInput: Boolean = false,
    val hint: String? = null
) : Parcelable
