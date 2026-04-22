package io.nativeplanet.urbit.launcher.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = io.nativeplanet.urbit.launcher.R.array.com_google_android_gms_fonts_certs
)

val FrauncesFamily = FontFamily(
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = provider),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = provider, weight = FontWeight.Bold),
)

val InterFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Bold),
)

val InstrumentSerifFamily = FontFamily(
    Font(googleFont = GoogleFont("Instrument Serif"), fontProvider = provider),
)

val JetBrainsMonoFamily = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Medium),
)

data class LauncherTypography(
    val titleFont: FontFamily,
    val bodyFont: FontFamily,
    val monoFont: FontFamily,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
)

fun typographyForAesthetic(aesthetic: Aesthetic): LauncherTypography {
    val titleFont = when (aesthetic) {
        Aesthetic.Paper -> FrauncesFamily
        Aesthetic.Softbit -> InterFamily
        Aesthetic.Prism -> InstrumentSerifFamily
        Aesthetic.Terra -> FrauncesFamily
        Aesthetic.Grid -> JetBrainsMonoFamily
    }
    val bodyFont = when (aesthetic) {
        Aesthetic.Paper -> FrauncesFamily
        Aesthetic.Grid -> JetBrainsMonoFamily
        else -> InterFamily
    }
    val monoFont = JetBrainsMonoFamily

    return LauncherTypography(
        titleFont = titleFont,
        bodyFont = bodyFont,
        monoFont = monoFont,
        headline = TextStyle(
            fontFamily = titleFont,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp,
            lineHeight = 32.sp,
        ),
        title = TextStyle(
            fontFamily = titleFont,
            fontSize = 22.sp,
            letterSpacing = (-0.3).sp,
        ),
        body = TextStyle(
            fontFamily = bodyFont,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        label = TextStyle(
            fontFamily = bodyFont,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        mono = TextStyle(
            fontFamily = monoFont,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
        ),
        monoSmall = TextStyle(
            fontFamily = monoFont,
            fontSize = 9.sp,
            letterSpacing = 0.3.sp,
        ),
    )
}

val LocalTokens = staticCompositionLocalOf { Softbit }
val LocalTypography = staticCompositionLocalOf { typographyForAesthetic(Aesthetic.Softbit) }

object LauncherTheme {
    val tokens: LauncherTokens
        @Composable get() = LocalTokens.current
    val typography: LauncherTypography
        @Composable get() = LocalTypography.current
}

@Composable
fun LauncherTheme(
    aesthetic: Aesthetic = Aesthetic.Softbit,
    content: @Composable () -> Unit
) {
    val tokens = tokensByAesthetic(aesthetic)
    val typography = typographyForAesthetic(aesthetic)

    CompositionLocalProvider(
        LocalTokens provides tokens,
        LocalTypography provides typography,
        content = content
    )
}
