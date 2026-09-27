package com.serviceops.mobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D6B45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F1D8),
    onPrimaryContainer = Color(0xFF062E1B),
    secondary = Color(0xFF496455),
    tertiary = Color(0xFF3D6471),
    background = Color(0xFFF6F7F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE1E7E0),
    outline = Color(0xFF747D76),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED5AC),
    primaryContainer = Color(0xFF075232),
    secondary = Color(0xFFB0CCBA),
    background = Color(0xFF101612),
    surface = Color(0xFF182019)
)

@Composable
fun ServiceOpsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
