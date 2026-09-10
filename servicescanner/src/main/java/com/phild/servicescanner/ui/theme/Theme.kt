package com.phild.servicescanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    secondary = BlueGrey80,
    tertiary = Sand80
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = ScannerContainer,
    onPrimaryContainer = Teal40,
    secondary = BlueGrey40,
    onSecondary = Color.White,
    tertiary = Sand40,
    background = ScannerMint,
    onBackground = ScannerInk,
    surface = ScannerSurface,
    onSurface = ScannerInk,
    surfaceVariant = ScannerContainer,
    onSurfaceVariant = ScannerMuted,
    outline = ScannerOutline
)

private val ScannerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ServiceScannerTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ScannerShapes,
        content = content
    )
}
