package com.hereliesaz.aznavrail.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.hereliesaz.aznavrail.AzAppMeta
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.LocalAzAppMeta
import com.hereliesaz.aznavrail.LocalAzNavHostPresent
import com.hereliesaz.aznavrail.LocalAzNavHostScope
import com.hereliesaz.aznavrail.rememberAzNavHostScope

/**
 * The shared demo UI, used by every platform entry point.
 *
 * `AzNavRail` checks [LocalAzNavHostPresent] and renders a red "Configuration Error" placeholder
 * when it is `false` (the default) — the Android sibling relies on `AzHostActivityLayout` to set it,
 * which has no cross-platform analogue, so the CMP consumer provides it themselves. That single local
 * is the only *required* wiring; the rest have safe defaults but are supplied here to showcase the
 * full feature set:
 *  - [LocalAzAppMeta] names the app and (via `packageId`) lets the About screen derive its GitHub repo.
 *  - [LocalAzNavHostScope] enables the built-in About / Help / More-from-Az overlays.
 */
@Composable
fun App() {
    MaterialTheme {
        CompositionLocalProvider(
            LocalAzNavHostPresent provides true,
            LocalAzAppMeta provides AzAppMeta(
                name = "AzNavRail Demo",
                packageId = "com.hereliesaz.aznavrail",
            ),
            LocalAzNavHostScope provides rememberAzNavHostScope(),
        ) {
            AzNavRail {
                azRailItem(id = "home", text = "Home", onClick = {})
                azRailItem(id = "docs", text = "Docs", onClick = {})
                azMenuItem(id = "about", text = "About", onClick = {})
                azMenuItem(id = "settings", text = "Settings", onClick = {})
            }
        }
    }
}
