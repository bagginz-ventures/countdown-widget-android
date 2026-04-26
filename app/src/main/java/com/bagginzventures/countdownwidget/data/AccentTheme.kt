package com.bagginzventures.countdownwidget.data

import androidx.compose.ui.graphics.Color

const val DEFAULT_TITLE = "Dream date"

enum class AccentTheme(
    val key: String,
    val displayName: String,
    val accentColor: Long,
    val surfaceColor: Long,
    val surfaceTintColor: Long
) {
    Aurora(
        key = "aurora",
        displayName = "Aurora",
        accentColor = 0xFF76E4F7,
        surfaceColor = 0xFF121827,
        surfaceTintColor = 0xFF1E2D48
    ),
    Ember(
        key = "ember",
        displayName = "Ember",
        accentColor = 0xFFFF8A5B,
        surfaceColor = 0xFF1D1416,
        surfaceTintColor = 0xFF372126
    ),
    Orchid(
        key = "orchid",
        displayName = "Orchid",
        accentColor = 0xFFC5A3FF,
        surfaceColor = 0xFF181321,
        surfaceTintColor = 0xFF2A2140
    );

    val accentComposeColor: Color get() = Color(accentColor)
    val surfaceComposeColor: Color get() = Color(surfaceColor)
    val surfaceTintComposeColor: Color get() = Color(surfaceTintColor)

    companion object {
        fun fromKey(key: String?): AccentTheme = entries.firstOrNull { it.key == key } ?: Aurora
    }
}
