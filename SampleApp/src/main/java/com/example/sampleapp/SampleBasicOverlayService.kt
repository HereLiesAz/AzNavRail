package com.example.sampleapp

import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.service.AzNavRailSimpleOverlayService

class SampleBasicOverlayService : AzNavRailSimpleOverlayService() {

    @Composable
    override fun OverlayContent() {
        MyApplicationTheme {
            SampleScreen(
                enableRailDragging = true,
                initiallyExpanded = false,
                onUndockOverride = {
                     // Clicking undock in overlay mode should close the overlay
                     stopSelf()
                },
                // Removed manual onOverlayDrag, relying on automatic behavior from AzNavRailOverlayService
                onRailDrag = null,
                showContent = false
            )
        }
    }
}
