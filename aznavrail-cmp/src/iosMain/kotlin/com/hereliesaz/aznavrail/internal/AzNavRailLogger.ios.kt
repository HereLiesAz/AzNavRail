package com.hereliesaz.aznavrail.internal

internal actual fun platformLogE(tag: String, message: String, throwable: Throwable?) {
    println("E/$tag: $message")
    throwable?.printStackTrace()
}
