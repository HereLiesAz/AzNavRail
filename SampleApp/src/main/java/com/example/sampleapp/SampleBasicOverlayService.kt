package com.example.sampleapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.service.AzNavRailWindowService
import com.hereliesaz.aznavrail.AzStrictLayout

class SampleBasicOverlayService : AzNavRailWindowService() {

    @OptIn(AzStrictLayout::class)
    @Composable
    override fun OverlayContent() {
        MaterialTheme {
            AzNavRail {
                azConfig(
                    dockingSide = AzDockingSide.RIGHT,
                    packButtons = true,
                    displayAppName = false
                )

                azTheme(
                    defaultShape = AzButtonShape.SQUARE
                )

                azAdvanced(
                    enableRailDragging = true,
                    onUndock = { stopSelf() }
                )

                azRailItem(
                    id = "home",
                    text = "Home (Basic)",
                    route = "home"
                )

                azRailItem(
                    id = "settings",
                    text = "Settings (Basic)",
                    route = "settings"
                )
            }
        }
    }
}
