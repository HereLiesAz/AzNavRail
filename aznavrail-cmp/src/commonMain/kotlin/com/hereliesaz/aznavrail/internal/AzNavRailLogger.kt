package com.hereliesaz.aznavrail.internal

/**
 * Logger for AzNavRail internal events.
 *
 * The Android sibling uses `android.util.Log`. In the multiplatform module the same object exposes
 * the same API but routes each call through [platformLogE], which each target implements natively
 * (Android → `Log.e`, everything else → `println` to stderr with a stack-trace dump).
 */
internal object AzNavRailLogger {
    /** Whether logging is enabled. */
    var enabled: Boolean = true

    /** Logs an error via the platform's native logger. No-ops when [enabled] is false. */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            platformLogE(tag, message, throwable)
        }
    }
}

/**
 * Platform-specific error log implementation. Fanned out via `expect/actual`:
 *  - Android → `android.util.Log.e(tag, message, throwable)`
 *  - Desktop/iOS/wasmJs → `println("E/$tag: $message")` + `throwable?.printStackTrace()`.
 */
internal expect fun platformLogE(tag: String, message: String, throwable: Throwable?)
