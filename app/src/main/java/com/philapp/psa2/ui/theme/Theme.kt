package com.philapp.psa2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Define your custom colors
private val LightGreen = Color(0xFFE8F5E9) // Light green background
private val MediumGreen = Color(0xFFA5D6A7) // Medium green for surfaces
private val DarkGreen = Color(0xFF2E7D32) // Darker green for text
private val AccentRed = Color(0xFFE53935) // Red for highlights/accents

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = Color.White,
    secondary = AccentRed,
    onSecondary = Color.White,
    tertiary = MediumGreen,
    background = LightGreen,
    surface = MediumGreen,
    surfaceVariant = LightGreen,
    onSurfaceVariant = DarkGreen
)

@Composable
fun PSATheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

