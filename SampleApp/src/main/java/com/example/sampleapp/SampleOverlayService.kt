package com.example.sampleapp

import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.service.AzNavRailOverlayService

class SampleOverlayService : AzNavRailOverlayService() {
    override val content = androidx.compose.runtime.Composable {
        // In Overlay Service, we render AzNavRail directly because the service
        // IS the container. However, we still use the segmented config.
        
        com.hereliesaz.aznavrail.AzNavRail {
            azTheme(
                activeColor = Color.Magenta,
                expandedWidth = 200.dp
            )
            azConfig(
                dockingSide = AzDockingSide.RIGHT,
                packButtons = true
            )
            azAdvanced(
                 enableRailDragging = true
            )

            azRailItem(id = "overlay_1", text = "Close Overlay", onClick = { stopSelf() })
            azRailItem(id = "overlay_2", text = "Action", onClick = { })
        }
    }
}
