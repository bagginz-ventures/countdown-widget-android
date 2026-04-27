package com.bagginzventures.countdownwidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

private val Context.dataStore by preferencesDataStore(name = "countdown_config")
private const val PHOTO_PATH_SEPARATOR = "\u001F"

class CountdownRepository(private val context: Context) {
    private object Keys {
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

    val config: Flow<CountdownConfig> = context.dataStore.data.map { preferences ->
        CountdownConfig(
            title = preferences[Keys.title]?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE,
            targetDateTime = preferences[Keys.targetDateTime]?.let(LocalDateTime::parse) ?: LocalDateTime.now().plusDays(30),
            accentTheme = AccentTheme.fromKey(preferences[Keys.accentTheme]),
            description = preferences[Keys.description].orEmpty(),
            extraFieldEnabled = (preferences[Keys.extraFieldEnabled] ?: 0) == 1,
            extraFieldLabel = preferences[Keys.extraFieldLabel].orEmpty(),
            extraFieldValue = preferences[Keys.extraFieldValue].orEmpty(),
            backgroundPhotoPaths = preferences[Keys.backgroundPhotoPaths]
                ?.split(PHOTO_PATH_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            rotationHours = preferences[Keys.rotationHours]?.coerceIn(1, 168) ?: 24
        )
    }

    suspend fun save(config: CountdownConfig) {
        context.dataStore.edit { preferences ->
            preferences[Keys.title] = config.title.trim().ifBlank { DEFAULT_TITLE }
            preferences[Keys.targetDateTime] = config.targetDateTime.toString()
            preferences[Keys.accentTheme] = config.accentTheme.key
            preferences[Keys.description] = config.description.trim()
            preferences[Keys.extraFieldEnabled] = if (config.extraFieldEnabled) 1 else 0
            preferences[Keys.extraFieldLabel] = config.extraFieldLabel.trim()
            preferences[Keys.extraFieldValue] = config.extraFieldValue.trim()
            preferences[Keys.backgroundPhotoPaths] = config.backgroundPhotoPaths.joinToString(PHOTO_PATH_SEPARATOR)
            preferences[Keys.rotationHours] = config.rotationHours.coerceIn(1, 168)
        }
    }
}
