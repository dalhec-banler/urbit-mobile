package io.nativeplanet.urbit.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.nativeplanet.urbit.launcher.service.UrbitService
import io.nativeplanet.urbit.launcher.theme.Aesthetic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

class LauncherViewModel : ViewModel() {

    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    val connection = UrbitConnection()

    private var urbitService: UrbitService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? UrbitService.LocalBinder
            urbitService = localBinder?.service
            serviceBound = true

            viewModelScope.launch {
                urbitService?.connectionState?.collect { serviceState ->
                    val status = when (serviceState) {
                        UrbitService.ConnectionState.CONNECTED -> ConnectionStatus.CONNECTED
                        UrbitService.ConnectionState.CONNECTING -> ConnectionStatus.CONNECTING
                        UrbitService.ConnectionState.RECONNECTING -> ConnectionStatus.RECONNECTING
                        else -> ConnectionStatus.DISCONNECTED
                    }
                    _state.update { it.copy(
                        isConnected = serviceState == UrbitService.ConnectionState.CONNECTED,
                        connectionStatus = status
                    )}
                }
            }

            viewModelScope.launch {
                urbitService?.shipName?.collect { name ->
                    _state.update { it.copy(shipName = name) }
                }
            }

            viewModelScope.launch {
                urbitService?.unreadCount?.collect { count ->
                    _state.update { it.copy(
                        agents = it.agents.map { agent ->
                            if (agent.desk == "groups") agent.copy(liveCount = count)
                            else agent
                        }
                    )}
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            urbitService = null
            serviceBound = false
        }
    }

    fun bindService(context: Context) {
        if (!serviceBound) {
            val intent = Intent(context, UrbitService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService(context: Context) {
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
            urbitService = null
        }
    }

    fun loadPreferences(context: Context) {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val aestheticName = prefs[stringPreferencesKey("aesthetic")]
            val homeStyleName = prefs[stringPreferencesKey("home_style")]
            val savedCode = prefs[stringPreferencesKey("ship_code")]

            val bgNotifs = prefs[stringPreferencesKey("settings_bg_notifications")]?.toBoolean() ?: true
            val notifyDMs = prefs[stringPreferencesKey("settings_notify_dms")]?.toBoolean() ?: true
            val notifyMentions = prefs[stringPreferencesKey("settings_notify_mentions")]?.toBoolean() ?: true
            val notifyOther = prefs[stringPreferencesKey("settings_notify_other")]?.toBoolean() ?: true
            val autoStart = prefs[stringPreferencesKey("settings_auto_start")]?.toBoolean() ?: true

            _state.update { s ->
                s.copy(
                    aesthetic = aestheticName?.let {
                        try { Aesthetic.valueOf(it) } catch (_: Exception) { null }
                    } ?: s.aesthetic,
                    homeStyle = homeStyleName?.let {
                        try { HomeStyle.valueOf(it) } catch (_: Exception) { null }
                    } ?: s.homeStyle,
                    shipCode = savedCode,
                    serviceSettings = ServiceSettings(
                        backgroundNotificationsEnabled = bgNotifs,
                        notifyDMs = notifyDMs,
                        notifyMentions = notifyMentions,
                        notifyOther = notifyOther,
                        autoStartOnBoot = autoStart
                    )
                )
            }

            // Auto-connect: try loopback first (seamless), fall back to saved code
            autoConnect(context)

            // Load real installed apps as guests
            loadInstalledApps(context)
        }
    }

    /**
     * Query all launchable apps from PackageManager and populate the guest list.
     * Excludes our own launcher and system framework packages.
     */
    private fun loadInstalledApps(context: Context) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(intent, 0)
        val ourPackage = context.packageName

        // Packages that correspond to ship agents — shown under Agents, not Guests
        val agentPackages = setOf("io.tlon.groups")

        val apps = activities
            .filter { it.activityInfo.packageName != ourPackage }
            .filter { it.activityInfo.packageName !in agentPackages }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                GuestApp(
                    id = pkg,
                    name = label,
                    kind = "guest",
                    pkg = pkg
                )
            }
            .sortedBy { it.name.lowercase() }

        _state.update { it.copy(guests = apps) }
    }

    /**
     * Auto-connect to the ship using saved +code.
     * Starts the background service and authenticates via Eyre.
     */
    private fun autoConnect(context: Context) {
        viewModelScope.launch {
            val savedCode = _state.value.shipCode
            if (savedCode != null) {
                // Start the background service
                UrbitService.start(context)
                bindService(context)

                val authed = connection.authenticate(savedCode)
                if (authed) {
                    val name = connection.getShipName()
                    _state.update { it.copy(isConnected = true, shipName = name) }
                    loadShipAgents()

                    // Connect the service too
                    urbitService?.connect(savedCode)
                    return@launch
                }
            }
            // No saved code or auth failed — stay on setup screen
        }
    }

    /**
     * Fetch installed desks from the ship's docket and populate the agents list.
     * Excludes system desks (landscape) since they aren't user-facing apps.
     */
    // Standin tiles for apps that aren't installed yet but will be
    private val standins = listOf(
        AgentInfo("grove", "Grove", "share", "File sharing & storage", desk = "grove"),
        AgentInfo("time", "Time", "plan", "Calendar & scheduling", desk = "time"),
    )

    private fun loadShipAgents() {
        viewModelScope.launch {
            val dockets = connection.getDockets()
            val shipAgents = dockets
                .filter { it.key != "landscape" }
                .map { (desk, info) ->
                    AgentInfo(
                        id = desk,
                        name = info.title,
                        verb = desk,
                        desc = info.info,
                        desk = desk,
                    )
                }
            // Merge: ship agents first, then standins for any not already on ship
            val shipDesks = shipAgents.map { it.desk }.toSet()
            val combined = shipAgents + standins.filter { it.desk !in shipDesks }
            _state.update { it.copy(agents = combined.sortedBy { a -> a.name.lowercase() }) }
        }
    }

    fun savePreferences(context: Context) {
        viewModelScope.launch {
            val current = _state.value
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey("aesthetic")] = current.aesthetic.name
                prefs[stringPreferencesKey("home_style")] = current.homeStyle.name
                current.shipCode?.let { prefs[stringPreferencesKey("ship_code")] = it }
            }
        }
    }

    fun connectToShip(code: String, context: Context? = null) {
        viewModelScope.launch {
            _state.update { it.copy(shipCode = code) }
            val authed = connection.authenticate(code)
            if (authed) {
                val name = connection.getShipName()
                _state.update { it.copy(isConnected = true, shipName = name) }
                loadShipAgents()

                // Start and connect to the background service
                context?.let { ctx ->
                    UrbitService.start(ctx)
                    bindService(ctx)
                    urbitService?.connect(code)
                }
            } else {
                _state.update { it.copy(isConnected = false) }
            }
        }
    }

    fun navigateTo(surface: Surface) {
        _state.update { it.copy(surface = surface, overlay = Overlay.None) }
    }

    fun showOverlay(overlay: Overlay) {
        _state.update { it.copy(overlay = overlay) }
    }

    fun dismissOverlay() {
        _state.update { it.copy(overlay = Overlay.None) }
    }

    fun reorderAgents(agents: List<AgentInfo>) {
        _state.update { it.copy(agents = agents) }
    }

    fun setAesthetic(aesthetic: Aesthetic) {
        _state.update { it.copy(aesthetic = aesthetic) }
    }

    fun setHomeStyle(style: HomeStyle) {
        _state.update { it.copy(homeStyle = style) }
    }

    fun selectAgent(agentId: String) {
        _state.update { it.copy(selectedAgent = agentId, surface = Surface.Agent) }
    }

    fun selectAgentByDesk(desk: String) {
        val agent = _state.value.agents.find { it.desk == desk }
        if (agent != null) {
            selectAgent(agent.id)
        }
    }

    fun selectGuest(guestId: String) {
        _state.update { it.copy(selectedGuest = guestId, surface = Surface.GuestApp) }
    }

    fun unlock() {
        _state.update { it.copy(surface = Surface.Home) }
    }

    fun lock() {
        _state.update { it.copy(surface = Surface.Lock, overlay = Overlay.None) }
    }

    fun updateSettings(settings: ServiceSettings, context: Context) {
        _state.update { it.copy(serviceSettings = settings) }
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey("settings_bg_notifications")] = settings.backgroundNotificationsEnabled.toString()
                prefs[stringPreferencesKey("settings_notify_dms")] = settings.notifyDMs.toString()
                prefs[stringPreferencesKey("settings_notify_mentions")] = settings.notifyMentions.toString()
                prefs[stringPreferencesKey("settings_notify_other")] = settings.notifyOther.toString()
                prefs[stringPreferencesKey("settings_auto_start")] = settings.autoStartOnBoot.toString()
            }
        }
    }

    fun disconnect(context: Context) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey("ship_code"))
            }
            UrbitService.stop(context)
            _state.update { it.copy(
                isConnected = false,
                shipName = null,
                shipCode = null,
                surface = Surface.Lock
            )}
        }
    }
}
