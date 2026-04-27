package io.nativeplanet.urbit.launcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nativeplanet.urbit.launcher.data.HomeStyle
import io.nativeplanet.urbit.launcher.data.LauncherViewModel
import io.nativeplanet.urbit.launcher.data.Overlay
import io.nativeplanet.urbit.launcher.data.Surface
import io.nativeplanet.urbit.launcher.guest.GuestLauncher
import io.nativeplanet.urbit.launcher.service.NotificationHelper
import io.nativeplanet.urbit.launcher.theme.LauncherTheme
import io.nativeplanet.urbit.launcher.theme.tokensByAesthetic
import io.nativeplanet.urbit.launcher.ui.*

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channels early
        NotificationHelper.createChannels(this)

        // Request notification permission on Android 13+
        requestNotificationPermission()

        // Show over the system lock screen and auto-dismiss keyguard
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContent {
            val vm: LauncherViewModel = viewModel()
            val state by vm.state.collectAsState()
            val context = LocalContext.current

            // Bind to service when composable enters, unbind when it leaves
            DisposableEffect(Unit) {
                vm.bindService(context)
                onDispose {
                    vm.unbindService(context)
                }
            }

            LaunchedEffect(Unit) {
                vm.loadPreferences(context)
            }

            LaunchedEffect(state.aesthetic, state.homeStyle) {
                vm.savePreferences(context)
            }

            // Set system status bar icons: dark on light themes, light on dark themes
            val darkTheme = tokensByAesthetic(state.aesthetic).dark
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }

            LauncherTheme(aesthetic = state.aesthetic) {
                LauncherShell(vm)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun LauncherShell(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val tokens = LauncherTheme.tokens
    val context = LocalContext.current

    // A launcher must never finish — swallow all back presses.
    BackHandler(enabled = true) {
        when {
            state.overlay != Overlay.None -> vm.dismissOverlay()
            state.surface == Surface.Agent -> vm.navigateTo(Surface.Home)
            state.surface == Surface.Drawer -> vm.navigateTo(Surface.Home)
            state.surface == Surface.GuestApp -> vm.navigateTo(Surface.Home)
            state.surface == Surface.Tasks -> vm.navigateTo(Surface.Home)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.bg)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main surface with slide animation for drawer
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = state.surface,
                    transitionSpec = {
                        if (targetState == Surface.Drawer) {
                            // Drawer slides up from bottom
                            slideInVertically { it } togetherWith slideOutVertically { -it / 4 }
                        } else if (initialState == Surface.Drawer) {
                            // Home slides back, drawer slides down
                            slideInVertically { -it / 4 } togetherWith slideOutVertically { it }
                        } else {
                            // Default: no animation for other transitions
                            slideInVertically { 0 } togetherWith slideOutVertically { 0 }
                        }
                    },
                    label = "surface"
                ) { surface ->
                    when {
                        !state.isConnected -> {
                            SetupScreen(
                                onConnect = { code ->
                                    vm.connectToShip(code, context)
                                    vm.savePreferences(context)
                                },
                                onAutoConnect = {
                                    vm.loadPreferences(context)
                                }
                            )
                        }

                        surface == Surface.Lock -> {
                            LockScreen(
                                shipName = state.shipName,
                                agents = state.agents,
                                onUnlock = { vm.unlock() }
                            )
                        }

                        surface == Surface.Home -> {
                            val handleAgentTap: (String) -> Unit = { agentId ->
                                val agent = state.agents.find { it.id == agentId }
                                if (agent != null && agent.desk == "groups") {
                                    // Launch Tlon native app (from Play Store)
                                    // Falls back to browser if not installed
                                    val launched = GuestLauncher.launchPackage(context, "io.tlon.groups")
                                    if (!launched) {
                                        GuestLauncher.launchBrowser(
                                            context,
                                            "${vm.connection.baseUrl}/apps/${agent.desk}"
                                        )
                                    }
                                } else {
                                    vm.selectAgent(agentId)
                                }
                            }
                            when (state.homeStyle) {
                                HomeStyle.Tiles -> VesselHome(
                                    shipName = state.shipName,
                                    agents = state.agents,
                                    onAgentTap = handleAgentTap,
                                    onReorderAgents = { vm.reorderAgents(it) },
                                    onSwipeUp = { vm.navigateTo(Surface.Drawer) },
                                    onSettingsTap = { vm.showOverlay(Overlay.Quick) },
                                    onCallTap = { GuestLauncher.launchDialer(context) },
                                    onBrowseTap = { GuestLauncher.launchBrowser(context) },
                                    onCaptureTap = { GuestLauncher.launchCamera(context) }
                                )
                                HomeStyle.Quiet -> VesselQuiet(
                                    shipName = state.shipName,
                                    agents = state.agents,
                                    onAgentTap = handleAgentTap,
                                    onSwipeUp = { vm.navigateTo(Surface.Drawer) },
                                    onSettingsTap = { vm.showOverlay(Overlay.Quick) },
                                    onCallTap = { GuestLauncher.launchDialer(context) },
                                    onBrowseTap = { GuestLauncher.launchBrowser(context) },
                                    onCaptureTap = { GuestLauncher.launchCamera(context) }
                                )
                            }
                        }

                        surface == Surface.Drawer -> {
                            PeerDrawer(
                                agents = state.agents,
                                guests = state.guests,
                                onAgentTap = { vm.selectAgent(it) },
                                onGuestTap = { guest ->
                                    GuestLauncher.launchPackage(context, guest.pkg)
                                }
                            )
                        }

                        surface == Surface.Agent -> {
                            val agent = state.agents.find { it.id == state.selectedAgent }
                            if (agent != null) {
                                AgentScreen(
                                    agent = agent,
                                    shipUrl = vm.connection.baseUrl,
                                    shipCode = state.shipCode,
                                    onBack = { vm.navigateTo(Surface.Home) }
                                )
                            }
                        }

                        surface == Surface.GuestApp -> {
                            vm.navigateTo(Surface.Home)
                        }

                        surface == Surface.Tasks -> {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            // No visible dock — swipe up is handled on the home screen itself
        }

        // Overlays
        when (state.overlay) {
            Overlay.Notifications -> NotificationShade(
                agents = state.agents,
                onDismiss = { vm.dismissOverlay() }
            )
            Overlay.Quick -> QuickSettings(
                shipName = state.shipName,
                currentAesthetic = state.aesthetic,
                onAestheticChange = { vm.setAesthetic(it); vm.savePreferences(context) },
                onDismiss = { vm.dismissOverlay() }
            )
            Overlay.Command -> CommandBar(
                agents = state.agents,
                onAgentTap = { vm.selectAgent(it) },
                onDismiss = { vm.dismissOverlay() }
            )
            else -> {}
        }
    }
}
