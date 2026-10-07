package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF1F1E1D),
    primaryContainer = Color(0xFF4A2A1C),
    onPrimaryContainer = Color(0xFFF6D9CB),
    secondary = FlameSecondaryDark,
    onSecondary = Color(0xFF1F1E1D),
    secondaryContainer = Color(0xFF34312C),
    onSecondaryContainer = Color(0xFFFFF6EE),
    tertiary = TurmericAccent,
    onTertiary = Color(0xFF2A1C00),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF34332F)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE5D6),
    onPrimaryContainer = Color(0xFF6B2A0C),
    secondary = FlameSecondaryLight,
    onSecondary = Color(0xFF1F1E1B),
    secondaryContainer = Color(0xFF1F1E1B),
    onSecondaryContainer = Color(0xFFFFF6EE),
    tertiary = TurmericAccent,
    onTertiary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceElevated,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFD9D3C3)
)

@Composable
fun FitBharatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        content = content
    )
}
