package io.nativeplanet.urbit.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nativeplanet.urbit.launcher.theme.LauncherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LauncherStatusBar(
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: time
        Text(
            text = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()),
            style = typo.mono.copy(fontSize = 11.sp),
            color = tokens.ink.copy(alpha = 0.85f),
        )

        // Center: dot
        Canvas(modifier = Modifier.size(4.dp)) {
            drawCircle(color = tokens.ink.copy(alpha = 0.4f), radius = size.minDimension / 2)
        }

        // Right: sigil + 5G + signal + battery
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Sigil(size = 12.dp)

            Text(
                text = "5G",
                style = typo.mono.copy(fontSize = 9.sp),
                color = tokens.ink.copy(alpha = 0.7f),
            )

            // Signal bars
            SignalIcon(color = tokens.ink.copy(alpha = 0.7f))

            // Battery
            BatteryIcon(color = tokens.ink.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun SignalIcon(color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(12.dp, 10.dp)) {
        val barW = size.width / 5.5f
        val gap = barW * 0.4f
        for (i in 0..3) {
            val barH = size.height * (0.3f + i * 0.23f)
            val x = i * (barW + gap)
            drawRect(
                color = color,
                topLeft = Offset(x, size.height - barH),
                size = Size(barW, barH)
            )
        }
    }
}

@Composable
private fun BatteryIcon(color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(16.dp, 9.dp)) {
        // Body
        drawRect(
            color = color,
            topLeft = Offset(0f, 1f),
            size = Size(size.width - 3, size.height - 2),
            style = Stroke(width = 1.2f)
        )
        // Tip
        drawRect(
            color = color,
            topLeft = Offset(size.width - 2.5f, size.height * 0.28f),
            size = Size(2f, size.height * 0.44f),
        )
        // Fill (80%)
        drawRect(
            color = color.copy(alpha = 0.6f),
            topLeft = Offset(1.5f, 2.5f),
            size = Size((size.width - 6) * 0.8f, size.height - 5f),
        )
    }
}
