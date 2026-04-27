package io.nativeplanet.urbit.launcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.nativeplanet.urbit.launcher.data.dataStore
import io.nativeplanet.urbit.launcher.widget.UrbitWidget
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class UrbitService : Service() {

    companion object {
        private const val TAG = "UrbitService"
        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_DELAY_MS = 60000L

        fun start(context: Context) {
            val intent = Intent(context, UrbitService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UrbitService::class.java))
        }
    }

    inner class LocalBinder : Binder() {
        val service: UrbitService get() = this@UrbitService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sseJob: Job? = null
    private var reconnectDelay = RECONNECT_DELAY_MS

    private val eventId = AtomicLong(1)
    private var channelId: String? = null
    private var shipCode: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _shipName = MutableStateFlow<String?>(null)
    val shipName: StateFlow<String?> = _shipName.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // SSE needs no read timeout
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.SERVICE_NOTIFICATION_ID,
            NotificationHelper.createServiceNotification(
                this,
                _shipName.value,
                _connectionState.value == ConnectionState.CONNECTED,
                _unreadCount.value
            )
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        unregisterNetworkCallback()
        scope.cancel()
    }

    fun connect(code: String) {
        shipCode = code
        _connectionState.value = ConnectionState.CONNECTING
        updateNotification()

        scope.launch {
            try {
                if (authenticate(code)) {
                    _connectionState.value = ConnectionState.CONNECTED
                    reconnectDelay = RECONNECT_DELAY_MS
                    updateNotification()
                    openChannel()
                } else {
                    _connectionState.value = ConnectionState.AUTH_FAILED
                    updateNotification()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        }
    }

    fun disconnect() {
        sseJob?.cancel()
        sseJob = null
        channelId = null
        _connectionState.value = ConnectionState.DISCONNECTED
        updateNotification()
    }

    private suspend fun authenticate(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = "password=$code".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder()
                .url("http://127.0.0.1:80/~/login")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    fetchShipName()
                    true
                } else false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auth failed", e)
            false
        }
    }

    private suspend fun fetchShipName() {
        try {
            val request = Request.Builder()
                .url("http://127.0.0.1:80/~/name")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    _shipName.value = response.body?.string()?.trim()?.trim('"')
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ship name", e)
        }
    }

    private fun openChannel() {
        channelId = "launcher-${System.currentTimeMillis()}"
        sseJob = scope.launch {
            subscribeToHark()
            listenToChannel()
        }
    }

    private suspend fun subscribeToHark() {
        val id = channelId ?: return

        val payload = JSONArray().apply {
            put(JSONObject().apply {
                put("id", eventId.getAndIncrement())
                put("action", "subscribe")
                put("ship", _shipName.value?.removePrefix("~") ?: return)
                put("app", "hark-store")
                put("path", "/updates")
            })
        }

        val request = Request.Builder()
            .url("http://127.0.0.1:80/~/channel/$id")
            .put(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to subscribe to hark: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hark subscription failed", e)
        }
    }

    private suspend fun listenToChannel() = withContext(Dispatchers.IO) {
        val id = channelId ?: return@withContext

        val request = Request.Builder()
            .url("http://127.0.0.1:80/~/channel/$id")
            .header("Accept", "text/event-stream")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "SSE connection failed: ${response.code}")
                    scheduleReconnect()
                    return@use
                }

                val reader = response.body?.source()?.inputStream()?.bufferedReader()
                    ?: return@use

                readSseStream(reader)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "SSE error", e)
            scheduleReconnect()
        }
    }

    private suspend fun readSseStream(reader: BufferedReader) {
        var eventData = StringBuilder()

        while (true) {
            val line = reader.readLine() ?: break

            when {
                line.startsWith("data:") -> {
                    eventData.append(line.removePrefix("data:").trim())
                }
                line.isEmpty() && eventData.isNotEmpty() -> {
                    processEvent(eventData.toString())
                    eventData = StringBuilder()
                }
            }
        }
    }

    private fun processEvent(data: String) {
        try {
            val json = JSONObject(data)
            val response = json.optJSONObject("json") ?: return

            when {
                response.has("add-note") -> {
                    val note = response.getJSONObject("add-note")
                    handleNotification(note)
                }
                response.has("unread-count") -> {
                    _unreadCount.value = response.getInt("unread-count")
                    updateNotification()
                }
            }

            // ACK the event
            val id = json.optInt("id", -1)
            if (id >= 0) {
                ackEvent(id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process event: $data", e)
        }
    }

    private fun handleNotification(note: JSONObject) {
        try {
            val bin = note.optJSONObject("bin") ?: return
            val place = bin.optJSONObject("place") ?: return
            val desk = place.optString("desk", "")
            val body = note.optJSONObject("body") ?: return

            val title = when (desk) {
                "groups" -> "Groups"
                "talk" -> "Talk"
                else -> desk.replaceFirstChar { it.uppercase() }
            }

            val content = body.optString("content", "New notification")

            val channelId = when {
                content.contains("mentioned") -> NotificationHelper.MENTION_CHANNEL_ID
                desk == "talk" -> NotificationHelper.DM_CHANNEL_ID
                else -> NotificationHelper.HARK_CHANNEL_ID
            }

            val (notifId, notification) = NotificationHelper.createHarkNotification(
                this,
                title,
                content,
                channelId,
                groupKey = desk,
                notificationId = NotificationHelper.HARK_NOTIFICATION_BASE_ID + (System.currentTimeMillis() % 1000).toInt()
            )

            NotificationHelper.notify(this, notifId, notification)
            _unreadCount.value++
            updateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle notification", e)
        }
    }

    private fun ackEvent(id: Int) {
        val channelId = this.channelId ?: return

        scope.launch {
            try {
                val payload = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", eventId.getAndIncrement())
                        put("action", "ack")
                        put("event-id", id)
                    })
                }

                val request = Request.Builder()
                    .url("http://127.0.0.1:80/~/channel/$channelId")
                    .put(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ack event $id", e)
            }
        }
    }

    private fun scheduleReconnect() {
        if (_connectionState.value == ConnectionState.DISCONNECTED) return

        _connectionState.value = ConnectionState.RECONNECTING
        updateNotification()

        scope.launch {
            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)

            shipCode?.let { connect(it) }
        }
    }

    private fun updateNotification() {
        val notification = NotificationHelper.createServiceNotification(
            this,
            _shipName.value,
            _connectionState.value == ConnectionState.CONNECTED,
            _unreadCount.value
        )
        NotificationHelper.notify(this, NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        persistStateAndUpdateWidget()
    }

    private fun persistStateAndUpdateWidget() {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("ship_name")] = _shipName.value ?: "~"
                prefs[stringPreferencesKey("is_connected")] = (_connectionState.value == ConnectionState.CONNECTED).toString()
                prefs[stringPreferencesKey("unread_count")] = _unreadCount.value.toString()
            }
            UrbitWidget.notifyUpdate(this@UrbitService)
        }
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (_connectionState.value == ConnectionState.DISCONNECTED ||
                    _connectionState.value == ConnectionState.RECONNECTING) {
                    shipCode?.let { connect(it) }
                }
            }

            override fun onLost(network: Network) {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.RECONNECTING
                    updateNotification()
                }
            }
        }

        cm.registerNetworkCallback(request, networkCallback!!)
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            val cm = getSystemService(ConnectivityManager::class.java)
            cm.unregisterNetworkCallback(it)
        }
        networkCallback = null
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        AUTH_FAILED
    }
}
