package com.hereliesaz.aznavrail.internal

import com.hereliesaz.aznavrail.service.azCacheSettings

/**
 * Persistence for the [com.hereliesaz.aznavrail.model.AzUnattachedAnchor.FLOATING] stack's position.
 *
 * The position is stored as a **fraction of the window** rather than raw pixels, so a stack dropped
 * near the right edge on a phone comes back near the right edge after a rotation, a resize, or on a
 * different device. Backed by `azCacheSettings` (multiplatform-settings), the same store the About
 * reader's HTTP cache and the guidance controller use: `SharedPreferences` on Android,
 * `java.util.prefs` on desktop, `localStorage` on wasmJs, `NSUserDefaults` on iOS.
 */
internal object AzUnattachedStore {

    private const val KEY = "az_unattached_floating_pos"

    /** The saved position as `x` / `y` fractions of the window, or null when nothing is saved yet. */
    fun load(): Pair<Float, Float>? {
        val raw = azCacheSettings.getString(KEY, "")
        if (raw.isBlank()) return null
        val parts = raw.split(',')
        if (parts.size != 2) return null
        val x = parts[0].toFloatOrNull() ?: return null
        val y = parts[1].toFloatOrNull() ?: return null
        return x.coerceIn(0f, 1f) to y.coerceIn(0f, 1f)
    }

    /** Saves [xFraction] / [yFraction] (both 0..1) as the floating stack's home. */
    fun save(xFraction: Float, yFraction: Float) {
        azCacheSettings.putString(
            KEY,
            "${xFraction.coerceIn(0f, 1f)},${yFraction.coerceIn(0f, 1f)}",
        )
    }
}
