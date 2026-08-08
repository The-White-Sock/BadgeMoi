package fr.whitytoes.badgemoi.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * La règle de démarrage a **deux** appelants — l'écran d'accueil et le widget — et
 * n'est éprouvée qu'ici, sur le code partagé. La couvrir de nouveau côté ViewModel
 * ne dirait rien de plus, et laisserait croire que les deux chemins sont vérifiés
 * séparément alors qu'ils exécutent le même code.
 */
class StartTripTest {
    private val now = Instant.parse("2026-08-08T07:15:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun startTrip(
        repository: ActiveTripRepository,
        id: String = "t-neuf",
    ) = StartTrip(activeTripRepository = repository, clock = clock, newTripId = { id })

    @Test
    fun `démarrer sans trajet en cours crée le trajet demandé`() =
        runTest {
            val repository = FakeActiveTripRepository()

            val started = startTrip(repository)(Direction.ALLER)

            assertTrue("le démarrage est accordé", started)
            assertEquals(Direction.ALLER, repository.current?.direction)
            assertEquals("t-neuf", repository.current?.id)
        }

    @Test
    fun `l'heure de départ vient de l'horloge injectée`() =
        runTest {
            val repository = FakeActiveTripRepository()

            startTrip(repository)(Direction.RETOUR)

            assertEquals(now, repository.current?.departureAt)
            assertEquals(now, repository.current?.createdAt)
        }

    @Test
    fun `le trajet créé est neuf, aucun jalon posé au-delà du départ`() =
        runTest {
            val repository = FakeActiveTripRepository()

            startTrip(repository)(Direction.ALLER)

            val trip = repository.current
            assertFalse("un trajet neuf n'est pas complet", trip?.isComplete ?: true)
            assertEquals(1, trip?.currentStep)
        }

    /**
     * Garde-fou : l'application s'utilise en roulant et le widget se touche à
     * l'aveugle. Un second appui ne doit pas effacer un trajet déjà entamé.
     */
    @Test
    fun `démarrer est sans effet si un trajet est déjà en cours`() =
        runTest {
            val enCours = Trip.start(id = "t-en-cours", direction = Direction.RETOUR, departureAt = now)
            val repository = FakeActiveTripRepository(enCours)

            val started = startTrip(repository)(Direction.ALLER)

            assertFalse("le démarrage est refusé", started)
            assertEquals(enCours, repository.current)
        }

    /**
     * Reproduction du défaut que la garde atomique ferme : deux appuis successifs ne
     * produisent qu'une seule écriture. Une garde qui relirait l'état avant d'écrire
     * laisserait la seconde passer.
     */
    @Test
    fun `un double appui ne crée qu'un seul trajet`() =
        runTest {
            val repository = FakeActiveTripRepository()
            val start = startTrip(repository)

            val first = start(Direction.ALLER)
            val second = start(Direction.RETOUR)

            assertTrue("le premier appui démarre", first)
            assertFalse("le second appui est refusé", second)
            assertEquals(1, repository.saveCount)
            assertEquals(Direction.ALLER, repository.current?.direction)
        }
}
