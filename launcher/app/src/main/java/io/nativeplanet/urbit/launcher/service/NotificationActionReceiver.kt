package io.nativeplanet.urbit.launcher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.nativeplanet.urbit.launcher.data.dataStore
import io.nativeplanet.urbit.launcher.widget.UrbitWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_READ -> {
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                if (notificationId >= 0) {
                    NotificationHelper.clearNotification(context, notificationId)
                }
            }
            ACTION_CLEAR_ALL -> {
                NotificationHelper.clearAllNotifications(context)
                CoroutineScope(Dispatchers.IO).launch {
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey("unread_count")] = "0"
                    }
                    UrbitWidget.notifyUpdate(context)
                }
            }
        }
    }

    companion object {
        const val ACTION_MARK_READ = "io.nativeplanet.urbit.launcher.ACTION_MARK_READ"
        const val ACTION_CLEAR_ALL = "io.nativeplanet.urbit.launcher.ACTION_CLEAR_ALL"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
