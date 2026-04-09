package com.dkshe.audiobookplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = DeepBackground,
    secondary = AccentMuted,
    onSecondary = TextPrimary,
    background = DeepBackground,
    onBackground = TextPrimary,
    surface = SurfaceBase,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = Error,
)

@Composable
fun AudiobookPlayerTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AudiobookTypography,
        content = content,
    )
}
