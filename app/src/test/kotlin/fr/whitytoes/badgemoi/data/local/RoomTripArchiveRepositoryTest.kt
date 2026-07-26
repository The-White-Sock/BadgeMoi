package fr.whitytoes.badgemoi.data.local

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Archive des trajets. Le DAO est remplacé par un double en mémoire : ce qui est
 * vérifié ici est la conversion entité ↔ domaine et la délégation, pas SQLite.
 */
class RoomTripArchiveRepositoryTest {
    private val dao = FakeTripDao()
    private val repository = RoomTripArchiveRepository(dao)

    @Test
    fun `l'archive est vide au départ`() =
        runTest {
            assertTrue(repository.observeAll().first().isEmpty())
        }

    @Test
    fun `un trajet archivé est relu à l'identique`() =
        runTest {
            val trip = mixedTrip()

            repository.add(trip)

            assertEquals(listOf(trip), repository.observeAll().first())
        }

    @Test
    fun `supprimer un trajet ne retire que celui-ci`() =
        runTest {
            val premier = mixedTrip()
            val second = Trip.start(id = "trip-2", direction = Direction.ALLER, departureAt = testBase)
            repository.add(premier)
            repository.add(second)

            repository.delete(premier.id)

            assertEquals(listOf(second), repository.observeAll().first())
        }

    @Test
    fun `purger l'archive la vide entièrement`() =
        runTest {
            repository.add(mixedTrip())
            repository.add(Trip.start(id = "trip-2", direction = Direction.ALLER, departureAt = testBase))

            repository.clear()

            assertTrue(repository.observeAll().first().isEmpty())
        }
}

/** DAO en mémoire reproduisant le contrat de [TripDao] (remplacement sur conflit d'id). */
private class FakeTripDao : TripDao {
    private val rows = MutableStateFlow<List<TripEntity>>(emptyList())

    override fun observeAll(): Flow<List<TripEntity>> = rows

    override suspend fun insert(trip: TripEntity) {
        rows.value = rows.value.filterNot { it.id == trip.id } + trip
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }
}
