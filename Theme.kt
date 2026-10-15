package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = MickVioletPrimary,
    onPrimary = Color(0xFF130938),
    primaryContainer = MickVioletContainer,
    onPrimaryContainer = MickOnVioletContainer,
    secondary = MickGoldSecondary,
    onSecondary = Color(0xFF281C03),
    secondaryContainer = MickGoldContainer,
    onSecondaryContainer = MickOnGoldContainer,
    tertiary = MickCyanTertiary,
    onTertiary = Color(0xFF032527),
    tertiaryContainer = MickCyanContainer,
    onTertiaryContainer = MickOnCyanContainer,
    background = MickDarkBackground,
    onBackground = Color(0xFFE9EAFF),
    surface = MickDarkSurface,
    onSurface = Color(0xFFE9EAFF),
    surfaceVariant = MickDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC7CAE2),
    outline = MickDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = MickVioletDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF23114F),
    secondary = Color(0xFF885E00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF2C1C00),
    tertiary = Color(0xFF00696D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9CF1F5),
    onTertiaryContainer = Color(0xFF002022),
    background = MickLightBackground,
    onBackground = Color(0xFF171926),
    surface = MickLightSurface,
    onSurface = Color(0xFF171926),
    surfaceVariant = MickLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF45475A),
    outline = MickLightOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted creative palette by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
