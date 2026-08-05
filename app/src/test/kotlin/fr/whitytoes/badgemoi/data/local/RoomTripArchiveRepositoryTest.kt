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

    /**
     * La purge est **par sens** : l'écran Historique l'est de bout en bout, et emporter
     * l'autre sens détruirait des trajets que rien n'avait montrés à l'utilisateur.
     */
    @Test
    fun `purger un sens laisse l'autre intact`() =
        runTest {
            val aller = Trip.start(id = "aller", direction = Direction.ALLER, departureAt = testBase)
            val retour = Trip.start(id = "retour", direction = Direction.RETOUR, departureAt = testBase)
            repository.add(aller)
            repository.add(retour)

            repository.clear(Direction.ALLER)

            assertEquals(listOf(retour), repository.observeAll().first())
        }

    /** La purge emporte **tous** les trajets du sens, pas seulement le premier trouvé. */
    @Test
    fun `purger un sens vide tous ses trajets`() =
        runTest {
            repository.add(Trip.start(id = "a1", direction = Direction.ALLER, departureAt = testBase))
            repository.add(Trip.start(id = "a2", direction = Direction.ALLER, departureAt = testBase))

            repository.clear(Direction.ALLER)

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

    override suspend fun clear(direction: String) {
        rows.value = rows.value.filterNot { it.direction == direction }
    }
}
