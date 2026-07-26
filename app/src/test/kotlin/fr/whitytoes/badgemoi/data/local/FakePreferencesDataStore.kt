package fr.whitytoes.badgemoi.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * DataStore en mémoire : permet de tester les dépôts qui s'appuient dessus sans
 * fichier ni contexte Android. Suffisant ici car les dépôts n'utilisent que
 * `data` et `updateData` (ce dernier via l'extension `edit`).
 */
internal class FakePreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
