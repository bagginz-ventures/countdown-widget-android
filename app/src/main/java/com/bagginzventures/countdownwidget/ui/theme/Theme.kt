package com.bagginzventures.countdownwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FE9F7),
    onPrimary = Color(0xFF001F24),
    secondary = Color(0xFFC5A3FF),
    background = Color(0xFF0C1018),
    surface = Color(0xFF121927),
    onSurface = Color(0xFFE9EEF6)
)

@Composable
fun CountdownWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}
