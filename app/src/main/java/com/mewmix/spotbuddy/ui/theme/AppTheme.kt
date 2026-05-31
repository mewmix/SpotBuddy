package com.mewmix.spotbuddy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFFE86F51),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDED4),
    onPrimaryContainer = Color(0xFF3D160D),
    secondary = Color(0xFF3E8D72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9F0DF),
    onSecondaryContainer = Color(0xFF0D2D22),
    tertiary = Color(0xFF4E6FAE),
    onTertiary = Color.White,
    background = Color(0xFFFAF6F0),
    onBackground = Color(0xFF2D2518),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2D2518),
    surfaceVariant = Color(0xFFF0E8DC),
    onSurfaceVariant = Color(0xFF5B5147),
    outline = Color(0xFFD4C8B8)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9B7F),
    onPrimary = Color(0xFF3D160D),
    primaryContainer = Color(0xFF663225),
    onPrimaryContainer = Color(0xFFFFDED4),
    secondary = Color(0xFF7EDDB9),
    onSecondary = Color(0xFF0D2D22),
    secondaryContainer = Color(0xFF265A49),
    onSecondaryContainer = Color(0xFFC9F0DF),
    tertiary = Color(0xFF9DB9F7),
    onTertiary = Color(0xFF13264F),
    background = Color(0xFF201A14),
    onBackground = Color(0xFFFAF6F0),
    surface = Color(0xFF2D2518),
    onSurface = Color(0xFFFAF6F0),
    surfaceVariant = Color(0xFF3D3629),
    onSurfaceVariant = Color(0xFFE0D8CC),
    outline = Color(0xFF6B5E4E)
)

@Composable
fun SpotBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = SpotBuddyTypography,
        content = content
    )
}

