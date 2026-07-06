package com.hereliesaz.aznavrail.internal

internal actual fun platformLogE(tag: String, message: String, throwable: Throwable?) {
    System.err.println("E/$tag: $message")
    throwable?.printStackTrace()
}
