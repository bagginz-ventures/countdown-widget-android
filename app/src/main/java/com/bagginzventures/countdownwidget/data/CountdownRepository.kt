package com.bagginzventures.countdownwidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

private val Context.dataStore by preferencesDataStore(name = "countdown_config")
private const val PHOTO_PATH_SEPARATOR = "\u001F"

class CountdownRepository(private val context: Context) {
    private object Keys {
        val eventsJson = stringPreferencesKey("events_json")
        val selectedEventId = stringPreferencesKey("selected_event_id")
        val widgetBindingsJson = stringPreferencesKey("widget_bindings_json")

        // Legacy single-event keys retained for migration.
        val title = stringPreferencesKey("title")
        val targetDateTime = stringPreferencesKey("target_date_time")
        val accentTheme = stringPreferencesKey("accent_theme")
        val description = stringPreferencesKey("description")
        val extraFieldEnabled = intPreferencesKey("extra_field_enabled")
        val extraFieldLabel = stringPreferencesKey("extra_field_label")
        val extraFieldValue = stringPreferencesKey("extra_field_value")
        val backgroundPhotoPaths = stringPreferencesKey("background_photo_paths")
        val rotationHours = intPreferencesKey("rotation_hours")
    }

    val store: Flow<CountdownStore> = context.dataStore.data.map { preferences ->
        decodeStore(
            eventsJson = preferences[Keys.eventsJson],
            selectedEventId = preferences[Keys.selectedEventId],
            widgetBindingsJson = preferences[Keys.widgetBindingsJson],
            legacyPreferences = preferences
        )
    }

    val config: Flow<CountdownConfig> = store.map { it.resolveEvent(it.selectedEventId) }

    suspend fun createEvent(template: CountdownConfig? = null): CountdownConfig {
        val store = currentStore()
        val event = (template ?: defaultCountdownEvent()).copy(id = newCountdownEventId())
        val events = store.events + event
        writeStore(events, event.id, store.widgetEventMap)
        return event
    }

    suspend fun save(config: CountdownConfig) {
        saveEvent(config)
    }

    suspend fun saveEvent(config: CountdownConfig) {
        val store = currentStore()
        val event = normalizeEvent(config)
        val events = if (store.events.any { it.id == event.id }) {
            store.events.map { if (it.id == event.id) event else it }
        } else {
            store.events + event
        }
        val selectedId = store.selectedEventId?.takeIf { events.any { event -> event.id == it } } ?: event.id
        writeStore(events, selectedId, filterWidgetMap(store.widgetEventMap, events))
    }

    suspend fun deleteEvent(eventId: String) {
        val store = currentStore()
        val remaining = store.events.filterNot { it.id == eventId }
        val normalizedEvents = if (remaining.isEmpty()) listOf(defaultCountdownEvent()) else remaining
        val selectedId = when {
            store.selectedEventId == eventId -> normalizedEvents.first().id
            normalizedEvents.any { it.id == store.selectedEventId } -> store.selectedEventId
            else -> normalizedEvents.first().id
        }
        val widgetMap = store.widgetEventMap.filterValues { it != eventId }
        writeStore(normalizedEvents, selectedId, widgetMap)
    }

    suspend fun setSelectedEvent(eventId: String) {
        val store = currentStore()
        val selectedId = store.events.firstOrNull { it.id == eventId }?.id ?: store.events.first().id
        writeStore(store.events, selectedId, store.widgetEventMap)
    }

    suspend fun bindWidgetToEvent(widgetId: Int, eventId: String) {
        val store = currentStore()
        val resolvedEventId = store.events.firstOrNull { it.id == eventId }?.id ?: store.events.first().id
        writeStore(store.events, store.selectedEventId ?: resolvedEventId, store.widgetEventMap + (widgetId to resolvedEventId))
    }

    suspend fun removeWidgetBinding(widgetId: Int) {
        val store = currentStore()
        writeStore(store.events, store.selectedEventId ?: store.events.first().id, store.widgetEventMap - widgetId)
    }

    suspend fun eventForWidget(widgetId: Int): CountdownConfig = currentStore().resolveEventForWidget(widgetId)

    private suspend fun currentStore(): CountdownStore = store.first()

    private suspend fun writeStore(
        events: List<CountdownConfig>,
        selectedEventId: String?,
        widgetEventMap: Map<Int, String>
    ) {
        val normalizedEvents = normalizeEvents(events)
        val selectedId = selectedEventId?.takeIf { id -> normalizedEvents.any { it.id == id } } ?: normalizedEvents.first().id
        val filteredWidgetMap = filterWidgetMap(widgetEventMap, normalizedEvents)

        context.dataStore.edit { preferences ->
            preferences[Keys.eventsJson] = encodeEvents(normalizedEvents)
            preferences[Keys.selectedEventId] = selectedId
            preferences[Keys.widgetBindingsJson] = encodeWidgetBindings(filteredWidgetMap)
        }
    }

    private fun decodeStore(
        eventsJson: String?,
        selectedEventId: String?,
        widgetBindingsJson: String?,
        legacyPreferences: androidx.datastore.preferences.core.Preferences
    ): CountdownStore {
        val events = normalizeEvents(
            parseEvents(eventsJson).ifEmpty {
                listOf(readLegacyEvent(legacyPreferences))
            }
        )
        val resolvedSelectedId = selectedEventId?.takeIf { id -> events.any { it.id == id } } ?: events.first().id
        val widgetMap = filterWidgetMap(parseWidgetBindings(widgetBindingsJson), events)
        return CountdownStore(events = events, selectedEventId = resolvedSelectedId, widgetEventMap = widgetMap)
    }

    private fun parseEvents(raw: String?): List<CountdownConfig> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        CountdownConfig(
                            id = item.optString("id").ifBlank { newCountdownEventId() },
                            title = item.optString("title").ifBlank { DEFAULT_TITLE },
                            targetDateTime = item.optString("targetDateTime")
                                .takeIf { it.isNotBlank() }
                                ?.let(LocalDateTime::parse)
                                ?: LocalDateTime.now().plusDays(30),
                            accentTheme = AccentTheme.fromKey(item.optString("accentTheme")),
                            description = item.optString("description"),
                            extraFieldEnabled = item.optBoolean("extraFieldEnabled", false),
                            extraFieldLabel = item.optString("extraFieldLabel"),
                            extraFieldValue = item.optString("extraFieldValue"),
                            backgroundPhotoPaths = item.optJSONArray("backgroundPhotoPaths")?.toStringList() ?: emptyList(),
                            rotationHours = item.optInt("rotationHours", 24).coerceIn(1, 168)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeEvents(events: List<CountdownConfig>): String {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject()
                    .put("id", event.id)
                    .put("title", event.title)
                    .put("targetDateTime", event.targetDateTime.toString())
                    .put("accentTheme", event.accentTheme.key)
                    .put("description", event.description)
                    .put("extraFieldEnabled", event.extraFieldEnabled)
                    .put("extraFieldLabel", event.extraFieldLabel)
                    .put("extraFieldValue", event.extraFieldValue)
                    .put("backgroundPhotoPaths", JSONArray(event.backgroundPhotoPaths))
                    .put("rotationHours", event.rotationHours.coerceIn(1, 168))
            )
        }
        return array.toString()
    }

    private fun parseWidgetBindings(raw: String?): Map<Int, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val widgetId = key.toIntOrNull() ?: continue
                    val eventId = json.optString(key)
                    if (eventId.isNotBlank()) put(widgetId, eventId)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeWidgetBindings(bindings: Map<Int, String>): String {
        val json = JSONObject()
        bindings.forEach { (widgetId, eventId) -> json.put(widgetId.toString(), eventId) }
        return json.toString()
    }

    private fun readLegacyEvent(preferences: androidx.datastore.preferences.core.Preferences): CountdownConfig {
        val photos = preferences[Keys.backgroundPhotoPaths]
            ?.split(PHOTO_PATH_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        return defaultCountdownEvent().copy(
            title = preferences[Keys.title]?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE,
            targetDateTime = preferences[Keys.targetDateTime]?.let(LocalDateTime::parse) ?: LocalDateTime.now().plusDays(30),
            accentTheme = AccentTheme.fromKey(preferences[Keys.accentTheme]),
            description = preferences[Keys.description].orEmpty(),
            extraFieldEnabled = (preferences[Keys.extraFieldEnabled] ?: 0) == 1,
            extraFieldLabel = preferences[Keys.extraFieldLabel].orEmpty(),
            extraFieldValue = preferences[Keys.extraFieldValue].orEmpty(),
            backgroundPhotoPaths = photos,
            rotationHours = preferences[Keys.rotationHours]?.coerceIn(1, 168) ?: 24
        )
    }

    private fun normalizeEvents(events: List<CountdownConfig>): List<CountdownConfig> {
        val normalized = events.map { normalizeEvent(it) }.distinctBy { it.id }
        return if (normalized.isEmpty()) listOf(defaultCountdownEvent()) else normalized
    }

    private fun normalizeEvent(config: CountdownConfig): CountdownConfig = config.copy(
        id = config.id.ifBlank { newCountdownEventId() },
        title = config.title.trim().ifBlank { DEFAULT_TITLE },
        description = config.description.trim(),
        extraFieldLabel = config.extraFieldLabel.trim(),
        extraFieldValue = config.extraFieldValue.trim(),
        backgroundPhotoPaths = config.backgroundPhotoPaths.filter { it.isNotBlank() },
        rotationHours = config.rotationHours.coerceIn(1, 168)
    )

    private fun filterWidgetMap(widgetEventMap: Map<Int, String>, events: List<CountdownConfig>): Map<Int, String> {
        val ids = events.mapTo(mutableSetOf()) { it.id }
        return widgetEventMap.filterValues { it in ids }
    }
}

private fun JSONArray.toStringList(): List<String> = buildList {
    for (index in 0 until length()) {
        val value = optString(index)
        if (value.isNotBlank()) add(value)
    }
}
