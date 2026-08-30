package com.hereliesaz.sampleapp

import androidx.navigation.NavGraphBuilder
import com.hereliesaz.aznavrail.AzActivity
import com.hereliesaz.aznavrail.AzNavRailScope
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.RailItem

/**
 * Regression check for the KSP processor: a host class with `@Az` on its **members only** — no
 * class-level `@Az` at all — used to be silently skipped entirely (never appeared in
 * `getSymbolsWithAnnotation`'s results as a class, so `generate()` never ran for it and no
 * `<Name>AzGraph` file was produced). If this file compiles, `AzGraphMemberOnlyDemoActivityAzGraph`
 * exists and the fix holds.
 */
class AzGraphMemberOnlyDemoActivity : AzActivity() {

    override val graph = AzGraphMemberOnlyDemoActivityAzGraph

    @Az(rail = RailItem(id = "member-only-home", text = "Home", route = "member-only-home"))
    fun goHome() = Unit

    override fun azGraphDestinations(builder: NavGraphBuilder) = Unit

    override fun AzNavRailScope.configureRail() = Unit
}
