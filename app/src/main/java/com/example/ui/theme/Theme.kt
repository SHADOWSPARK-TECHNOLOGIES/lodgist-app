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

private val LodgistDarkColorScheme = darkColorScheme(
    primary = LodgistOrange,
    onPrimary = Color.White,
    primaryContainer = LodgistOrangeContainer,
    onPrimaryContainer = LodgistOrangeLight,
    secondary = LodgistTeal,
    onSecondary = Color.Black,
    secondaryContainer = LodgistTealContainer,
    onSecondaryContainer = LodgistTeal,
    tertiary = TruthScoreGreen,
    background = LodgistNavyBg,
    onBackground = LodgistTextLight,
    surface = LodgistNavySurface,
    onSurface = LodgistTextLight,
    surfaceVariant = LodgistNavySurfaceElevated,
    onSurfaceVariant = LodgistMutedLight,
    outline = LodgistNavyBorder,
    outlineVariant = Color(0xFF1E293B)
)

private val LodgistLightColorScheme = lightColorScheme(
    primary = LodgistOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8DF),
    onPrimaryContainer = LodgistOrangeDark,
    secondary = LodgistTealDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1F2EB),
    onSecondaryContainer = LodgistTealDark,
    tertiary = TruthScoreGreen,
    background = LodgistLightBg,
    onBackground = LodgistTextDark,
    surface = LodgistLightSurface,
    onSurface = LodgistTextDark,
    surfaceVariant = LodgistLightSurfaceElevated,
    onSurfaceVariant = LodgistMutedDark,
    outline = LodgistLightBorder,
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun LodgistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Preserve custom branding by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LodgistDarkColorScheme
        else -> LodgistLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for backward compatibility if needed
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = LodgistTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
