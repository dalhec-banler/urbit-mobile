package io.nativeplanet.urbit.launcher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.stringPreferencesKey
import io.nativeplanet.urbit.launcher.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.dataStore.data.first()
            val savedCode = prefs[stringPreferencesKey("ship_code")]

            if (savedCode != null) {
                UrbitService.start(context)
            }
        }
    }
}
