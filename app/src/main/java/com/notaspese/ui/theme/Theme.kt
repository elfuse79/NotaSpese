package com.notaspese.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(primary = PrimaryLight, primaryContainer = PrimaryDark, background = BackgroundDark, surface = SurfaceDark, onSurface = TextOnPrimary, error = Error)
private val LightColorScheme = lightColorScheme(primary = Primary, onPrimary = TextOnPrimary, primaryContainer = PrimaryLight, background = BackgroundLight, surface = SurfaceLight, onSurface = TextPrimary, error = Error)

@Composable
fun NotaSpeseTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) { SideEffect { (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb() } }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
