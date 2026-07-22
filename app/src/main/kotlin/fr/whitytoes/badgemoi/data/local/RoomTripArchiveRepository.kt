package fr.whitytoes.badgemoi.data.local

import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Implémentation Room de l'archive des trajets. */
class RoomTripArchiveRepository
    @Inject
    constructor(
        private val dao: TripDao,
    ) : TripArchiveRepository {
        override fun observeAll(): Flow<List<Trip>> =
            dao.observeAll().map { entities -> entities.map(TripEntity::toDomain) }

        override suspend fun add(trip: Trip) = dao.insert(trip.toEntity())

        override suspend fun delete(id: String) = dao.delete(id)

        override suspend fun clear() = dao.clear()
    }
