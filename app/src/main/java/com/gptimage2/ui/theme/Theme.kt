package com.gptimage2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object GptColors {
    val Obsidian = Color(0xFF0A0A0A)
    val Onyx = Color(0xFF141414)
    val Charcoal = Color(0xFF1C1C1C)
    val Graphite = Color(0xFF242424)
    val Steel = Color(0xFF2E2E2E)
    val ChampagneGold = Color(0xFFD4AF6E)
    val SoftGold = Color(0xFFE3C389)
    val DimGold = Color(0xFF8A7549)
    val WarmWhite = Color(0xFFF5EEDC)
    val Muted = Color(0xFFB9B2A4)
    val Error = Color(0xFFD46E6E)
    val Success = Color(0xFF9EC78C)
}

private val GptColorScheme = darkColorScheme(
    primary = GptColors.ChampagneGold,
    onPrimary = GptColors.Obsidian,
    primaryContainer = GptColors.Steel,
    onPrimaryContainer = GptColors.SoftGold,
    secondary = GptColors.SoftGold,
    onSecondary = GptColors.Obsidian,
    background = GptColors.Obsidian,
    onBackground = GptColors.WarmWhite,
    surface = GptColors.Onyx,
    onSurface = GptColors.WarmWhite,
    surfaceVariant = GptColors.Charcoal,
    onSurfaceVariant = GptColors.Muted,
    error = GptColors.Error,
    onError = GptColors.Obsidian,
    outline = GptColors.DimGold
)

private val GptTypography = Typography(
    displayLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontSize = 12.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
)

@Composable
fun GptImageTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme() // ignored; we force dark
    MaterialTheme(
        colorScheme = GptColorScheme,
        typography = GptTypography,
        content = content
    )
}
