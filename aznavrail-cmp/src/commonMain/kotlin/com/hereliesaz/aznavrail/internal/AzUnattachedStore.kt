package com.hereliesaz.aznavrail.internal

import com.hereliesaz.aznavrail.service.azCacheSettings

/** Which screen edge a top-level `FLOATING` unattached rail is pinned to; [FREE] is unpinned. */
internal enum class AzFloatingDock { FREE, TOP, BOTTOM, OPPOSITE }

/**
 * A `FLOATING` rail's persisted resting spot.
 *
 * @param a While [dock] is [AzFloatingDock.FREE], the x fraction of the window. Otherwise, the
 *   rail's sort key among peers docked to the same edge (see `edgeDockedPosition` in
 *   `AzUnattachedRail.kt`) — a fraction too, so it survives a resize/rotation the same way.
 * @param b While [dock] is [AzFloatingDock.FREE], the y fraction of the window. Unused otherwise.
 */
internal data class AzFloatingSave(val dock: AzFloatingDock, val a: Float, val b: Float)

/**
 * Persistence for each [com.hereliesaz.aznavrail.model.AzUnattachedAnchor.FLOATING] rail's resting
 * spot — which screen edge (if any) it is docked to, and its position along/off that edge.
 *
 * Everything is stored as a **fraction of the window** rather than raw pixels, so a rail dropped near
 * the right edge on a phone comes back near the right edge after a rotation, a resize, or on a
 * different device. Keyed per rail id (not a single shared slot) since every `FLOATING` rail now
 * floats and docks independently. Rail-to-rail docking (two rails snapped to each other, forming a
 * group) is deliberately NOT persisted here — only where each rail personally rests is; see the KDoc
 * on `AzFloatingRailState` in `AzUnattachedRail.kt` for why. Backed by `azCacheSettings`
 * (multiplatform-settings), the same store the About reader's HTTP cache and the guidance controller
 * use: `SharedPreferences` on Android, `java.util.prefs` on desktop, `localStorage` on wasmJs,
 * `NSUserDefaults` on iOS.
 */
internal object AzUnattachedStore {

    private const val KEY_PREFIX = "az_unattached_floating_pos_"

    /** The saved resting spot for [hostId], or null when nothing is saved yet. */
    fun loadFloating(hostId: String): AzFloatingSave? {
        val raw = azCacheSettings.getString(KEY_PREFIX + hostId, "")
        if (raw.isBlank()) return null
        val parts = raw.split(',')
        if (parts.size != 3) return null
        val dock = runCatching { AzFloatingDock.valueOf(parts[0]) }.getOrNull() ?: return null
        val a = parts[1].toFloatOrNull() ?: return null
        val b = parts[2].toFloatOrNull() ?: return null
        return AzFloatingSave(dock, a.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
    }

    /** Saves [hostId]'s resting spot. */
    fun saveFloating(hostId: String, save: AzFloatingSave) {
        azCacheSettings.putString(
            KEY_PREFIX + hostId,
            "${save.dock.name},${save.a.coerceIn(0f, 1f)},${save.b.coerceIn(0f, 1f)}",
        )
    }
}
