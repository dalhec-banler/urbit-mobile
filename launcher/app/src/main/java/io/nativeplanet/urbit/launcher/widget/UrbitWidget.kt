package io.nativeplanet.urbit.launcher.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.datastore.preferences.core.stringPreferencesKey
import io.nativeplanet.urbit.launcher.MainActivity
import io.nativeplanet.urbit.launcher.R
import io.nativeplanet.urbit.launcher.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UrbitWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, UrbitWidget::class.java)
            )
            onUpdate(context, appWidgetManager, widgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE = "io.nativeplanet.urbit.launcher.WIDGET_UPDATE"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = context.dataStore.data.first()
                val shipName = prefs[stringPreferencesKey("ship_name")] ?: "~"
                val isConnected = prefs[stringPreferencesKey("is_connected")] == "true"
                val unreadCount = prefs[stringPreferencesKey("unread_count")]?.toIntOrNull() ?: 0

                val views = RemoteViews(context.packageName, R.layout.widget_urbit)

                // Set ship name
                views.setTextViewText(R.id.widget_ship_name, shipName)

                // Set status text
                val statusText = when {
                    !isConnected -> "disconnected"
                    unreadCount > 0 -> "$unreadCount unread"
                    else -> "connected"
                }
                views.setTextViewText(R.id.widget_status, statusText)

                // Set status indicator color
                val statusColor = if (isConnected) 0xFF4CAF50.toInt() else 0xFFEF5350.toInt()
                views.setInt(R.id.widget_status_dot, "setColorFilter", statusColor)

                // Set click intent to open launcher
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context, 0, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun notifyUpdate(context: Context) {
            val intent = Intent(context, UrbitWidget::class.java).apply {
                action = ACTION_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
