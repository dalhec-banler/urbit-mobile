package io.nativeplanet.urbit.launcher.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nativeplanet.urbit.launcher.guest.GuestLauncher
import io.nativeplanet.urbit.launcher.theme.LauncherTheme

/**
 * Single bottom dock — essentials (call, browse, capture) + swipe-up handle.
 * Swipe up on the handle opens the app drawer.
 */
@Composable
fun BottomDock(
    context: Context,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Essentials row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem("call", tokens.ink) { GuestLauncher.launchDialer(context) }
            DockItem("browse", tokens.ink) { GuestLauncher.launchBrowser(context) }
            DockItem("capture", tokens.ink) { GuestLauncher.launchCamera(context) }
        }

        Spacer(Modifier.height(6.dp))

        // Swipe-up handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -60) onSwipeUp()
                    }
                }
                .clickable { onSwipeUp() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(40.dp, 4.dp)) {
                drawRoundRect(
                    color = tokens.ink.copy(alpha = 0.3f),
                    cornerRadius = CornerRadius(2f),
                    size = size
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    inkColor: Color,
    onClick: () -> Unit
) {
    val typo = LauncherTheme.typography
    val iconColor = inkColor.copy(alpha = 0.7f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            when (label) {
                "call" -> drawPhoneIcon(iconColor)
                "browse" -> drawGlobeIcon(iconColor)
                "capture" -> drawCameraIcon(iconColor)
            }
        }
        Text(
            text = label,
            style = typo.mono.copy(fontSize = 9.sp),
            color = inkColor.copy(alpha = 0.5f),
        )
    }
}

private fun DrawScope.drawPhoneIcon(color: Color) {
    val s = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width
    val h = size.height
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
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.width * 0.38f
    drawCircle(color, radius = r, center = Offset(cx, cy), style = s)
    drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1.2f)
    drawOval(color, topLeft = Offset(cx - r * 0.4f, cy - r), size = Size(r * 0.8f, r * 2f), style = s)
}

private fun DrawScope.drawCameraIcon(color: Color) {
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width
    val h = size.height
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
