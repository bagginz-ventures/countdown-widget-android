package com.bagginzventures.countdownwidget.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

data class CountdownPresentation(
    val daysValue: String,
    val statusLabel: String,
    val detailLabelDateOnly: String,
    val detailLabelDateTime: String
)

object CountdownCalculator {
    private val widgetDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val widgetDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")

    fun daysUntil(targetDateTime: LocalDateTime, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, targetDateTime.toLocalDate())

    fun presentation(config: CountdownConfig, today: LocalDate = LocalDate.now()): CountdownPresentation {
        val daysRemaining = daysUntil(config.targetDateTime, today)
        val formattedDate = config.targetDateTime.format(widgetDateFormatter)
        val formattedDateTime = config.targetDateTime.format(widgetDateTimeFormatter)
        return when {
            daysRemaining > 0 -> CountdownPresentation(
                daysValue = daysRemaining.toString(),
                statusLabel = "days left",
                detailLabelDateOnly = formattedDate,
                detailLabelDateTime = formattedDateTime
            )
            daysRemaining == 0L -> CountdownPresentation(
                daysValue = "0",
                statusLabel = "happening today",
                detailLabelDateOnly = formattedDate,
                detailLabelDateTime = formattedDateTime
            )
            else -> CountdownPresentation(
                daysValue = daysRemaining.absoluteValue.toString(),
                statusLabel = "days since",
                detailLabelDateOnly = formattedDate,
                detailLabelDateTime = formattedDateTime
            )
        }
    }
}
