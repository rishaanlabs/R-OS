package com.rishaanlabs.ros.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material's scheme, derived from the same tokens as [RosColors].
 *
 * Both exist because Material's own components (dialogs, sheets, snackbars, text fields) read the
 * scheme and would otherwise ignore the design entirely. Everything the app draws itself should
 * read [Ros.colors]; this is here so the framework's own surfaces agree with it.
 */
private val LightColorScheme = lightColorScheme(
    primary = RosLightColors.acc,
    onPrimary = RosLightColors.onAcc,
    primaryContainer = RosLightColors.surf2,
    onPrimaryContainer = RosLightColors.ink,
    secondary = RosLightColors.ink2,
    onSecondary = Color.White,
    background = RosLightColors.bg,
    onBackground = RosLightColors.ink,
    surface = RosLightColors.surf,
    onSurface = RosLightColors.ink,
    surfaceVariant = RosLightColors.surf2,
    onSurfaceVariant = RosLightColors.ink3,
    outline = RosLightColors.line,
    outlineVariant = RosLightColors.line2,
    error = RosLightColors.danger,
    onError = Color.White,
    scrim = RosLightColors.shade
)

private val DarkColorScheme = darkColorScheme(
    primary = RosDarkColors.acc,
    onPrimary = RosDarkColors.onAcc,
    primaryContainer = RosDarkColors.surf2,
    onPrimaryContainer = RosDarkColors.ink,
    secondary = RosDarkColors.ink2,
    onSecondary = RosDarkColors.bg,
    background = RosDarkColors.bg,
    onBackground = RosDarkColors.ink,
    surface = RosDarkColors.surf,
    onSurface = RosDarkColors.ink,
    surfaceVariant = RosDarkColors.surf2,
    onSurfaceVariant = RosDarkColors.ink3,
    outline = RosDarkColors.line,
    outlineVariant = RosDarkColors.line2,
    error = RosDarkColors.danger,
    onError = RosDarkColors.bg,
    scrim = RosDarkColors.shade
)

@Composable
fun RosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val rosColors = if (darkTheme) RosDarkColors else RosLightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = rosColors.bg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalRosColors provides rosColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RosTypography,
            content = content
        )
    }
}
