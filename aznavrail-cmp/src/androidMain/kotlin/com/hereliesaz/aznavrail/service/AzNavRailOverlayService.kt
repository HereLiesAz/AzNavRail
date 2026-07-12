package com.hereliesaz.aznavrail.service

import android.content.pm.ServiceInfo
import android.os.Build
import com.hereliesaz.aznavrail.internal.AzNavRailLogger

/**
 * Service class for creating a system overlay with AzNavRail using a Foreground Service.
 *
 * To use this, extend this class and implement [OverlayContent] and [getNotification].
 * You must also declare the [android.permission.SYSTEM_ALERT_WINDOW] and [android.permission.FOREGROUND_SERVICE] permissions in your manifest.
 * For Android 14+, you must also declare [android.permission.FOREGROUND_SERVICE_SPECIAL_USE] and the service type in the manifest.
 */
abstract class AzNavRailOverlayService : AzNavRailWindowService() {

    /**
     * Returns the notification to be displayed while the service is running.
     */
    abstract fun getNotification(): android.app.Notification

    /**
     * Returns the ID for the notification. Defaults to 1234.
     */
    open fun getNotificationId(): Int = 1234

    override fun onCreate() {
        super.onCreate()

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(getNotificationId(), getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(getNotificationId(), getNotification())
            }
        } catch (e: Exception) {
             AzNavRailLogger.e("AzNavRailOverlayService", "Failed to start foreground service", e)
        }
    }
}
