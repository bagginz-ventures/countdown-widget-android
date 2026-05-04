package com.bagginzventures.countdownwidget.data

import java.time.LocalDateTime
import java.util.UUID

data class CountdownStore(
    val events: List<CountdownConfig> = listOf(defaultCountdownEvent()),
    val selectedEventId: String? = events.firstOrNull()?.id,
    val widgetEventMap: Map<Int, String> = emptyMap()
) {
    fun resolveEvent(eventId: String?): CountdownConfig =
        events.firstOrNull { it.id == eventId }
            ?: events.firstOrNull()
            ?: defaultCountdownEvent()

    fun resolveEventForWidget(widgetId: Int): CountdownConfig = resolveEvent(widgetEventMap[widgetId])
}

fun defaultCountdownEvent(id: String = newCountdownEventId()): CountdownConfig = CountdownConfig(
    id = id,
    title = DEFAULT_TITLE,
    targetDateTime = LocalDateTime.now().plusDays(30),
    accentTheme = AccentTheme.Aurora,
    description = "",
    extraFieldEnabled = false,
    extraFieldLabel = "",
    extraFieldValue = "",
    backgroundPhotoPaths = emptyList(),
    rotationHours = 24
)

fun newCountdownEventId(): String = UUID.randomUUID().toString()
