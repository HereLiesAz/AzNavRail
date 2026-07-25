package com.hereliesaz.SampleApp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import com.hereliesaz.aznavrail.AzActivity
import com.hereliesaz.aznavrail.AzNavRailScope
import com.hereliesaz.aznavrail.annotation.Advanced
import com.hereliesaz.aznavrail.annotation.App
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.Cycler
import com.hereliesaz.aznavrail.annotation.Divider
import com.hereliesaz.aznavrail.annotation.MenuItem
import com.hereliesaz.aznavrail.annotation.RailHost
import com.hereliesaz.aznavrail.annotation.RailItem
import com.hereliesaz.aznavrail.annotation.SubItem
import com.hereliesaz.aznavrail.annotation.Toggle

/**
 * The High-Inference API, end to end.
 *
 * Nothing here builds a rail by hand: the `@Az` annotations below are read at compile time and the
 * KSP processor generates `AzGraphDemoActivityAzGraph`, which [AzActivity] runs. Compare with
 * `MainApp.kt`, which writes the same kind of thing in the DSL directly — the DSL remains the full
 * API, and this is sugar over it.
 *
 * Existing as a second Activity (rather than replacing `MainActivity`) keeps the DSL showcase intact
 * while proving the generator actually produces something that compiles and runs.
 */
@Az(
    app = App(displayAppName = true, vibrate = true, startDestination = "graph-home"),
    advanced = Advanced(enableRailDragging = true),
)
class AzGraphDemoActivity : AzActivity() {

    override val graph = AzGraphDemoActivityAzGraph

    /** Bound reactively by `badgeProperty` — changing it updates the badge with no further wiring. */
    var unread by mutableIntStateOf(2)

    /** Bound by `isCheckedProperty`; the generated onClick flips it. */
    var dark by mutableStateOf(true)

    /** Bound by `selectedOptionProperty`; the generated onClick advances it. */
    var density by mutableStateOf("Comfortable")

    /** Bound by `textProperty` — the item's label is whatever this says. */
    var homeLabel by mutableStateOf("Home")

    @Az(rail = RailItem(id = "graph-home", route = "graph-home", textProperty = "homeLabel", badgeProperty = "unread"))
    fun goHome() {
        unread = 0
    }

    @Az(rail = RailItem(id = "graph-docs", text = "Docs", route = "graph-docs", screenTitle = "Docs"))
    fun goDocs() = Unit

    @Az(toggle = Toggle(id = "graph-theme", toggleOnText = "Dark", toggleOffText = "Light", isCheckedProperty = "dark"))
    fun toggleTheme() = Unit

    @Az(
        cycler = Cycler(
            id = "graph-density",
            options = ["Comfortable", "Compact", "Dense"],
            selectedOptionProperty = "density",
        )
    )
    fun cycleDensity() = Unit

    @Az(host = RailHost(id = "graph-tools", text = "Tools"))
    fun tools() = Unit

    @Az(sub = SubItem(id = "graph-measure", hostId = "graph-tools", text = "Measure"))
    fun measure() = Unit

    @Az(divider = Divider(enabled = true))
    fun separator() = Unit

    @Az(menu = MenuItem(id = "graph-settings", text = "Settings", route = "graph-settings"))
    fun openSettings() = Unit

    override fun azGraphDestinations(builder: NavGraphBuilder) {
        builder.composable("graph-home") { Text("Generated graph — Home") }
        builder.composable("graph-docs") { Text("Generated graph — Docs") }
        builder.composable("graph-settings") { Text("Generated graph — Settings") }
    }

    /** Anything the annotations can't express still goes here, inside the same DSL block. */
    override fun AzNavRailScope.configureRail() {
        azItemState(id = "graph-docs", badge = if (dark) null else "!")
    }
}
