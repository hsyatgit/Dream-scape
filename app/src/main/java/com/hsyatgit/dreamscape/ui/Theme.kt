package com.hsyatgit.dreamscape.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Night = darkColorScheme(
    primary = Color(0xFFD7C4A3),
    onPrimary = Color(0xFF2A241C),
    secondary = Color(0xFF9BB7C9),
    background = Color(0xFF12161C),
    onBackground = Color(0xFFE7E1D6),
    surface = Color(0xFF1B222B),
    onSurface = Color(0xFFE7E1D6),
    surfaceVariant = Color(0xFF2A3340),
    error = Color(0xFFE7B4A8),
)

@Composable
fun DreamscapeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Night, content = content)
}
