package io.nativeplanet.urbit.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nativeplanet.urbit.launcher.data.AgentInfo
import io.nativeplanet.urbit.launcher.theme.LauncherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockScreen(
    shipName: String?,
    agents: List<AgentInfo>,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val now = Date()
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
    val notifications = agents.filter { it.liveCount > 0 }.take(3)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
            .padding(horizontal = 28.dp)
            .padding(top = 56.dp, bottom = 28.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Swipe up to unlock — threshold in real pixels
                    if (dragAmount < -100) onUnlock()
                }
            }
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = shipName ?: "~",
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink.copy(alpha = 0.7f),
            )
            Text(
                text = "link · home",
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.weight(1f))

        // Center: time + date + sigil
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = time,
                style = typo.headline.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-2).sp,
                    lineHeight = 72.sp,
                ),
                color = tokens.ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = date,
                style = typo.body.copy(fontSize = 13.sp),
                color = tokens.ink.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(20.dp))
            Sigil(
                seed = shipName?.let { seedFromShipName(it) } ?: listOf(2, 4, 9, 1),
                size = 64.dp
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.weight(1f))

        // Bottom: notification previews
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            notifications.forEach { agent ->
                NotificationCard(agent)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Unlock button — tap to unlock
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(100))
                .clickable { onUnlock() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "tap to unlock ↑",
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun NotificationCard(agent: AgentInfo) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val shape = RoundedCornerShape(tokens.radius.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.tile)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Agent initial as placeholder glyph
        Text(
            text = agent.name.take(1).uppercase(),
            style = typo.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = tokens.accent,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = agent.name,
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink,
            )
            if (agent.livePreview.isNotEmpty()) {
                Text(
                    text = agent.livePreview,
                    style = typo.mono.copy(fontSize = 10.sp),
                    color = tokens.ink2,
                    maxLines = 1,
                )
            }
        }
        if (agent.liveCount > 0) {
            CountBadge(agent.liveCount)
        }
    }
}
