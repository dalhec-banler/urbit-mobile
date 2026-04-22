package io.nativeplanet.urbit.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nativeplanet.urbit.launcher.data.AgentInfo
import io.nativeplanet.urbit.launcher.data.GuestApp
import io.nativeplanet.urbit.launcher.theme.LauncherTheme

enum class DrawerRail { Agents, Guests }

@Composable
fun PeerDrawer(
    agents: List<AgentInfo>,
    guests: List<GuestApp>,
    onAgentTap: (String) -> Unit,
    onGuestTap: (GuestApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    var rail by remember { mutableStateOf(DrawerRail.Agents) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // Title
        Text(
            text = "everyone",
            style = typo.headline.copy(fontSize = 28.sp),
            color = tokens.ink,
        )

        Spacer(Modifier.height(12.dp))

        // Rail toggle
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.ink.copy(alpha = 0.05f))
                .padding(2.dp)
        ) {
            RailTab("agents", rail == DrawerRail.Agents) { rail = DrawerRail.Agents }
            RailTab("guests", rail == DrawerRail.Guests) { rail = DrawerRail.Guests }
        }

        Spacer(Modifier.height(14.dp))

        // Rail label
        Text(
            text = if (rail == DrawerRail.Agents) "agents · of this ship" else "guests · sandboxed",
            style = typo.mono.copy(fontSize = 10.sp),
            color = tokens.ink.copy(alpha = 0.55f),
        )

        Spacer(Modifier.height(10.dp))

        when (rail) {
            DrawerRail.Agents -> AgentsRail(agents, onAgentTap)
            DrawerRail.Guests -> GuestsRail(guests, onGuestTap)
        }
    }
}

@Composable
private fun RailTab(label: String, active: Boolean, onClick: () -> Unit) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) tokens.tile else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = typo.mono.copy(fontSize = 11.sp),
            color = if (active) tokens.ink else tokens.ink2,
        )
    }
}

@Composable
private fun AgentsRail(agents: List<AgentInfo>, onAgentTap: (String) -> Unit) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(agents) { agent ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAgentTap(agent.id) }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(tokens.tile)
                        .border(1.dp, tokens.tileBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = agent.name.take(2).uppercase(),
                        style = typo.mono.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = tokens.ink,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = agent.name.lowercase(),
                    style = typo.mono.copy(fontSize = 9.sp),
                    color = tokens.ink2,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun GuestsRail(
    guests: List<GuestApp>,
    onGuestTap: (GuestApp) -> Unit
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(guests) { guest ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onGuestTap(guest) }
            ) {
                AppIcon(
                    pkg = guest.pkg,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = guest.name,
                    style = typo.mono.copy(fontSize = 9.sp),
                    color = tokens.ink2,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Loads and displays the real app icon from PackageManager.
 */
@Composable
private fun AppIcon(pkg: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(pkg) {
        try {
            val drawable = context.packageManager.getApplicationIcon(pkg)
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = pkg,
            modifier = modifier
        )
    } else {
        // Fallback
        val tokens = LauncherTheme.tokens
        Box(
            modifier = modifier
                .background(tokens.tile, RoundedCornerShape(12.dp))
                .border(1.dp, tokens.tileBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pkg.substringAfterLast(".").take(2).uppercase(),
                style = LauncherTheme.typography.mono.copy(fontSize = 12.sp),
                color = tokens.ink2,
            )
        }
    }
}
