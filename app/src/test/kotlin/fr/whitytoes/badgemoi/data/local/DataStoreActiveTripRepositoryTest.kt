package fr.whitytoes.badgemoi.data.local

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Dépôt du trajet en cours. C'est la source de vérité partagée entre l'application
 * et le widget (cahier §3.6, §4.2) : un trajet doit se relire exactement tel qu'il
 * a été enregistré, y compris ses jalons ignorés ou non posés.
 */
class DataStoreActiveTripRepositoryTest {
    private val dataStore = FakePreferencesDataStore()
    private val repository = DataStoreActiveTripRepository(dataStore)

    @Test
    fun `sans trajet enregistré le dépôt est vide`() =
        runTest {
            assertNull(repository.get())
            assertNull(repository.observe().first())
        }

    @Test
    fun `un trajet enregistré est relu à l'identique`() =
        runTest {
            val trip = mixedTrip()

            repository.save(trip)

            assertEquals(trip, repository.get())
        }

    @Test
    fun `le flux observé reflète le trajet enregistré`() =
        runTest {
            val trip = mixedTrip()

            repository.save(trip)

            assertEquals(trip, repository.observe().first())
        }

    @Test
    fun `enregistrer un trajet remplace le précédent`() =
        runTest {
            repository.save(mixedTrip())
            val autre = Trip.start(id = "trip-2", direction = Direction.ALLER, departureAt = testBase)

            repository.save(autre)

            assertEquals(autre, repository.get())
        }

    @Test
    fun `effacer le trajet en cours vide le dépôt`() =
        runTest {
            repository.save(mixedTrip())

            repository.clear()

            assertNull(repository.get())
        }
}
