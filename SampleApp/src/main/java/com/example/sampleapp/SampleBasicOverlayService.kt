package com.example.sampleapp

import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.service.AzNavRailSimpleOverlayService
import androidx.compose.material3.MaterialTheme

class SampleBasicOverlayService : AzNavRailSimpleOverlayService() {

    @Composable
    override fun OverlayContent() {
        // Fallback to MaterialTheme if MyApplicationTheme is missing in this context
        MaterialTheme {
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
