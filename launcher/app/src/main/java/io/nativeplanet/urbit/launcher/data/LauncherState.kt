package io.nativeplanet.urbit.launcher.data

import io.nativeplanet.urbit.launcher.theme.Aesthetic

data class AgentInfo(
    val id: String,
    val name: String,
    val verb: String,
    val desc: String,
    val desk: String = id,  // actual desk name on the ship (for WebView URL)
    val liveCount: Int = 0,
    val livePreview: String = "",
)

data class GuestEssential(
    val id: String,
    val verb: String,
    val desc: String,
    val pkg: String,
)

data class GuestApp(
    val id: String,
    val name: String,
    val kind: String,
    val running: Boolean = false,
    val unread: Int = 0,
    val preview: String = "",
    val pkg: String,
)

enum class Surface {
    Lock, Home, Drawer, Agent, Tasks, GuestApp
}

enum class Overlay {
    None, Notifications, Quick, Command, Share, Intent, Channel
}

enum class HomeStyle {
    Tiles, Quiet
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class LauncherState(
    val surface: Surface = Surface.Lock,
    val overlay: Overlay = Overlay.None,
    val aesthetic: Aesthetic = Aesthetic.Softbit,
    val homeStyle: HomeStyle = HomeStyle.Tiles,
    val shipName: String? = null,
    val shipCode: String? = null,
    val isConnected: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val agents: List<AgentInfo> = defaultAgents,
    val essentials: List<GuestEssential> = defaultEssentials,
    val guests: List<GuestApp> = defaultGuests,
    val selectedAgent: String? = null,
    val selectedGuest: String? = null,
)

// Agents are populated dynamically from the ship's docket — see LauncherViewModel.loadShipAgents()
val defaultAgents = emptyList<AgentInfo>()

val defaultEssentials = listOf(
    GuestEssential("phone", "call", "Phone dialer", "com.android.dialer"),
    GuestEssential("sms", "text", "Messages", "com.android.messaging"),
    GuestEssential("camera", "capture", "Camera", "app.grapheneos.camera"),
    GuestEssential("browser", "browse", "Vanadium", "app.vanadium.browser"),
)

// Guests are populated dynamically from installed apps — see LauncherViewModel.loadInstalledApps()
val defaultGuests = emptyList<GuestApp>()
