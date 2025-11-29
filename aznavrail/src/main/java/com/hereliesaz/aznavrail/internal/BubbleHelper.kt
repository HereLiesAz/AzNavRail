package com.hereliesaz.aznavrail.internal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap

internal object BubbleHelper {

    fun launch(context: Context, targetActivity: Class<*>) {
        // Bubbles are supported on Android 10 (API 29) and higher.
        // However, the Bubble API was developer preview in 10 and finalized in 11.
        // For simplicity, we check for Q (API 29) as per the sample app logic.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val target = Intent(context, targetActivity)
        val bubbleIntent = PendingIntent.getActivity(
            context,
            0,
            target,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Try to get the app icon, fallback to system default
        val icon = try {
            val appIconDrawable = context.packageManager.getApplicationIcon(context.packageName)
            IconCompat.createWithBitmap(appIconDrawable.toBitmap())
        } catch (e: Exception) {
            IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon)
        }

        val bubbleData = NotificationCompat.BubbleMetadata.Builder(bubbleIntent, icon)
            .setDesiredHeight(600)
            .setAutoExpandBubble(true)
            .setSuppressNotification(true)
            .build()

        val person = Person.Builder()
            .setName("NavRail")
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
            .setIntent(target.setAction(Intent.ACTION_MAIN))
            .setPerson(person)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("NavRail Overlay")
            .setContentText("Tap to open")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setBubbleMetadata(bubbleData)
            .setShortcutId(shortcutId)
            .addPerson(person)
            .setCategory(Notification.CATEGORY_STATUS)
            .setStyle(NotificationCompat.MessagingStyle(person).setConversationTitle("NavRail"))

        notificationManager.notify(1, builder.build())
    }
}
