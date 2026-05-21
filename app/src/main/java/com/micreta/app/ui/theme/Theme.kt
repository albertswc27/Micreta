package com.micreta.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MicretaDark = darkColorScheme(
    primary = MicretaBlueVivid,
    onPrimary = MicretaWarmWhite,
    secondary = MicretaCyanSoft,
    onSecondary = MicretaNavyDeep,
    tertiary = MicretaOk,                 // OK / healthy states
    onTertiary = MicretaWarmWhite,
    background = MicretaCharcoal,
    onBackground = MicretaWarmWhite,
    surface = MicretaNavyDeep,
    onSurface = MicretaWarmWhite,
    surfaceVariant = MicretaBlueDark,
    onSurfaceVariant = MicretaWarmWhite.copy(alpha = 0.66f),
    outline = MicretaBlueMid,
    error = MicretaAlert,
    onError = MicretaWarmWhite
)

// Light scheme exists only as a fallback; the app is dark-first.
private val MicretaLight = lightColorScheme(
    primary = MicretaBlueMid,
    secondary = MicretaCyanSoft,
    tertiary = MicretaOk,
    background = MicretaWarmWhite,
    surface = Color(0xFFFFFFFF),
    error = MicretaAlert
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
