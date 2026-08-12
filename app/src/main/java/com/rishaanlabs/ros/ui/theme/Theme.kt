package com.rishaanlabs.ros.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8F0),
    onPrimaryContainer = Color(0xFF1A1A2E),
    secondary = Color(0xFF4A4A6A),
    onSecondary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1A1A2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF0F0F5),
    onSurfaceVariant = Color(0xFF4A4A6A),
    outline = Color(0xFFD0D0E0),
    error = Color(0xFFB00020),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB0B0D0),
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = Color(0xFF2A2A4A),
    onPrimaryContainer = Color(0xFFB0B0D0),
    secondary = Color(0xFF9090B0),
    onSecondary = Color(0xFF1A1A2E),
    background = Color(0xFF0F0F1A),
    onBackground = Color(0xFFE0E0F0),
    surface = Color(0xFF1A1A2A),
    onSurface = Color(0xFFE0E0F0),
    surfaceVariant = Color(0xFF252535),
    onSurfaceVariant = Color(0xFFB0B0D0),
    outline = Color(0xFF3A3A5A),
    error = Color(0xFFCF6679),
    onError = Color(0xFF1A1A2E)
)

@Composable
fun RosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
