package com.sih26001.mobilealert.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Slate900,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = Slate700,
    onSecondary = Color.White,
    secondaryContainer = Slate200,
    onSecondaryContainer = Slate800,
    tertiary = WarningAmber700,
    onTertiary = Color.White,
    tertiaryContainer = WarningAmber100,
    onTertiaryContainer = WarningAmber900,
    error = AlertRed600,
    onError = Color.White,
    errorContainer = AlertRed100,
    onErrorContainer = AlertRed900,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    outlineVariant = Slate300
)

private val DarkColorScheme = lightColorScheme(
    primary = Slate100,
    onPrimary = Slate900,
    primaryContainer = Slate800,
    onPrimaryContainer = Slate100,
    secondary = Slate300,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = WarningAmber500,
    onTertiary = Slate900,
    tertiaryContainer = WarningAmber900,
    onTertiaryContainer = WarningAmber100,
    error = AlertRed500,
    onError = Slate900,
    errorContainer = AlertRed900,
    onErrorContainer = AlertRed100,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    outlineVariant = Slate500
)

@Composable
fun SIH26001MobileAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Strictly preserve crisp disaster response styling rather than unpredictable dynamic pastel wallpaper colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
