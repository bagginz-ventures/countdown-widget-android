package com.bagginzventures.countdownwidget.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

data class CountdownPresentation(
    val daysValue: String,
    val statusLabel: String,
    val detailLabel: String
)

object CountdownCalculator {
    private val widgetDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun daysUntil(targetDate: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, targetDate)

    fun presentation(config: CountdownConfig, today: LocalDate = LocalDate.now()): CountdownPresentation {
        val daysRemaining = daysUntil(config.targetDate, today)
        val formattedDate = config.targetDate.format(widgetDateFormatter)
        return when {
            daysRemaining > 0 -> CountdownPresentation(
                daysValue = daysRemaining.toString(),
                statusLabel = "days left",
                detailLabel = formattedDate
            )
            daysRemaining == 0L -> CountdownPresentation(
                daysValue = "0",
                statusLabel = "happening today",
                detailLabel = formattedDate
            )
            else -> CountdownPresentation(
                daysValue = daysRemaining.absoluteValue.toString(),
                statusLabel = "days since",
                detailLabel = formattedDate
            )
        }
    }
}
