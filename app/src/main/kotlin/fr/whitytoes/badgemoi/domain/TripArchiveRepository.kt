package fr.whitytoes.badgemoi.domain

import kotlinx.coroutines.flow.Flow

/** Archive des trajets terminés (écran Historique, lot 5). */
interface TripArchiveRepository {
    fun observeAll(): Flow<List<Trip>>

    suspend fun add(trip: Trip)

    suspend fun delete(id: String)

    /**
     * Vide l'archive d'un **sens**, et de lui seul.
     *
     * L'écran Historique est par sens de bout en bout — sélecteur, moyennes, trajets
     * récents. Une purge qui déborderait sur l'autre sens détruirait des trajets que rien
     * n'avait montrés à l'utilisateur.
     */
    suspend fun clear(direction: Direction)
}
