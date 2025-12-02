package com.hereliesaz.aznavrail.internal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap

internal object BubbleHelper {

    const val DISMISS_ACTION_SUFFIX = ".AZNAVRAIL_DISMISS_BUBBLE"

    fun launch(context: Context, targetActivity: Class<*>) {
        // Bubbles are supported on Android 10 (API 29) and higher.
        // However, the Bubble API was developer preview in 10 and finalized in 11.
        // For simplicity, we check for Q (API 29) as per the sample app logic.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val target = Intent(context, targetActivity)
        target.setAction(Intent.ACTION_MAIN)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val bubbleIntent = PendingIntent.getActivity(
            context,
            0,
            target,
            flags
        )

        val dismissIntent = Intent("${context.packageName}$DISMISS_ACTION_SUFFIX")
        dismissIntent.setPackage(context.packageName)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Try to get the app icon, fallback to system default
        val icon = try {
            val pm = context.packageManager
            val appInfo: ApplicationInfo = pm.getApplicationInfo(context.packageName, 0)

            // Prefer roundIcon if available (API 25+), otherwise use standard icon
            val iconResId = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val field = ApplicationInfo::class.java.getField("roundIcon")
                    val roundIcon = field.getInt(appInfo)
                    if (roundIcon != 0) roundIcon else appInfo.icon
                } else {
                    appInfo.icon
                }
            } catch (e: Exception) {
                appInfo.icon
            }

            if (iconResId != 0) {
                IconCompat.createWithResource(context, iconResId)
            } else {
                val appIconDrawable = appInfo.loadIcon(pm)
                IconCompat.createWithBitmap(appIconDrawable.toBitmap())
            }
        } catch (e: PackageManager.NameNotFoundException) {
            IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon)
        }

        val bubbleData = NotificationCompat.BubbleMetadata.Builder(bubbleIntent, icon)
            .setDesiredHeight(600)
            .setAutoExpandBubble(true)
            .setSuppressNotification(true)
            .build()

        val currentUser = Person.Builder()
            .setName("You")
            .build()

        val chatPartner = Person.Builder()
            .setName("NavRail")
            .setIcon(icon)
            .setImportant(true)
            .build()

        val channelId = "bubble_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Bubbles", NotificationManager.IMPORTANCE_HIGH)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val shortcutId = "navrail_bubble"
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel("NavRail")
            .setLongLabel("NavRail Bubble")
            .setIcon(icon)
            .setIntent(target)
            .setPerson(chatPartner)
            .setLongLived(true)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("NavRail Overlay")
            .setContentText("Tap to access")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setBubbleMetadata(bubbleData)
            .setShortcutId(shortcutId)
            .addPerson(chatPartner)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setDeleteIntent(dismissPendingIntent)
            .setStyle(
                NotificationCompat.MessagingStyle(currentUser)
                    .setConversationTitle("NavRail")
                    .setGroupConversation(false)
                    .addMessage("Tap to access", System.currentTimeMillis(), chatPartner)
            )

        notificationManager.notify(1, builder.build())
    }
}
