package com.fleetopt.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Monochromatic Industrial Dark Palette
private val DarkColorScheme = darkColorScheme(
    primary = Teal400,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = Teal400,
    secondary = Emerald400,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = Emerald400,
    tertiary = Cyan500,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    surfaceContainerHighest = Slate800,
    error = Red400,
    onError = Color.White,
    outline = Slate700,
    outlineVariant = Slate800
)

// Monochromatic Crisp Light Palette
private val LightColorScheme = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Teal600,
    secondary = Emerald600,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Emerald600,
    tertiary = Cyan500,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceContainerHighest = Slate200,
    error = Red500,
    onError = Color.White,
    outline = Slate300,
    outlineVariant = Slate200
)

@Composable
fun FleetOptTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
