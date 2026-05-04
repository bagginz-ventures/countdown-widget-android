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
    val detailLabelDateTime: String,
    val breakdownLabel: String
)

object CountdownCalculator {
    private val widgetDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val widgetDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")

    fun daysUntil(targetDateTime: LocalDateTime, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, targetDateTime.toLocalDate())

    fun presentation(
        config: CountdownConfig,
        now: LocalDateTime = LocalDateTime.now(),
        today: LocalDate = now.toLocalDate()
    ): CountdownPresentation {
        val daysRemaining = daysUntil(config.targetDateTime, today)
        val formattedDate = config.targetDateTime.format(widgetDateFormatter)
        val formattedDateTime = config.targetDateTime.format(widgetDateTimeFormatter)
        val breakdownLabel = breakdown(now, config.targetDateTime)
        return when {
            daysRemaining > 0 -> CountdownPresentation(
                daysValue = daysRemaining.toString(),
                statusLabel = "days left",
                detailLabelDateOnly = formattedDate,
                detailLabelDateTime = formattedDateTime,
                breakdownLabel = breakdownLabel
            )
            daysRemaining == 0L -> CountdownPresentation(
                daysValue = "0",
                statusLabel = "happening today",
                detailLabelDateOnly = formattedDate,
                detailLabelDateTime = formattedDateTime,
                breakdownLabel = breakdownLabel
            )
            else -> CountdownPresentation(
                daysValue = daysRemaining.absoluteValue.toString(),
                statusLabel = "days since",
                detailLabelDateOnly = formattedDate,
                detailLabelDateTime = formattedDateTime,
                breakdownLabel = breakdownLabel
            )
        }
    }

    private fun breakdown(now: LocalDateTime, target: LocalDateTime): String {
        if (now == target) return "right now"

        val future = target.isAfter(now)
        val start = if (future) now else target
        val end = if (future) target else now
        var cursor = start

        var years = 0L
        while (!cursor.plusYears(1).isAfter(end)) {
            cursor = cursor.plusYears(1)
            years++
        }

        var months = 0L
        while (!cursor.plusMonths(1).isAfter(end)) {
            cursor = cursor.plusMonths(1)
            months++
        }

        val days = ChronoUnit.DAYS.between(cursor, end)
        cursor = cursor.plusDays(days)
        val hours = ChronoUnit.HOURS.between(cursor, end)
        cursor = cursor.plusHours(hours)
        val minutes = ChronoUnit.MINUTES.between(cursor, end)

        val parts = mutableListOf<String>()
        if (years != 0L) parts += unitLabel(years, "year")
        if (months != 0L) parts += unitLabel(months, "month")
        if (days != 0L) parts += unitLabel(days, "day")
        if (hours != 0L) parts += unitLabel(hours, "hour")
        if (minutes != 0L) parts += unitLabel(minutes, "minute")

        if (parts.isEmpty()) parts += "less than a minute"

        return if (future) parts.joinToString(", ") else "${parts.joinToString(", ")} ago"
    }

    private fun unitLabel(value: Long, unit: String): String =
        if (value == 1L) "1 $unit" else "$value ${unit}s"

    private fun unitLabel(value: Int, unit: String): String = unitLabel(value.toLong(), unit)
}
