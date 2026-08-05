package fr.whitytoes.badgemoi.domain

import kotlinx.coroutines.flow.Flow

/** Archive des trajets terminés (écran Historique, lot 5). */
interface TripArchiveRepository {
    fun observeAll(): Flow<List<Trip>>

    suspend fun add(trip: Trip)

    /**
     * Retire les trajets désignés, et eux seuls.
     *
     * **Unique** primitive de suppression : l'écran sélectionne des trajets, qu'il s'agisse
     * d'un seul ou de tout un sens via « Tout sélectionner ». Une purge par sens ferait un
     * second chemin vers le même résultat, sans appelant qui lui soit propre.
     */
    suspend fun delete(ids: Collection<String>)
}
