package com.bagginzventures.countdownwidget

import com.bagginzventures.countdownwidget.data.AccentTheme
import com.bagginzventures.countdownwidget.data.CountdownCalculator
import com.bagginzventures.countdownwidget.data.CountdownConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CountdownCalculatorTest {
    @Test
    fun `returns days left for future dates`() {
        val config = CountdownConfig(
            title = "Launch",
            targetDateTime = LocalDateTime.of(2026, 5, 5, 19, 30),
            accentTheme = AccentTheme.Aurora
        )

        val presentation = CountdownCalculator.presentation(
            config = config,
            today = LocalDate.of(2026, 5, 1)
        )

        assertEquals("4", presentation.daysValue)
        assertEquals("days left", presentation.statusLabel)
    }
}
