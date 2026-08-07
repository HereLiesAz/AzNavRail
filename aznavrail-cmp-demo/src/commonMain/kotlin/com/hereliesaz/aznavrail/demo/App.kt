package com.hereliesaz.aznavrail.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzAppMeta
import com.hereliesaz.aznavrail.AzDropdownMenu
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzPopupKind
import com.hereliesaz.aznavrail.LocalAzAppMeta
import com.hereliesaz.aznavrail.model.AzDropdownTrigger
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import com.hereliesaz.aznavrail.rememberAzPopupController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hereliesaz.aznavrail.model.AzButtonShape
import androidx.compose.ui.graphics.Color

/**
 * The shared demo UI, used by every platform entry point.
 *
 * It runs inside [AzHostActivityLayout], which is what supplies the safe zones, the big screen
 * title, the title-row drop-down triggers, the unattached-host anchors and the popup layer. (The
 * demo used to mount a bare `AzNavRail` behind hand-provided CompositionLocals, so none of those
 * host-rendered surfaces could be shown at all.)
 *
 * [LocalAzAppMeta] is still provided by hand: desktop and web have no OS-level app name or icon, and
 * `packageId` is what lets the About screen derive its GitHub repo.
 */
@Composable
fun App() {
    MaterialTheme {
        CompositionLocalProvider(
            LocalAzAppMeta provides AzAppMeta(
                name = "AzNavRail Demo",
                packageId = "com.hereliesaz.aznavrail",
            ),
        ) {
            DemoHost()
        }
    }
}

@Composable
private fun DemoHost() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val alerts = rememberAzPopupController()

    var syncing by remember { mutableStateOf(false) }
    var unread by remember { mutableIntStateOf(3) }
    var dark by remember { mutableStateOf(true) }
    var grid by remember { mutableStateOf(false) }
    // The third highlight is the app's to drive; nothing in the library ever lights it.
    var armed by remember { mutableStateOf(false) }

    AzHostActivityLayout(navController = navController) {
        // `vibrate` turns on the haptic vocabulary: every commit answers, not just FAB activation.
        azConfig(
            vibrate = true,
            // The declarative half of the secondary highlight: every item tagged "armed" wears it
            // while the toggle below is on.
            secondaryClassifiers = if (armed) setOf("armed") else emptySet(),
        )
        azAdvanced(enableRailDragging = true)

        // The three highlights, each answering a different question. Active is where you are; focus
        // is what you are touching (left unset here, so it reuses the active colour, which is how
        // the rail has always looked); secondary is a condition only this app knows about, so it
        // gets an amber rather than a second navigation colour.
        azTheme(secondaryColor = Color(0xFFFFB300))

        // --- Ordinary rail items ---------------------------------------------------------------
        // Every item is drawn in the same white, so the rail *reads* as white — and that is the
        // colour the drop-down, the unattached rail and the About reader all inherit, without an
        // `activeColor` being set anywhere. See `azResolveRailAccent`.
        azRailItem(id = "home", text = "Home", screenTitle = "Home", color = Color.White) {}
        azRailItem(id = "docs", text = "Docs", screenTitle = "Docs", color = Color.White) {}

        // The borderless family keeps the footprint of the base shape it names, so a borderless
        // item still lines up with the bordered ones beside it.
        azRailItem(
            id = "flat",
            text = "Flat",
            screenTitle = "Flat",
            shape = AzButtonShape.NONE_CIRCLE,
            color = Color.White,
        ) {}

        // Per-item loading: only this button spins; the rest of the rail stays live.
        azRailItem(id = "sync", text = "Sync", screenTitle = "Sync") {
            if (!syncing) {
                syncing = true
                scope.launch {
                    delay(2500)
                    syncing = false
                    unread += 1
                }
            }
        }
        // Declared by id, so the same call decorates any item type, anywhere.
        azItemState(
            id = "sync",
            isLoading = syncing,
            badge = unread.takeIf { it > 0 }?.toString(),
        )

        azRailToggle(
            id = "theme",
            isChecked = dark,
            toggleOnText = "Dark",
            toggleOffText = "Light",
        ) { dark = !dark }

        // Raising a WARNING with no itemId binds it to whatever was touched last, and redraws that
        // item as the yellow rounded-corner triangle for as long as the popup is up.
        azRailItem(id = "warn", text = "Warn", screenTitle = "Warn") {
            alerts.show(
                kind = AzPopupKind.WARNING,
                title = "Offline",
                message = "Changes are queued and will sync when the connection returns.",
            )
        }

        // --- The third highlight ----------------------------------------------------------------
        // "Armed" is a condition, not a destination. It is not where the user is (active) and not
        // what they are touching (focus), so it takes the highlight that exists for exactly that.
        azRailToggle(
            id = "armed",
            isChecked = armed,
            toggleOnText = "Armed",
            toggleOffText = "Safe",
            classifiers = setOf("armed"),
        ) { armed = !armed }
        azItemState(id = "armed", secondary = armed)

        azMenuItem(id = "settings", text = "Settings", screenTitle = "Settings") {}

        // The rail ends with an About ("?") item on its own. Declaring one places it yourself —
        // here, right after the menu item — and `azAbout(aboutRailItem = false)` drops it. Tapping
        // it opens the reader; tapping any other rail or menu item, or the app icon, closes it.
        azAboutRailItem(
            id = "about",
            text = "?",
            color = Color.White,
            info = "The About reader. Persistent, but yours to place.",
        )

        // --- Unattached hosts ------------------------------------------------------------------
        // A rail host that lives outside the rail: draggable, and its position is remembered.
        azUnattachedHostItem(
            id = "tools",
            text = "Tools",
            anchor = AzUnattachedAnchor.FLOATING,
        )
        azRailSubItem(id = "measure", hostId = "tools", text = "Measure") {}
        azRailSubToggle(
            id = "grid",
            hostId = "tools",
            isChecked = grid,
            toggleOnText = "Grid On",
            toggleOffText = "Grid Off",
        ) { grid = !grid }

        // A second host, anchored bottom-opposite, stacking its own children beneath it.
        azUnattachedHostItem(
            id = "layers",
            text = "Layers",
            anchor = AzUnattachedAnchor.BOTTOM,
        )
        azRailSubItem(id = "layer-base", hostId = "layers", text = "Base") {}
        azRailSubItem(id = "layer-over", hostId = "layers", text = "Over") {}

        // --- Popup bound to the rail -----------------------------------------------------------
        azPopup(alerts)

        // --- Onscreen content, plus two title-row drop-downs ------------------------------------
        onscreen(alignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Tap Sync to watch one item spin on its own.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Drag the floating Tools host anywhere; it comes back there.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Declared here, drawn up beside the screen title — and the two line up next to each
            // other in declaration order.
            AzDropdownMenu(navController = navController) {
                azConfig(showFooter = false)
                azItem("Reset") { unread = 0 }
                azItem("Add badge") { unread += 1 }
            }
            AzDropdownMenu(navController = navController) {
                azConfig(showFooter = false, trigger = AzDropdownTrigger.Text("View"))
                azToggle(
                    isChecked = grid,
                    toggleOnText = "Grid On",
                    toggleOffText = "Grid Off",
                ) { grid = it }
                azDivider()
                azItem("Notice") {
                    alerts.show(
                        itemId = "home",
                        kind = AzPopupKind.NOTICE,
                        title = "Heads up",
                        message = "Home is flagged while this notice is open.",
                    )
                }
            }
        }
    }
}
