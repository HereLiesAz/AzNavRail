package com.hereliesaz.aznavrail.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class HiddenMenuItem(
    val id: String,
    val text: String,
    val route: String? = null,
    val isInput: Boolean = false,
    val hint: String? = null
) : Parcelable
