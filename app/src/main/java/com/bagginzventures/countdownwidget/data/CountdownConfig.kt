package com.bagginzventures.countdownwidget.data

import java.time.LocalDate

data class CountdownConfig(
    val title: String = DEFAULT_TITLE,
    val targetDate: LocalDate = LocalDate.now().plusDays(30),
    val accentTheme: AccentTheme = AccentTheme.Aurora
)
