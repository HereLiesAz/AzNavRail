package com.example.sampleapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import com.hereliesaz.aznavrail.service.AzNavRailOverlayService

class SampleOverlayService : AzNavRailOverlayService() {

    override fun getNotification(): Notification {
        val channelId = "overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AzNavRail Overlay")
            .setContentText("Running...")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .build()
    }

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
                overlayService = null,
                onOverlayDrag = { x, y -> updatePosition(x, y) },
                showContent = false
            )
        }
    }
}
