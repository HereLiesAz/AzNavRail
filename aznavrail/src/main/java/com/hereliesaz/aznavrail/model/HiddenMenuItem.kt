package com.hereliesaz.aznavrail.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HiddenMenuItem(
    val id: String,
    val text: String,
    val route: String? = null,
    val isInput: Boolean = false,
    val hint: String? = null
) : Parcelable
