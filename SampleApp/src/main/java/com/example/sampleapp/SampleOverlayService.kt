package com.example.sampleapp

import android.app.Notification
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzStrictLayout
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.service.AzNavRailOverlayService

class SampleOverlayService : AzNavRailOverlayService() {

    override fun getNotification(): Notification {
         // Create a basic notification for the foreground service
         // (Implementation details would depend on your notification channel setup)
         // For now returning a dummy notification or null if allowed, but usually required.
         // Assuming base class or helper might exist, but abstract method forces it.
         // We will throw UnsupportedOperation for now or just return a minimal one if we had context.
         // Since I can't easily modify manifest/resources here, I'll return a stub if possible
         // or just fix the compilation by implementing it.
         return androidx.core.app.NotificationCompat.Builder(this, "channel_id")
             .setContentTitle("Overlay Active")
             .setSmallIcon(android.R.drawable.ic_menu_compass)
             .build()
    }

    @OptIn(AzStrictLayout::class)
    @Composable
    override fun OverlayContent() {
        // In Overlay Service, we render AzNavRail directly because the service
        // IS the container.
        
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
