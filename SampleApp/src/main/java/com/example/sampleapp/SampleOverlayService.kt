package com.example.sampleapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.service.AzNavRailOverlayService
import com.hereliesaz.aznavrail.service.AzNavRailWindowService
import com.hereliesaz.aznavrail.AzStrictLayout

class SampleOverlayService : AzNavRailOverlayService() {

    override fun getNotification(): Notification {
        val channelId = "overlay_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AzNavRail Overlay")
            .setContentText("Overlay is running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    @OptIn(AzStrictLayout::class)
    @Composable
    override fun OverlayContent() {
        // We wrap the content in our theme so it looks consistent with the app
        MaterialTheme {
            AzNavRail {
                azConfig(
                    dockingSide = AzDockingSide.LEFT,
                    packButtons = true,
                    displayAppName = false
                )

                azTheme(
                    defaultShape = AzButtonShape.CIRCLE
                )

                azAdvanced(
                    enableRailDragging = true,
                    onUndock = { stopSelf() }
                )

                azRailItem(
                    id = "home",
                    text = "Home",
                    route = "home"
                )

                azRailItem(
                    id = "settings",
                    text = "Settings",
                    route = "settings"
                )
            }
        }
    }
}
