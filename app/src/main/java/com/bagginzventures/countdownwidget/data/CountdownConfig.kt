package com.bagginzventures.countdownwidget.data

import java.time.LocalDateTime

data class CountdownConfig(
    val id: String = "",
    val title: String = DEFAULT_TITLE,
    val targetDateTime: LocalDateTime = LocalDateTime.now().plusDays(30),
    val accentTheme: AccentTheme = AccentTheme.Aurora,
    val description: String = "",
    val extraFieldEnabled: Boolean = false,
    val extraFieldLabel: String = "",
    val extraFieldValue: String = "",
    val backgroundPhotoPaths: List<String> = emptyList(),
    val rotationHours: Int = 24
)
