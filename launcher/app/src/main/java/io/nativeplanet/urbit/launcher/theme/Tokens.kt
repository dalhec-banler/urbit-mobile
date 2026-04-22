package io.nativeplanet.urbit.launcher.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class LauncherTokens(
    val name: String,
    val bg: Color,
    val bgDeep: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val hair: Color,
    val tile: Color,
    val tileBorder: Color,
    val accent: Color,
    val accent2: Color,
    val sigilBg: Color,
    val sigilFg: Color,
    val chromeBg: Color,
    val radius: Int,
    val dark: Boolean,
    val quiet: Boolean = false,
    val density: Density = Density.Adaptive
)

enum class Density { Sparse, Adaptive, Dense }

enum class Aesthetic { Paper, Softbit, Prism, Terra, Grid }

val Paper = LauncherTokens(
    name = "paper",
    bg = Color(0xFFEFE9DD),
    bgDeep = Color(0xFFE4DCCD),
    ink = Color(0xFF111111),
    ink2 = Color(0xFF5A544A),
    ink3 = Color(0xFF8E8778),
    hair = Color(0x24111111),
    tile = Color(0xFFEFE9DD),
    tileBorder = Color(0x1F111111),
    accent = Color(0xFF111111),
    accent2 = Color(0x0F111111),
    sigilBg = Color(0xFF111111),
    sigilFg = Color(0xFFEFE9DD),
    chromeBg = Color(0xEBEFE9DD),
    radius = 0,
    dark = false,
    quiet = true,
    density = Density.Sparse
)

val Softbit = LauncherTokens(
    name = "softbit",
    bg = Color(0xFFF1EDE4),
    bgDeep = Color(0xFFE7E2D6),
    ink = Color(0xFF1A1815),
    ink2 = Color(0xFF5A554B),
    ink3 = Color(0xFF9A9287),
    hair = Color(0x1A1A1815),
    tile = Color(0xFFFAF7F0),
    tileBorder = Color(0x121A1815),
    accent = Color(0xFFCC5D3A),  // oklch(0.62 0.16 30) ≈ warm coral
    accent2 = Color(0x1FCC5D3A),
    sigilBg = Color(0xFF1A1815),
    sigilFg = Color(0xFFF1EDE4),
    chromeBg = Color(0xD9F1EDE4),
    radius = 12,
    dark = false,
    density = Density.Adaptive
)

val Prism = LauncherTokens(
    name = "prism",
    bg = Color(0xFF0D0E11),
    bgDeep = Color(0xFF07080A),
    ink = Color(0xFFF4F3EF),
    ink2 = Color(0xFFB7B4AC),
    ink3 = Color(0xFF6D6A63),
    hair = Color(0x14FFFFFF),
    tile = Color(0x0AFFFFFF),
    tileBorder = Color(0x14FFFFFF),
    accent = Color(0xFF3DC8E0),  // oklch(0.78 0.18 220) ≈ electric cyan
    accent2 = Color(0x293DC8E0),
    sigilBg = Color(0xFF3DC8E0),
    sigilFg = Color(0xFF0D0E11),
    chromeBg = Color(0xC70D0E11),
    radius = 16,
    dark = true,
    density = Density.Sparse
)

val Terra = LauncherTokens(
    name = "terra",
    bg = Color(0xFFE8DFD0),
    bgDeep = Color(0xFFD8CCB6),
    ink = Color(0xFF2B241B),
    ink2 = Color(0xFF6B5E4B),
    ink3 = Color(0xFFA79985),
    hair = Color(0x242B241B),
    tile = Color(0xFFF3EBDB),
    tileBorder = Color(0x142B241B),
    accent = Color(0xFF4A8C50),  // oklch(0.55 0.12 140) ≈ moss
    accent2 = Color(0x244A8C50),
    sigilBg = Color(0xFF2B241B),
    sigilFg = Color(0xFFE8DFD0),
    chromeBg = Color(0xE0E8DFD0),
    radius = 14,
    dark = false,
    density = Density.Adaptive
)

val Grid = LauncherTokens(
    name = "grid",
    bg = Color(0xFF000000),
    bgDeep = Color(0xFF000000),
    ink = Color(0xFFE6FFE6),
    ink2 = Color(0xFF7AA67A),
    ink3 = Color(0xFF3F5F3F),
    hair = Color(0x407AA67A),
    tile = Color(0xFF060A06),
    tileBorder = Color(0x597AA67A),
    accent = Color(0xFF80E680),  // oklch(0.85 0.20 140) ≈ phosphor green
    accent2 = Color(0x2480E680),
    sigilBg = Color(0xFF80E680),
    sigilFg = Color(0xFF000000),
    chromeBg = Color(0xE6000000),
    radius = 2,
    dark = true,
    density = Density.Dense
)

fun tokensByAesthetic(aesthetic: Aesthetic): LauncherTokens = when (aesthetic) {
    Aesthetic.Paper -> Paper
    Aesthetic.Softbit -> Softbit
    Aesthetic.Prism -> Prism
    Aesthetic.Terra -> Terra
    Aesthetic.Grid -> Grid
}
