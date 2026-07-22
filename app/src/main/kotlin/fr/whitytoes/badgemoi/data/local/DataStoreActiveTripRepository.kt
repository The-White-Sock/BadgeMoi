package fr.whitytoes.badgemoi.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Trajet en cours persisté dans le DataStore sous forme d'un unique objet JSON,
 * lisible aussi bien par l'application que par le widget.
 */
class DataStoreActiveTripRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ActiveTripRepository {
        override fun observe(): Flow<Trip?> = dataStore.data.map { prefs -> prefs[KEY]?.let(::decode) }

        override suspend fun get(): Trip? = observe().first()

        override suspend fun save(trip: Trip) {
            dataStore.edit { prefs -> prefs[KEY] = Json.encodeToString(trip.toStored()) }
        }

        override suspend fun clear() {
            dataStore.edit { prefs -> prefs.remove(KEY) }
        }

        private fun decode(raw: String): Trip = Json.decodeFromString<StoredTrip>(raw).toDomain()

        private companion object {
            val KEY = stringPreferencesKey("active_trip")
        }
    }
