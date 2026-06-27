package com.dealwatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = GreenLight,
    secondary = Amber,
    surface = Surface,
    onSurface = OnSurface,
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    primaryContainer = GreenDark,
    secondary = Amber,
)

@Composable
fun DealWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
