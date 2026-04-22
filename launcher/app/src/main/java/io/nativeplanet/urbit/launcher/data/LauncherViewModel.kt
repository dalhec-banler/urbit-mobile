package io.nativeplanet.urbit.launcher.data

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun loadPreferences(context: Context) {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val aestheticName = prefs[stringPreferencesKey("aesthetic")]
            val homeStyleName = prefs[stringPreferencesKey("home_style")]
            val savedCode = prefs[stringPreferencesKey("ship_code")]

            _state.update { s ->
                s.copy(
                    aesthetic = aestheticName?.let {
                        try { Aesthetic.valueOf(it) } catch (_: Exception) { null }
                    } ?: s.aesthetic,
                    homeStyle = homeStyleName?.let {
                        try { HomeStyle.valueOf(it) } catch (_: Exception) { null }
                    } ?: s.homeStyle,
                    shipCode = savedCode
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
     * Authenticates directly via Eyre — no lens API needed.
     */
    private fun autoConnect(context: Context) {
        viewModelScope.launch {
            val savedCode = _state.value.shipCode
            if (savedCode != null) {
                val authed = connection.authenticate(savedCode)
                if (authed) {
                    val name = connection.getShipName()
                    _state.update { it.copy(isConnected = true, shipName = name) }
                    loadShipAgents()
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

    fun connectToShip(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(shipCode = code) }
            val authed = connection.authenticate(code)
            if (authed) {
                val name = connection.getShipName()
                _state.update { it.copy(isConnected = true, shipName = name) }
                loadShipAgents()
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

    fun selectGuest(guestId: String) {
        _state.update { it.copy(selectedGuest = guestId, surface = Surface.GuestApp) }
    }

    fun unlock() {
        _state.update { it.copy(surface = Surface.Home) }
    }

    fun lock() {
        _state.update { it.copy(surface = Surface.Lock, overlay = Overlay.None) }
    }
}
