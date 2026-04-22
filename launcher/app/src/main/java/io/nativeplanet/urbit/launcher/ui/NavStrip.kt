package io.nativeplanet.urbit.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.nativeplanet.urbit.launcher.data.Surface
import io.nativeplanet.urbit.launcher.theme.LauncherTheme

@Composable
fun NavStrip(
    currentSurface: Surface,
    onCommand: () -> Unit,
    onHomeToggle: () -> Unit,
    onTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Search / Command
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { onCommand() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val c = tokens.ink.copy(alpha = 0.6f)
                drawCircle(c, radius = 5f, center = Offset(7f, 7f), style = Stroke(1.6f))
                drawLine(c, Offset(11f, 11f), Offset(14f, 14f), strokeWidth = 1.6f, cap = StrokeCap.Round)
            }
        }

        // Center: home/drawer toggle bar
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onHomeToggle() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(108.dp, 4.dp)) {
                drawRoundRect(
                    color = tokens.ink.copy(alpha = 0.4f),
                    cornerRadius = CornerRadius(2f),
                    size = size
                )
            }
        }

        // Tasks / Recents
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { onTasks() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val c = tokens.ink.copy(alpha = 0.6f)
                val s = Stroke(1.4f, cap = StrokeCap.Round)
                // Two overlapping squares
                drawRoundRect(c, Offset(0f, 2f), Size(9f, 9f), CornerRadius(1.5f), style = s)
                drawRoundRect(c, Offset(4f, 0f), Size(9f, 9f), CornerRadius(1.5f), style = s)
            }
        }
    }
}
