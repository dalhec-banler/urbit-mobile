package io.nativeplanet.urbit.launcher.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import io.nativeplanet.urbit.launcher.theme.LauncherTheme

/**
 * Setup screen — shown when not yet connected to the local ship.
 * Tries auto-connect via loopback first; manual +code entry as fallback.
 */
@Composable
fun SetupScreen(
    isConnecting: Boolean = false,
    error: String? = null,
    onConnect: (String) -> Unit,
    onAutoConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    var code by remember { mutableStateOf(TextFieldValue("")) }
    var showManual by remember { mutableStateOf(false) }

    // Auto-connect on first composition
    LaunchedEffect(Unit) {
        onAutoConnect()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Sigil(size = 48.dp)

        Spacer(Modifier.height(24.dp))

        if (!showManual && !isConnecting) {
            Text(
                text = "connecting to your ship",
                style = typo.title.copy(fontSize = 22.sp),
                color = tokens.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "auto-detecting via loopback…",
                style = typo.body.copy(fontSize = 13.sp),
                color = tokens.ink2,
            )
        } else if (isConnecting) {
            Text(
                text = "connecting…",
                style = typo.title.copy(fontSize = 22.sp),
                color = tokens.ink,
            )
        }

        if (error != null || showManual) {
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    style = typo.mono.copy(fontSize = 11.sp),
                    color = tokens.accent,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "enter +code manually",
                style = typo.body.copy(fontSize = 13.sp),
                color = tokens.ink2,
            )

            Spacer(Modifier.height(12.dp))

            BasicTextField(
                value = code,
                onValueChange = { code = it },
                textStyle = typo.mono.copy(fontSize = 16.sp, color = tokens.ink, letterSpacing = 2.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(tokens.radius.dp))
                    .background(tokens.tile)
                    .border(1.dp, tokens.tileBorder, RoundedCornerShape(tokens.radius.dp))
                    .padding(16.dp),
                decorationBox = { inner ->
                    if (code.text.isEmpty()) {
                        Text(
                            text = "sampel-palnet-sampel-palnet",
                            style = typo.mono.copy(fontSize = 16.sp, letterSpacing = 2.sp),
                            color = tokens.ink3,
                        )
                    }
                    inner()
                }
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(tokens.radius.dp))
                    .background(tokens.accent)
                    .clickable(enabled = !isConnecting && code.text.isNotBlank()) {
                        onConnect(code.text.trim())
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "connect",
                    style = typo.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = tokens.sigilFg,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!showManual) {
            Text(
                text = "enter code manually →",
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink2,
                modifier = Modifier.clickable { showManual = true }
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "ship at localhost:80 · loopback :12321",
            style = typo.mono.copy(fontSize = 10.sp),
            color = tokens.ink3,
        )
    }
}
