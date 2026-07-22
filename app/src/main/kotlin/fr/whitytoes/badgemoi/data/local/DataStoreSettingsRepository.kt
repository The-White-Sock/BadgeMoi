package fr.whitytoes.badgemoi.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.whitytoes.badgemoi.domain.SettingsRepository
import fr.whitytoes.badgemoi.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Préférences persistées dans le DataStore. Thème par défaut : nuit (comme le POC). */
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override fun observeThemeMode(): Flow<ThemeMode> =
            dataStore.data.map { prefs ->
                prefs[KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.NIGHT
            }

        override suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { prefs -> prefs[KEY] = mode.name }
        }

        private companion object {
            val KEY = stringPreferencesKey("theme_mode")
        }
    }
