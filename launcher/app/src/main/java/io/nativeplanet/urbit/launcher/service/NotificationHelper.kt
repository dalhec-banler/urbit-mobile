package io.nativeplanet.urbit.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.nativeplanet.urbit.launcher.MainActivity
import io.nativeplanet.urbit.launcher.R

object NotificationHelper {

    const val SERVICE_CHANNEL_ID = "urbit_service"
    const val HARK_CHANNEL_ID = "urbit_hark"
    const val DM_CHANNEL_ID = "urbit_dm"
    const val MENTION_CHANNEL_ID = "urbit_mention"

    const val SERVICE_NOTIFICATION_ID = 1
    const val HARK_NOTIFICATION_BASE_ID = 1000
    const val GROUP_SUMMARY_ID = 999

    private const val GROUP_KEY_URBIT = "io.nativeplanet.urbit.NOTIFICATIONS"
    private val activeNotifications = mutableMapOf<Int, String>()

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Urbit Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Urbit is running"
            setShowBadge(false)
        }

        val harkChannel = NotificationChannel(
            HARK_CHANNEL_ID,
            "Urbit Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications from your Urbit ship"
        }

        val dmChannel = NotificationChannel(
            DM_CHANNEL_ID,
            "Direct Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Direct messages from other ships"
        }

        val mentionChannel = NotificationChannel(
            MENTION_CHANNEL_ID,
            "Mentions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "When someone mentions you"
        }

        manager.createNotificationChannels(
            listOf(serviceChannel, harkChannel, dmChannel, mentionChannel)
        )
    }

    fun createServiceNotification(
        context: Context,
        shipName: String?,
        isConnected: Boolean,
        unreadCount: Int = 0
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = shipName ?: "Urbit"
        val text = when {
            !isConnected -> "Connecting..."
            unreadCount > 0 -> "$unreadCount unread"
            else -> "Connected"
        }

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_urbit)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun createHarkNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = HARK_CHANNEL_ID,
        desk: String? = null,
        notificationId: Int = HARK_NOTIFICATION_BASE_ID
    ): Pair<Int, Notification> {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (desk != null) {
                putExtra(MainActivity.EXTRA_OPEN_DESK, desk)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_urbit)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_URBIT)
            .build()

        activeNotifications[notificationId] = "$title: $body"
        return notificationId to notification
    }

    fun createGroupSummary(context: Context): Notification {
        val count = activeNotifications.size
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, GROUP_SUMMARY_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("$count new notifications")

        activeNotifications.values.takeLast(5).forEach { line ->
            inboxStyle.addLine(line)
        }
        if (count > 5) {
            inboxStyle.setSummaryText("+${count - 5} more")
        }

        return NotificationCompat.Builder(context, HARK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_urbit)
            .setContentTitle("Urbit")
            .setContentText("$count new notifications")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_URBIT)
            .setGroupSummary(true)
            .setStyle(inboxStyle)
            .build()
    }

    fun notifyWithSummary(context: Context, id: Int, notification: Notification) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
        if (activeNotifications.size > 1) {
            manager.notify(GROUP_SUMMARY_ID, createGroupSummary(context))
        }
    }

    fun clearNotification(context: Context, id: Int) {
        activeNotifications.remove(id)
        cancel(context, id)
        if (activeNotifications.isEmpty()) {
            cancel(context, GROUP_SUMMARY_ID)
        } else if (activeNotifications.size == 1) {
            cancel(context, GROUP_SUMMARY_ID)
        }
    }

    fun clearAllNotifications(context: Context) {
        activeNotifications.keys.toList().forEach { id ->
            cancel(context, id)
        }
        activeNotifications.clear()
        cancel(context, GROUP_SUMMARY_ID)
    }

    fun notify(context: Context, id: Int, notification: Notification) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
    }

    fun cancel(context: Context, id: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(id)
    }
}
