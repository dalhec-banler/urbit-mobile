package io.nativeplanet.urbit.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.nativeplanet.urbit.launcher.data.AgentInfo
import io.nativeplanet.urbit.launcher.data.ConnectionStatus
import io.nativeplanet.urbit.launcher.theme.LauncherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * VesselHome — 2-column grid of live agent tiles.
 * Matches the mockup: date·time header, greeting, sigil, 2×3 grid,
 * secondary chips, and essential buttons at bottom.
 */
@Composable
fun VesselHome(
    shipName: String?,
    agents: List<AgentInfo>,
    connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    onAgentTap: (String) -> Unit,
    onReorderAgents: (List<AgentInfo>) -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSettingsTap: () -> Unit = {},
    onCallTap: () -> Unit = {},
    onBrowseTap: () -> Unit = {},
    onCaptureTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val now = Date()
    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(now).lowercase()
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
    val greeting = greetingForHour(SimpleDateFormat("H", Locale.getDefault()).format(now).toIntOrNull() ?: 12)
    val primary = agents.take(6)
    val secondary = agents.drop(6)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -80) onSwipeUp()
                }
            }
    ) {
        // Clock + sigil row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = time,
                style = typo.headline.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 48.sp,
                ),
                color = tokens.ink,
            )
            Sigil(
                seed = shipName?.let { seedFromShipName(it) } ?: listOf(2, 4, 9, 1),
                size = 22.dp,
                modifier = Modifier.clickable { onSettingsTap() }
            )
        }

        Spacer(Modifier.height(2.dp))

        // Day + greeting + connection status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Connection indicator dot
            val indicatorColor = when (connectionStatus) {
                ConnectionStatus.CONNECTED -> Color(0xFF4CAF50) // Green
                ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> Color(0xFFFFA726) // Orange
                ConnectionStatus.DISCONNECTED -> Color(0xFFEF5350) // Red
            }
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = indicatorColor)
            }

            Text(
                text = "$dayName · $greeting",
                style = typo.mono.copy(fontSize = 10.sp),
                color = tokens.ink.copy(alpha = 0.55f),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Primary tiles — 2×3 draggable grid
        DraggableAgentGrid(
            agents = primary,
            allAgents = agents,
            onAgentTap = onAgentTap,
            onReorder = onReorderAgents,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.height(10.dp))

        // Secondary agents — horizontal scroll chips with agent name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            secondary.forEach { agent ->
                SecondaryChip(
                    agent = agent,
                    onClick = { onAgentTap(agent.id) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Essential buttons — call, browse, capture (only addition beyond mockup)
        EssentialRow(
            inkColor = tokens.ink,
            onCallTap = onCallTap,
            onBrowseTap = onBrowseTap,
            onCaptureTap = onCaptureTap
        )
    }
}

/**
 * Draggable 2-column grid — long press to pick up, drag to reorder.
 */
@Composable
private fun DraggableAgentGrid(
    agents: List<AgentInfo>,
    allAgents: List<AgentInfo>,
    onAgentTap: (String) -> Unit,
    onReorder: (List<AgentInfo>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Index of the tile being dragged (-1 = none)
    var draggingIndex by remember { mutableIntStateOf(-1) }
    // Current drag offset in pixels
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // Tile dimensions for hit-testing
    var tileWidth by remember { mutableIntStateOf(0) }
    var tileHeight by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (rowIdx in 0 until (agents.size + 1) / 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (colIdx in 0..1) {
                    val idx = rowIdx * 2 + colIdx
                    if (idx < agents.size) {
                        val agent = agents[idx]
                        val isDragging = draggingIndex == idx

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    tileWidth = coords.size.width
                                    tileHeight = coords.size.height
                                }
                                .zIndex(if (isDragging) 10f else 0f)
                                .offset {
                                    if (isDragging) IntOffset(
                                        dragOffset.x.roundToInt(),
                                        dragOffset.y.roundToInt()
                                    ) else IntOffset.Zero
                                }
                                .graphicsLayer {
                                    if (isDragging) {
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                        shadowElevation = 8f
                                    }
                                }
                                .pointerInput(agents) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = idx
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                        },
                                        onDragEnd = {
                                            // Calculate target index from drag offset
                                            if (tileWidth > 0 && tileHeight > 0) {
                                                val colShift = (dragOffset.x / tileWidth).roundToInt()
                                                val rowShift = (dragOffset.y / tileHeight).roundToInt()
                                                val fromIdx = draggingIndex
                                                val fromRow = fromIdx / 2
                                                val fromCol = fromIdx % 2
                                                val toCol = (fromCol + colShift).coerceIn(0, 1)
                                                val toRow = (fromRow + rowShift).coerceIn(0, (agents.size - 1) / 2)
                                                val toIdx = (toRow * 2 + toCol).coerceIn(0, agents.size - 1)

                                                if (toIdx != fromIdx) {
                                                    val reordered = allAgents.toMutableList()
                                                    val item = reordered.removeAt(fromIdx)
                                                    reordered.add(toIdx, item)
                                                    onReorder(reordered)
                                                }
                                            }
                                            draggingIndex = -1
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggingIndex = -1
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                        ) {
                            AgentTile(agent = agent, onClick = { onAgentTap(agent.id) })
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * AgentTile — matches mockup: icon + name top left, badge top right,
 * preview text middle, verb bottom.
 */
@Composable
private fun AgentTile(
    agent: AgentInfo,
    onClick: () -> Unit
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    TileFrame(
        active = agent.liveCount > 0,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 90.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Icon + name + badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawAgentIcon(agent.id, tokens.accent)
                        }
                        Text(
                            text = agent.name,
                            style = typo.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            color = tokens.ink,
                        )
                    }
                    if (agent.liveCount > 0) {
                        CountBadge(agent.liveCount)
                    }
                }

                // Preview text
                if (agent.livePreview.isNotEmpty()) {
                    Text(
                        text = agent.livePreview,
                        style = typo.mono.copy(fontSize = 10.sp),
                        color = tokens.ink2,
                        maxLines = 2,
                    )
                }
            }

            // Verb — anchored to bottom
            Text(
                text = agent.verb,
                style = typo.mono.copy(fontSize = 10.sp),
                color = tokens.ink3,
            )
        }
    }
}

/**
 * Secondary chip — matches mockup: small icon + agent name + optional count.
 */
@Composable
private fun SecondaryChip(
    agent: AgentInfo,
    onClick: () -> Unit
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val label = if (agent.liveCount > 0) "${agent.name} · ${agent.liveCount}" else agent.name

    Chip(
        label = label,
        onClick = onClick
    )
}

/**
 * VesselQuiet — Light-Phone-style verb column.
 */
@Composable
fun VesselQuiet(
    shipName: String?,
    agents: List<AgentInfo>,
    connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    onAgentTap: (String) -> Unit,
    onSwipeUp: () -> Unit = {},
    onSettingsTap: () -> Unit = {},
    onCallTap: () -> Unit = {},
    onBrowseTap: () -> Unit = {},
    onCaptureTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val now = Date()
    val dateLine = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now).uppercase()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 28.dp, bottom = 16.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -80) onSwipeUp()
                }
            }
    ) {
        // Date label with connection indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val indicatorColor = when (connectionStatus) {
                ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> Color(0xFFFFA726)
                ConnectionStatus.DISCONNECTED -> Color(0xFFEF5350)
            }
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = indicatorColor)
            }
            Text(
                text = dateLine,
                style = typo.mono.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                ),
                color = tokens.ink.copy(alpha = 0.55f),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Headline
        Text(
            text = "today",
            style = typo.title.copy(fontSize = 22.sp),
            color = tokens.ink,
        )
        Text(
            text = "ship is quiet · no alarms",
            style = typo.body.copy(fontSize = 13.sp),
            color = tokens.ink2,
        )

        Spacer(Modifier.weight(1f))

        // Verb list
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            agents.forEach { agent ->
                VerbRow(
                    verb = agent.verb,
                    agentName = agent.name.lowercase(),
                    count = agent.liveCount,
                    onClick = { onAgentTap(agent.id) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Essential buttons
        EssentialRow(
            inkColor = tokens.ink,
            onCallTap = onCallTap,
            onBrowseTap = onBrowseTap,
            onCaptureTap = onCaptureTap
        )

        Spacer(Modifier.height(8.dp))

        // Footer
        Text(
            text = "ship is quiet · ${shipName ?: "~"}",
            style = typo.mono.copy(fontSize = 10.sp),
            color = tokens.ink.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onSettingsTap() }
        )
    }
}

/**
 * Essential row — call, browse, capture at bottom of home screen.
 */
@Composable
private fun EssentialRow(
    inkColor: Color,
    onCallTap: () -> Unit,
    onBrowseTap: () -> Unit,
    onCaptureTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = inkColor.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EssentialButton("call", iconColor) { onCallTap() }
        EssentialButton("browse", iconColor) { onBrowseTap() }
        EssentialButton("capture", iconColor) { onCaptureTap() }
    }
}

@Composable
private fun EssentialButton(
    label: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    val typo = LauncherTheme.typography

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            when (label) {
                "call" -> drawPhoneIcon(iconColor)
                "browse" -> drawGlobeIcon(iconColor)
                "capture" -> drawCameraIcon(iconColor)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = typo.mono.copy(fontSize = 9.sp),
            color = iconColor.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun VerbRow(
    verb: String,
    agentName: String,
    count: Int,
    onClick: () -> Unit
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = verb,
                style = typo.title.copy(fontSize = 26.sp),
                color = tokens.ink,
            )
            if (count > 0) {
                CountBadge(count)
            }
        }
        Text(
            text = agentName,
            style = typo.mono.copy(fontSize = 10.sp),
            color = tokens.ink.copy(alpha = 0.45f),
        )
    }
}

private fun greetingForHour(hour: Int): String = when {
    hour < 12 -> "good morning"
    hour < 17 -> "good afternoon"
    else -> "good evening"
}

// --- Agent tile icons ---

private fun DrawScope.drawAgentIcon(agentId: String, color: Color) {
    when (agentId) {
        "groups", "tlon" -> drawChatIcon(color)
        "grove" -> drawShareIcon(color)
        "kin" -> drawHeartIcon(color)
        "notes" -> drawDocIcon(color)
        "photos" -> drawImageIcon(color)
        "wallet" -> drawWalletIcon(color)
        "feed" -> drawFeedIcon(color)
        "radio" -> drawRadioIcon(color)
        "mail" -> drawMailIcon(color)
        "calendar", "time" -> drawCalendarIcon(color)
        "vault" -> drawLockIcon(color)
        "webterm" -> drawDocIcon(color)  // terminal uses doc icon
        else -> drawDefaultIcon(color)
    }
}

private fun DrawScope.drawChatIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    val path = Path().apply {
        moveTo(w * 0.15f, h * 0.2f)
        lineTo(w * 0.85f, h * 0.2f)
        lineTo(w * 0.85f, h * 0.65f)
        lineTo(w * 0.45f, h * 0.65f)
        lineTo(w * 0.25f, h * 0.85f)
        lineTo(w * 0.25f, h * 0.65f)
        lineTo(w * 0.15f, h * 0.65f)
        close()
    }
    drawPath(path, color, style = s)
}

private fun DrawScope.drawShareIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    drawLine(color, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.65f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.3f, h * 0.45f), Offset(w * 0.5f, h * 0.65f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.7f, h * 0.45f), Offset(w * 0.5f, h * 0.65f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    val tray = Path().apply {
        moveTo(w * 0.15f, h * 0.6f)
        lineTo(w * 0.15f, h * 0.85f)
        lineTo(w * 0.85f, h * 0.85f)
        lineTo(w * 0.85f, h * 0.6f)
    }
    drawPath(tray, color, style = s)
}

private fun DrawScope.drawHeartIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.85f)
        cubicTo(w * 0.1f, h * 0.55f, w * 0.1f, h * 0.2f, w * 0.5f, h * 0.35f)
        cubicTo(w * 0.9f, h * 0.2f, w * 0.9f, h * 0.55f, w * 0.5f, h * 0.85f)
        close()
    }
    drawPath(path, color, style = s)
}

private fun DrawScope.drawDocIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    drawRoundRect(color, Offset(w * 0.15f, h * 0.1f), Size(w * 0.7f, h * 0.8f), CornerRadius(1.5f), style = s)
    drawLine(color, Offset(w * 0.3f, h * 0.35f), Offset(w * 0.7f, h * 0.35f), strokeWidth = 1.2f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.7f, h * 0.5f), strokeWidth = 1.2f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.3f, h * 0.65f), Offset(w * 0.55f, h * 0.65f), strokeWidth = 1.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawImageIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    drawRoundRect(color, Offset(w * 0.1f, h * 0.15f), Size(w * 0.8f, h * 0.7f), CornerRadius(1.5f), style = s)
    drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.32f, h * 0.38f), style = s)
    val mountain = Path().apply {
        moveTo(w * 0.1f, h * 0.72f)
        lineTo(w * 0.35f, h * 0.5f)
        lineTo(w * 0.55f, h * 0.65f)
        lineTo(w * 0.72f, h * 0.48f)
        lineTo(w * 0.9f, h * 0.72f)
    }
    drawPath(mountain, color, style = s)
}

private fun DrawScope.drawWalletIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    drawRoundRect(color, Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.6f), CornerRadius(2f), style = s)
    drawLine(color, Offset(w * 0.1f, h * 0.42f), Offset(w * 0.9f, h * 0.42f), strokeWidth = 1.2f)
    drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.75f, h * 0.6f), style = s)
}

private fun DrawScope.drawFeedIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round)
    val w = size.width; val h = size.height
    drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.25f, h * 0.78f))
    drawArc(color, startAngle = 180f, sweepAngle = 90f, useCenter = false,
        topLeft = Offset(w * 0.12f, h * 0.35f), size = Size(w * 0.5f, h * 0.5f), style = s)
    drawArc(color, startAngle = 180f, sweepAngle = 90f, useCenter = false,
        topLeft = Offset(w * 0.12f, h * 0.1f), size = Size(w * 0.75f, h * 0.75f), style = s)
}

private fun DrawScope.drawRadioIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round)
    val w = size.width; val h = size.height
    val cx = w * 0.5f; val cy = h * 0.5f
    drawCircle(color, radius = w * 0.07f, center = Offset(cx, cy))
    drawArc(color, startAngle = 220f, sweepAngle = 100f, useCenter = false,
        topLeft = Offset(cx - w * 0.22f, cy - h * 0.22f), size = Size(w * 0.44f, h * 0.44f), style = s)
    drawArc(color, startAngle = 40f, sweepAngle = 100f, useCenter = false,
        topLeft = Offset(cx - w * 0.22f, cy - h * 0.22f), size = Size(w * 0.44f, h * 0.44f), style = s)
    drawArc(color, startAngle = 220f, sweepAngle = 100f, useCenter = false,
        topLeft = Offset(cx - w * 0.38f, cy - h * 0.38f), size = Size(w * 0.76f, h * 0.76f), style = s)
    drawArc(color, startAngle = 40f, sweepAngle = 100f, useCenter = false,
        topLeft = Offset(cx - w * 0.38f, cy - h * 0.38f), size = Size(w * 0.76f, h * 0.76f), style = s)
}

private fun DrawScope.drawMailIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    drawRoundRect(color, Offset(w * 0.1f, h * 0.22f), Size(w * 0.8f, h * 0.56f), CornerRadius(2f), style = s)
    val flap = Path().apply {
        moveTo(w * 0.1f, h * 0.22f)
        lineTo(w * 0.5f, h * 0.52f)
        lineTo(w * 0.9f, h * 0.22f)
    }
    drawPath(flap, color, style = s)
}

private fun DrawScope.drawCalendarIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    drawRoundRect(color, Offset(w * 0.1f, h * 0.18f), Size(w * 0.8f, h * 0.7f), CornerRadius(2f), style = s)
    drawLine(color, Offset(w * 0.1f, h * 0.4f), Offset(w * 0.9f, h * 0.4f), strokeWidth = 1.2f)
    drawLine(color, Offset(w * 0.35f, h * 0.1f), Offset(w * 0.35f, h * 0.26f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.65f, h * 0.1f), Offset(w * 0.65f, h * 0.26f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.35f, h * 0.55f))
    drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.55f))
    drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.65f, h * 0.55f))
    drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.35f, h * 0.72f))
    drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.72f))
}

private fun DrawScope.drawLockIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    drawRoundRect(color, Offset(w * 0.15f, h * 0.45f), Size(w * 0.7f, h * 0.42f), CornerRadius(2f), style = s)
    val shackle = Path().apply {
        moveTo(w * 0.3f, h * 0.45f)
        lineTo(w * 0.3f, h * 0.3f)
        cubicTo(w * 0.3f, h * 0.12f, w * 0.7f, h * 0.12f, w * 0.7f, h * 0.3f)
        lineTo(w * 0.7f, h * 0.45f)
    }
    drawPath(shackle, color, style = s)
}

private fun DrawScope.drawDefaultIcon(color: Color) {
    drawCircle(color, radius = size.width * 0.35f, center = Offset(size.width / 2, size.height / 2),
        style = Stroke(1.4f))
}

// --- Essential icons ---

private fun DrawScope.drawPhoneIcon(color: Color) {
    val s = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    val path = Path().apply {
        moveTo(w * 0.15f, h * 0.6f)
        cubicTo(w * 0.15f, h * 0.3f, w * 0.35f, h * 0.15f, w * 0.5f, h * 0.15f)
        cubicTo(w * 0.65f, h * 0.15f, w * 0.85f, h * 0.3f, w * 0.85f, h * 0.6f)
    }
    drawPath(path, color, style = s)
    drawLine(color, Offset(w * 0.15f, h * 0.6f), Offset(w * 0.15f, h * 0.75f), strokeWidth = 1.6f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.85f, h * 0.6f), Offset(w * 0.85f, h * 0.75f), strokeWidth = 1.6f, cap = StrokeCap.Round)
}

private fun DrawScope.drawGlobeIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round)
    val cx = size.width / 2f; val cy = size.height / 2f
    val r = size.width * 0.38f
    drawCircle(color, radius = r, center = Offset(cx, cy), style = s)
    drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1.2f)
    drawOval(color, topLeft = Offset(cx - r * 0.4f, cy - r), size = Size(r * 0.8f, r * 2f), style = s)
}

private fun DrawScope.drawCameraIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width; val h = size.height
    drawRoundRect(color, Offset(w * 0.08f, h * 0.3f), Size(w * 0.84f, h * 0.55f), CornerRadius(2f), style = s)
    drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.57f), style = s)
    val path = Path().apply {
        moveTo(w * 0.32f, h * 0.3f)
        lineTo(w * 0.37f, h * 0.15f)
        lineTo(w * 0.63f, h * 0.15f)
        lineTo(w * 0.68f, h * 0.3f)
    }
    drawPath(path, color, style = s)
}
