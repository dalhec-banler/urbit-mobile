package io.nativeplanet.urbit.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nativeplanet.urbit.launcher.data.ServiceSettings
import io.nativeplanet.urbit.launcher.theme.LauncherTheme

@Composable
fun SettingsScreen(
    settings: ServiceSettings,
    onSettingsChange: (ServiceSettings) -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit
) {
    val tokens = LauncherTheme.tokens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.bg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.ink,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Done",
                fontSize = 16.sp,
                color = tokens.accent,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Notifications")
            }

            item {
                SettingsToggle(
                    title = "Background Notifications",
                    subtitle = "Receive notifications when the app is closed",
                    checked = settings.backgroundNotificationsEnabled,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(backgroundNotificationsEnabled = it))
                    }
                )
            }

            item {
                SettingsToggle(
                    title = "Direct Messages",
                    subtitle = "Notify when you receive DMs",
                    checked = settings.notifyDMs,
                    enabled = settings.backgroundNotificationsEnabled,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(notifyDMs = it))
                    }
                )
            }

            item {
                SettingsToggle(
                    title = "Mentions",
                    subtitle = "Notify when someone mentions you",
                    checked = settings.notifyMentions,
                    enabled = settings.backgroundNotificationsEnabled,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(notifyMentions = it))
                    }
                )
            }

            item {
                SettingsToggle(
                    title = "Other Notifications",
                    subtitle = "Group activity and other updates",
                    checked = settings.notifyOther,
                    enabled = settings.backgroundNotificationsEnabled,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(notifyOther = it))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSection(title = "System")
            }

            item {
                SettingsToggle(
                    title = "Auto-Start on Boot",
                    subtitle = "Start background service when device boots",
                    checked = settings.autoStartOnBoot,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(autoStartOnBoot = it))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                SettingsButton(
                    title = "Disconnect Ship",
                    destructive = true,
                    onClick = onDisconnect
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    val tokens = LauncherTheme.tokens
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = tokens.ink2,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LauncherTheme.tokens
    val alpha = if (enabled) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.tile)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = tokens.ink.copy(alpha = alpha)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = tokens.ink2.copy(alpha = alpha)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.accent,
                checkedTrackColor = tokens.accent.copy(alpha = 0.5f),
                uncheckedThumbColor = tokens.ink3,
                uncheckedTrackColor = tokens.ink3.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsButton(
    title: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val tokens = LauncherTheme.tokens
    val errorColor = androidx.compose.ui.graphics.Color(0xFFEF5350)
    val color = if (destructive) errorColor else tokens.accent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
