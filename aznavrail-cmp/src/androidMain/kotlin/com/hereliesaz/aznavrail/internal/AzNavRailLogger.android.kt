package com.hereliesaz.aznavrail.internal

import android.util.Log

internal actual fun platformLogE(tag: String, message: String, throwable: Throwable?) {
    Log.e(tag, message, throwable)
}
