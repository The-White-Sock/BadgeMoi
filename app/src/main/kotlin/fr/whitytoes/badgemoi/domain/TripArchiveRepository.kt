package fr.whitytoes.badgemoi.domain

import kotlinx.coroutines.flow.Flow

/** Archive des trajets terminés (écran Historique, lot 5). */
interface TripArchiveRepository {
    fun observeAll(): Flow<List<Trip>>

    suspend fun add(trip: Trip)

    suspend fun delete(id: String)

    suspend fun clear()
}
