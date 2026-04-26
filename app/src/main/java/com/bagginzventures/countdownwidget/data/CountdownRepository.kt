package com.bagginzventures.countdownwidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "countdown_config")

class CountdownRepository(private val context: Context) {
    private object Keys {
        val title = stringPreferencesKey("title")
        val targetDate = stringPreferencesKey("target_date")
        val accentTheme = stringPreferencesKey("accent_theme")
    }

    val config: Flow<CountdownConfig> = context.dataStore.data.map { preferences ->
        CountdownConfig(
            title = preferences[Keys.title]?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE,
            targetDate = preferences[Keys.targetDate]?.let(LocalDate::parse) ?: LocalDate.now().plusDays(30),
            accentTheme = AccentTheme.fromKey(preferences[Keys.accentTheme])
        )
    }

    suspend fun save(config: CountdownConfig) {
        context.dataStore.edit { preferences ->
            preferences[Keys.title] = config.title.trim().ifBlank { DEFAULT_TITLE }
            preferences[Keys.targetDate] = config.targetDate.toString()
            preferences[Keys.accentTheme] = config.accentTheme.key
        }
    }
}
