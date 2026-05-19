package com.hereliesaz.SampleApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.service.AzNavRailSimpleOverlayService

/**
 * Minimal SYSTEM_ALERT_WINDOW overlay backed by AzNavRailSimpleOverlayService.
 * Launched from FabOverlayDemoScreen after the user grants the overlay permission.
 */
class SampleOverlayService : AzNavRailSimpleOverlayService() {
    @Composable
    override fun OverlayContent() {
        MaterialTheme {
            Column(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC1A237E))
                    .padding(16.dp),
            ) {
                Text("SampleOverlayService", color = Color.White)
                Text("Drag from the app to undock.", color = Color.White)
            }
        }
    }
}
