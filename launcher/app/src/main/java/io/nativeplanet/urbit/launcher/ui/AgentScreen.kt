package io.nativeplanet.urbit.launcher.ui

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.nativeplanet.urbit.launcher.data.AgentInfo
import io.nativeplanet.urbit.launcher.theme.LauncherTheme

/**
 * AgentScreen — opens the Urbit app's frontend in a WebView.
 * Authenticates by POSTing the ship code to /~/login inside the WebView,
 * then redirects to the app. This lets the WebView handle its own cookies
 * natively, avoiding Android CookieManager timing issues.
 */
@Composable
fun AgentScreen(
    agent: AgentInfo,
    shipUrl: String,
    shipCode: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LauncherTheme.tokens
    val typo = LauncherTheme.typography
    val appUrl = "$shipUrl/apps/${agent.desk}"
    val bgArgb = tokens.bg.toArgb()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "← back",
                style = typo.mono.copy(fontSize = 11.sp),
                color = tokens.ink2,
                modifier = Modifier.clickable { onBack() }
            )
            Text(
                text = agent.name.take(1).uppercase(),
                style = typo.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = tokens.accent,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = agent.name,
                    style = typo.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    color = tokens.ink,
                )
                Text(
                    text = agent.verb,
                    style = typo.mono.copy(fontSize = 10.sp),
                    color = tokens.ink3,
                )
            }
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.setSupportMultipleWindows(false)
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    setBackgroundColor(bgArgb)

                    // Enable keyboard input for WebView content (e.g. terminal)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()

                    val webView = this
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(webView, true)
                    }

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        private var didLogin = false

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // After the login POST completes, navigate to the app
                            if (!didLogin && url?.contains("/~/login") == true) {
                                didLogin = true
                                view?.loadUrl(appUrl)
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false
                    }

                    // Login via the WebView itself — POST the code to /~/login.
                    // The response sets the auth cookie natively in the WebView's
                    // cookie store, then onPageFinished redirects to the app.
                    if (shipCode != null) {
                        postUrl(
                            "$shipUrl/~/login",
                            "password=$shipCode".toByteArray()
                        )
                    } else {
                        // No code available — try loading directly (may redirect to login)
                        loadUrl(appUrl)
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}
