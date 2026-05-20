package com.micreta.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MicretaDark = darkColorScheme(
    primary = Color(0xFF3EA7FF),
    onPrimary = Color(0xFF002C50),
    secondary = Color(0xFF7BD0A3),
    onSecondary = Color(0xFF00321B),
    background = Color(0xFF0A0C10),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF13161C),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF1B1F27),
    onSurfaceVariant = Color(0xFF8A93A6),
    outline = Color(0xFF2A2F39),
    error = Color(0xFFFF5C7A),
    onError = Color(0xFF370011)
)

// Light scheme exists only as a fallback; the app is dark-first.
private val MicretaLight = lightColorScheme(
    primary = Color(0xFF0061A4),
    secondary = Color(0xFF2E7D5B),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun MicretaTheme(
    forceDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (forceDark || isSystemInDarkTheme()) MicretaDark else MicretaLight
    MaterialTheme(
        colorScheme = colors,
        typography = MicretaTypography,
        content = content
    )
}
