@file:Suppress("MagicNumber") // Données de test : indices de jalons en clair.

package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val departure = Instant.parse("2026-07-26T08:00:00Z")
    private val now = Instant.parse("2026-07-26T08:12:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun trip() = Trip.start(id = "t1", direction = Direction.ALLER, departureAt = departure)

    private fun viewModel(repository: ActiveTripRepository) = TripViewModel(repository, clock)

    @Test
    fun `l'état initial est le chargement`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeActiveTripRepository())

            assertEquals(TripUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `sans trajet l'écran signale qu'il n'a plus d'objet`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeActiveTripRepository())

            advanceUntilIdle()

            assertEquals(TripUiState.NoTrip, viewModel.uiState.value)
        }

    @Test
    fun `valider pose le jalon courant à l'heure de l'horloge`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository(trip())
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.validateCurrentMilestone()
            advanceUntilIdle()

            // Le jalon 0 est le départ : le premier jalon à poser est le 1.
            assertEquals(now, repository.current?.times?.get(1))
            assertEquals(2, repository.current?.currentStep)
        }

    @Test
    fun `passer ignore le jalon courant sans l'horodater`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository(trip())
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.skipCurrentMilestone()
            advanceUntilIdle()

            assertNull(repository.current?.times?.get(1))
            assertTrue(repository.current?.skipped?.get(1) == true)
            assertEquals(2, repository.current?.currentStep)
        }

    /** Garde-fou : agir sur un trajet terminé le modifierait après coup. */
    @Test
    fun `valider est sans effet sur un trajet déjà terminé`() =
        runTest(dispatcher) {
            val complet =
                (1 until Routes.MILESTONE_COUNT).fold(trip()) { trip, index ->
                    trip.poseMilestone(index, departure.plusSeconds(index * 60L))
                }
            val repository = FakeActiveTripRepository(complet)
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.validateCurrentMilestone()
            advanceUntilIdle()

            assertEquals(complet, repository.current)
        }

    @Test
    fun `valider est sans effet tant que le trajet n'est pas chargé`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository()
            val viewModel = viewModel(repository)

            viewModel.validateCurrentMilestone()
            advanceUntilIdle()

            assertNull(repository.current)
        }

    @Test
    fun `corriger un jalon fixe l'heure fournie, pas celle de l'horloge`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository(trip())
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            val corrigee = departure.plusSeconds(300)

            viewModel.poseMilestone(index = 2, at = corrigee)
            advanceUntilIdle()

            assertEquals(corrigee, repository.current?.times?.get(2))
        }

    @Test
    fun `effacer un jalon posé le remet en attente et fait reculer le jalon courant`() =
        runTest(dispatcher) {
            val repository =
                FakeActiveTripRepository(trip().poseMilestone(1, departure.plusSeconds(60)))
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            assertEquals(2, repository.current?.currentStep)

            viewModel.clearMilestone(1)
            advanceUntilIdle()

            assertNull(repository.current?.times?.get(1))
            assertEquals(1, repository.current?.currentStep)
        }

    /**
     * La conversion heure locale → instant vit dans le ViewModel : elle a besoin de
     * l'horloge pour écarter une occurrence future, et l'écran n'en a pas.
     */
    @Test
    fun `corriger un jalon convertit l'heure saisie avec l'horloge du ViewModel`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository(trip())
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            // 08h05 UTC, soit cinq minutes après le départ — et avant `now` (08h12).
            viewModel.correctMilestone(index = 1, hour = 8, minute = 5)
            advanceUntilIdle()

            assertEquals(departure.plusSeconds(300), repository.current?.times?.get(1))
        }

    /** Reproduction : le bandeau doit refléter une correction du départ. */
    @Test
    fun `corriger le départ ajuste le temps écoulé`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository(trip())
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.timers.collect {} }
            runCurrent()

            assertEquals(12.minutes, viewModel.timers.value.elapsed)

            // Le départ était en réalité dix minutes plus tôt.
            viewModel.poseMilestone(0, departure.minusSeconds(600))
            runCurrent()

            assertEquals(22.minutes, viewModel.timers.value.elapsed)
        }

    /**
     * L'abandon a suivi la fenêtre de reprise, de l'accueil vers cet écran. Il efface le
     * trajet sans l'archiver — la confirmation, elle, appartient à l'écran.
     */
    @Test
    fun `abandonner efface le trajet en cours`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository(trip())
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.abandonTrip()
            advanceUntilIdle()

            assertNull(repository.current)
            assertEquals(TripUiState.NoTrip, viewModel.uiState.value)
        }

    @Test
    fun `chaque action est persistée immédiatement`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository(trip())
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.validateCurrentMilestone()
            advanceUntilIdle()
            viewModel.skipCurrentMilestone()
            advanceUntilIdle()

            // Deux écritures distinctes : l'application peut être tuée entre les deux.
            assertEquals(2, repository.saveCount)
        }
}

private class FakeActiveTripRepository(
    initial: Trip? = null,
) : ActiveTripRepository {
    private val state = MutableStateFlow(initial)
    var saveCount: Int = 0
        private set

    val current: Trip? get() = state.value

    override fun observe(): Flow<Trip?> = state

    override suspend fun get(): Trip? = state.value

    override suspend fun save(trip: Trip) {
        saveCount++
        state.value = trip
    }

    override suspend fun clear() {
        state.value = null
    }
}
