package com.hereliesaz.aznavrail.internal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.hereliesaz.aznavrail.service.AzNavRailOverlayService

internal object OverlayHelper {
    fun launch(context: Context, serviceClass: Class<*>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } else {
            val intent = Intent(context, serviceClass)
            val isForegroundOverlay = AzNavRailOverlayService::class.java.isAssignableFrom(serviceClass)

            if (isForegroundOverlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
