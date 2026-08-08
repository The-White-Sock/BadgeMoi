package fr.whitytoes.badgemoi.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doublure du dépôt de trajet en cours, partagée par les tests du domaine et de
 * l'écran d'accueil.
 *
 * Sa sémantique reproduit celle de `DataStoreActiveTripRepository`, relue avant
 * d'être imitée : `save` **remplace** sans condition, et `saveIfNoneInProgress`
 * n'écrit que lorsque rien n'est stocké, sans point de suspension entre le test et
 * l'écriture — c'est ce qui rend la garde indivisible dans le vrai dépôt, où les deux
 * tiennent dans un seul `edit`.
 */
internal class FakeActiveTripRepository(
    initial: Trip? = null,
) : ActiveTripRepository {
    private val state = MutableStateFlow(initial)

    val current: Trip? get() = state.value

    var saveCount: Int = 0
        private set

    override fun observe(): Flow<Trip?> = state

    override suspend fun get(): Trip? = state.value

    override suspend fun save(trip: Trip) {
        saveCount++
        state.value = trip
    }

    override suspend fun saveIfNoneInProgress(trip: Trip): Boolean {
        if (state.value != null) return false
        save(trip)
        return true
    }

    override suspend fun clear() {
        state.value = null
    }
}
