package com.hereliesaz.aznavrail.internal

import android.content.Context

/**
 * Persistence for the [com.hereliesaz.aznavrail.model.AzUnattachedAnchor.FLOATING] stack's position.
 *
 * The position is stored as a **fraction of the window** rather than raw pixels, so a stack dropped
 * near the right edge comes back near the right edge after a rotation, a resize, or on a different
 * device. Backed by a private `SharedPreferences`, the same way the guidance controller persists
 * which tutorials have been completed.
 */
internal object AzUnattachedStore {

    private const val PREFS_NAME = "aznavrail_unattached"
    private const val KEY = "floating_pos"

    /** The saved position as `x` / `y` fractions of the window, or null when nothing is saved yet. */
    fun load(context: Context): Pair<Float, Float>? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return null
        val parts = raw.split(',')
        if (parts.size != 2) return null
        val x = parts[0].toFloatOrNull() ?: return null
        val y = parts[1].toFloatOrNull() ?: return null
        return x.coerceIn(0f, 1f) to y.coerceIn(0f, 1f)
    }

    /** Saves [xFraction] / [yFraction] (both 0..1) as the floating stack's home. */
    fun save(context: Context, xFraction: Float, yFraction: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, "${xFraction.coerceIn(0f, 1f)},${yFraction.coerceIn(0f, 1f)}")
            .apply()
    }
}
