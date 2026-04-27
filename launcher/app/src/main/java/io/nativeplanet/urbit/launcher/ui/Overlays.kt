package io.nativeplanet.urbit.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nativeplanet.urbit.launcher.data.AgentInfo
import io.nativeplanet.urbit.launcher.theme.Aesthetic
import io.nativeplanet.urbit.launcher.theme.LauncherTheme
import io.nativeplanet.urbit.launcher.theme.tokensByAesthetic

/** Notification shade overlay */
@Composable
fun NotificationShade(
    agents: List<AgentInfo>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val shadeBg = if (tokens.dark) tokens.bg.copy(alpha = 0.82f) else tokens.bg.copy(alpha = 0.92f)
    val notifications = agents.filter { it.liveCount > 0 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shadeBg)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .clickable(enabled = false) {}
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "notifications",
                    style = typo.title.copy(fontSize = 22.sp),
                    color = tokens.ink,
                )
                Text(
                    text = "clear all",
                    style = typo.mono.copy(fontSize = 11.sp),
                    color = tokens.ink2,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(Modifier.height(14.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ship is quiet",
                        style = typo.mono.copy(fontSize = 11.sp),
                        color = tokens.ink3,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(notifications) { agent ->
                        NotifCard(agent)
                    }
                }
            }

            // Close
            Text(
                text = "↓ close",
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink2,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onDismiss() }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun NotifCard(agent: AgentInfo) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radius.dp))
            .background(tokens.tile)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = agent.name.take(1).uppercase(),
                    style = typo.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = tokens.accent,
                )
                Text(
                    text = agent.name,
                    style = typo.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = tokens.ink,
                )
            }
            Text(
                text = "2m",
                style = typo.mono.copy(fontSize = 10.sp),
                color = tokens.ink3,
            )
        }
        if (agent.livePreview.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = agent.livePreview,
                style = typo.body.copy(fontSize = 12.sp),
                color = tokens.ink2,
                maxLines = 2,
            )
        }
    }
}

/** Quick Settings overlay */
@Composable
fun QuickSettings(
    shipName: String?,
    currentAesthetic: Aesthetic,
    onAestheticChange: (Aesthetic) -> Unit,
    onSettingsTap: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val shadeBg = if (tokens.dark) tokens.bg.copy(alpha = 0.82f) else tokens.bg.copy(alpha = 0.92f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shadeBg)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .clickable(enabled = false) {}
        ) {
            IdentityChip(shipName = shipName, size = ChipSize.Lg, showName = true)

            Spacer(Modifier.height(20.dp))

            // Quick toggles - 2 column grid
            Text(
                text = "toggles",
                style = typo.mono.copy(fontSize = 10.sp),
                color = tokens.ink.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(8.dp))

            val toggles = listOf("link·home" to true, "bluetooth" to true, "location" to false, "do not disturb" to false, "auto-rotate" to false, "airplane" to false)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(160.dp)
            ) {
                items(toggles) { (label, on) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(tokens.radius.dp))
                            .background(if (on) tokens.accent else tokens.tile)
                            .border(1.dp, if (on) tokens.accent else tokens.tileBorder, RoundedCornerShape(tokens.radius.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = label,
                            style = typo.mono.copy(fontSize = 11.sp),
                            color = if (on) tokens.sigilFg else tokens.ink,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Theme picker
            ThemePicker(
                current = currentAesthetic,
                onSelect = onAestheticChange
            )

            Spacer(Modifier.height(20.dp))

            // Settings button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(tokens.radius.dp))
                    .background(tokens.tile)
                    .clickable { onSettingsTap(); onDismiss() }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "settings",
                    style = typo.mono.copy(fontSize = 12.sp),
                    color = tokens.ink,
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "↓ close",
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink2,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onDismiss() }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ThemePicker(
    current: Aesthetic,
    onSelect: (Aesthetic) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radius.dp))
            .background(tokens.tile)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "theme · your phone",
                style = typo.mono.copy(fontSize = 10.sp),
                color = tokens.ink.copy(alpha = 0.55f),
            )
            Text(
                text = current.name.lowercase(),
                style = typo.mono.copy(fontSize = 10.sp),
                color = tokens.ink,
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Aesthetic.entries.forEach { aesthetic ->
                val t = tokensByAesthetic(aesthetic)
                val isActive = aesthetic == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(t.bg)
                        .then(
                            if (isActive) Modifier.border(2.dp, tokens.accent, RoundedCornerShape(6.dp))
                            else Modifier.border(1.dp, tokens.tileBorder, RoundedCornerShape(6.dp))
                        )
                        .clickable { onSelect(aesthetic) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(t.accent)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Aesthetic.entries.forEach { aesthetic ->
                Text(
                    text = aesthetic.name.lowercase(),
                    style = typo.monoSmall,
                    color = if (aesthetic == current) tokens.ink else tokens.ink3,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Command bar overlay */
@Composable
fun CommandBar(
    agents: List<AgentInfo>,
    onAgentTap: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val filteredAgents = if (query.text.isEmpty()) agents
    else agents.filter {
        it.name.contains(query.text, ignoreCase = true) ||
        it.verb.contains(query.text, ignoreCase = true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg.copy(alpha = 0.85f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 60.dp)
                .clip(RoundedCornerShape(tokens.radius.dp))
                .background(tokens.bg)
                .border(1.dp, tokens.tileBorder, RoundedCornerShape(tokens.radius.dp))
                .clickable(enabled = false) {}
                .padding(14.dp)
        ) {
            // Search input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "›",
                    style = typo.mono.copy(fontSize = 13.sp),
                    color = tokens.ink3,
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = typo.mono.copy(fontSize = 13.sp, color = tokens.ink),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.text.isEmpty()) {
                            Text(
                                text = "search agents, verbs, ships…",
                                style = typo.mono.copy(fontSize = 13.sp),
                                color = tokens.ink3,
                            )
                        }
                        inner()
                    }
                )
                Text(
                    text = "esc",
                    style = typo.mono.copy(fontSize = 10.sp),
                    color = tokens.ink3,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Results
            filteredAgents.take(9).forEach { agent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAgentTap(agent.id); onDismiss() }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "agent",
                        style = typo.monoSmall,
                        color = tokens.accent,
                    )
                    Text(
                        text = agent.name,
                        style = typo.body.copy(fontSize = 13.sp),
                        color = tokens.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = agent.verb,
                        style = typo.mono.copy(fontSize = 10.sp),
                        color = tokens.ink3,
                    )
                }
            }
        }
    }
}
