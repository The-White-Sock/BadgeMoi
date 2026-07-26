package fr.whitytoes.badgemoi.data.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.whitytoes.badgemoi.domain.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Préférence de thème persistée : valeur par défaut, persistance et repli. */
class DataStoreSettingsRepositoryTest {
    private val dataStore = FakePreferencesDataStore()
    private val repository = DataStoreSettingsRepository(dataStore)

    @Test
    fun `sans préférence enregistrée le thème est nuit comme dans le POC`() =
        runTest {
            assertEquals(ThemeMode.NIGHT, repository.observeThemeMode().first())
        }

    @Test
    fun `le thème jour est persisté`() =
        runTest {
            repository.setThemeMode(ThemeMode.DAY)

            assertEquals(ThemeMode.DAY, repository.observeThemeMode().first())
        }

    @Test
    fun `le suivi du thème système est persisté`() =
        runTest {
            repository.setThemeMode(ThemeMode.SYSTEM)

            assertEquals(ThemeMode.SYSTEM, repository.observeThemeMode().first())
        }

    /**
     * Garde-fou : une valeur devenue illisible (renommage d'une entrée de l'enum,
     * donnée corrompue) ne doit pas faire planter l'application au démarrage.
     */
    @Test
    fun `une valeur illisible retombe sur le thème nuit`() =
        runTest {
            dataStore.edit { prefs -> prefs[stringPreferencesKey("theme_mode")] = "ARC_EN_CIEL" }

            assertEquals(ThemeMode.NIGHT, repository.observeThemeMode().first())
        }
}
