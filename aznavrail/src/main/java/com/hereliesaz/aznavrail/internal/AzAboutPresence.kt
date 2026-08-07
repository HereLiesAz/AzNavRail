package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Every place an About affordance can be offered, ranked.
 *
 * The rank is the whole point: when more than one surface could show About, only the highest-ranked
 * one does. The order reads as "the most deliberate placement wins":
 *
 *  1. [RAIL_ITEM_EXPLICIT] — the developer declared `azAboutRailItem` themselves. Nothing outranks a
 *     decision that was made on purpose.
 *  2. [RAIL_MENU_FOOTER] — the rail's expanded menu footer, the library's canonical home for it.
 *  3. [DROPDOWN_FOOTER] — a drop-down menu's footer.
 *  4. [RAIL_ITEM_AUTO] — the automatic trailing `?` rail item, which the library added on its own and
 *     is therefore the first to give way.
 */
enum class AzAboutSurface(internal val priority: Int) {
    RAIL_ITEM_EXPLICIT(40),
    RAIL_MENU_FOOTER(30),
    DROPDOWN_FOOTER(20),
    RAIL_ITEM_AUTO(10),
}

/**
 * Keeps track of which surfaces currently offer an About affordance, so the library can render it in
 * exactly one of them.
 *
 * Without this, an app with a rail *and* a drop-down menu shows "About" twice — once in each footer —
 * plus a third time on the rail's own `?` button. Each surface registers what it *could* show; the
 * registry answers who actually shows it.
 *
 * Registration is by availability, not by visibility: a drop-down claims its footer as soon as the
 * drop-down exists, not when its panel opens. Resolving on availability keeps the answer stable —
 * the rail's `?` doesn't blink out of existence every time a panel opens over it.
 */
@Stable
class AzAboutRegistry {
    private val claims = mutableStateMapOf<Int, AzAboutSurface>()
    private var nextKey = 0

    /** A registration handle unique to one surface instance. */
    internal fun newKey(): Int = ++nextKey

    internal fun claim(key: Int, surface: AzAboutSurface) {
        claims[key] = surface
    }

    internal fun release(key: Int) {
        claims.remove(key)
    }

    /**
     * True when a surface of [surface] rank, registered under [key], is the one that should draw the
     * About affordance.
     *
     * The caller's own claim is deliberately excluded from the comparison, so the answer is correct
     * on the very first composition — before the [DisposableEffect] that registers it has run. Ties
     * are shared: two independent rails each keep their own footer About.
     */
    internal fun owns(key: Int, surface: AzAboutSurface): Boolean {
        var best: AzAboutSurface? = null
        for ((otherKey, otherSurface) in claims) {
            if (otherKey == key) continue
            if (best == null || otherSurface.priority > best.priority) best = otherSurface
        }
        return best == null || surface.priority >= best.priority
    }

    companion object {
        /**
         * The registry every rail and drop-down shares unless one is provided. A library-wide
         * default is what lets two *unrelated* components — a rail here, a drop-down there — agree on
         * who shows About without the developer wiring them together.
         */
        val Global: AzAboutRegistry = AzAboutRegistry()
    }
}

/** The [AzAboutRegistry] in force. Provide your own to scope de-duplication to one window/screen. */
val LocalAzAboutRegistry = staticCompositionLocalOf { AzAboutRegistry.Global }

/**
 * Registers this surface as a place that offers About, and answers whether it is the one that should
 * actually draw it.
 *
 * @param surface Which surface is asking.
 * @param offered Whether this surface has an About affordance to offer at all. A surface that offers
 *   nothing neither claims nor draws.
 * @param dedupe When false the caller opts out entirely: it always draws its About and never
 *   suppresses anyone else's.
 */
@Composable
internal fun rememberAzAboutOwnership(
    surface: AzAboutSurface,
    offered: Boolean,
    dedupe: Boolean = true,
): Boolean {
    val registry = LocalAzAboutRegistry.current
    val key = remember(registry) { registry.newKey() }
    val claiming = offered && dedupe
    DisposableEffect(registry, key, surface, claiming) {
        if (claiming) registry.claim(key, surface) else registry.release(key)
        onDispose { registry.release(key) }
    }
    if (!offered) return false
    if (!dedupe) return true
    return registry.owns(key, surface)
}
