package com.carecircle.medtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Evergreen = Color(0xFF0B6B57)
val EvergreenDark = Color(0xFF064B3E)
val Mint = Color(0xFFDDF2E9)
val Marigold = Color(0xFFF0A33B)
val Coral = Color(0xFFDD6B55)
val Canvas = Color(0xFFF7F8F3)
val Ink = Color(0xFF1A2723)
val MutedInk = Color(0xFF64716C)

private val LightColors = lightColorScheme(
    primary = Evergreen,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = EvergreenDark,
    secondary = Marigold,
    onSecondary = Color(0xFF3A2500),
    tertiary = Coral,
    background = Canvas,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9EEE8),
    onSurfaceVariant = MutedInk,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF73D7B7),
    primaryContainer = Color(0xFF075344),
    secondary = Color(0xFFFFB95C),
    tertiary = Color(0xFFFFB4A5),
    background = Color(0xFF111815),
    surface = Color(0xFF17211D),
    surfaceVariant = Color(0xFF29332F),
)

@Composable
fun FamilyMedicineTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
