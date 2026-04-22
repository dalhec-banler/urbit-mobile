package io.nativeplanet.urbit.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nativeplanet.urbit.launcher.theme.LauncherTheme

/** Identity chip: sigil + optional ship name in a pill */
@Composable
fun IdentityChip(
    shipName: String?,
    size: ChipSize = ChipSize.Md,
    showName: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val sigilDp = when (size) {
        ChipSize.Sm -> 18.dp
        ChipSize.Md -> 22.dp
        ChipSize.Lg -> 32.dp
    }

    Row(
        modifier = modifier
            .then(
                if (showName) Modifier
                    .background(tokens.tile, CircleShape)
                    .border(1.dp, tokens.tileBorder, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                else Modifier
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Sigil(
            seed = shipName?.let { seedFromShipName(it) } ?: listOf(2, 4, 9, 1),
            size = sigilDp
        )
        if (showName && shipName != null) {
            Text(
                text = shipName,
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink,
            )
        }
    }
}

enum class ChipSize { Sm, Md, Lg }

/** Small tag/filter chip */
@Composable
fun Chip(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (active) tokens.accent else Color.Transparent)
            .border(1.dp, if (active) tokens.accent else tokens.tileBorder, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = typo.mono.copy(fontSize = 12.sp, letterSpacing = 0.3.sp),
            color = if (active) tokens.sigilFg else tokens.ink,
        )
    }
}

/** Live count badge */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(tokens.accent)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = typo.monoSmall,
            color = tokens.sigilFg,
        )
    }
}

/** Tile frame for agent tiles on the home screen */
@Composable
fun TileFrame(
    active: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = LauncherTheme.tokens
    val shape = RoundedCornerShape(tokens.radius.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.tile)
            .border(
                width = 1.dp,
                color = if (active) tokens.accent else tokens.tileBorder,
                shape = shape
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        content()
    }
}

/** Bottom sheet overlay */
@Composable
fun Sheet(
    visible: Boolean,
    heightFraction: Float = 0.7f,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val tokens = LauncherTheme.tokens
    val shape = RoundedCornerShape(
        topStart = (tokens.radius + 10).dp,
        topEnd = (tokens.radius + 10).dp
    )

    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(heightFraction)
                        .clip(shape)
                        .background(tokens.bg)
                        .clickable(enabled = false) {} // consume click
                ) {
                    content()
                }
            }
        }
    }
}

/** Peer status dot */
@Composable
fun PeerDot(
    status: String,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val color = when (status) {
        "online", "typing" -> tokens.accent
        "away" -> tokens.ink2
        else -> tokens.ink3
    }
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}
