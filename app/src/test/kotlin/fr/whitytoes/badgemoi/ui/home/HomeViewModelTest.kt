package fr.whitytoes.badgemoi.ui.home

import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-07-26T08:30:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `l'état initial est le chargement`() =
        runTest(dispatcher) {
            val viewModel = HomeViewModel(FakeActiveTripRepository(), clock)

            assertEquals(HomeUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `sans trajet enregistré l'écran propose un démarrage`() =
        runTest(dispatcher) {
            val viewModel = HomeViewModel(FakeActiveTripRepository(), clock)

            advanceUntilIdle()

            assertEquals(HomeUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `un trajet enregistré est proposé à la reprise`() =
        runTest(dispatcher) {
            val trip = Trip.start(id = "t1", direction = Direction.RETOUR, departureAt = now)
            val viewModel = HomeViewModel(FakeActiveTripRepository(trip), clock)

            advanceUntilIdle()

            assertEquals(HomeUiState.TripInProgress(trip), viewModel.uiState.value)
        }

    @Test
    fun `démarrer un trajet le persiste avec l'heure de l'horloge`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository()
            val viewModel = HomeViewModel(repository, clock)
            advanceUntilIdle()

            viewModel.startTrip(Direction.ALLER)
            advanceUntilIdle()

            val saved = repository.observe().first()
            assertEquals(Direction.ALLER, saved?.direction)
            assertEquals(now, saved?.departureAt)
            assertTrue("le trajet neuf n'est pas complet", saved?.isComplete == false)
        }

    /**
     * Garde-fou : l'application s'utilise en roulant, un double appui ne doit pas
     * écraser un trajet déjà entamé par un trajet neuf.
     */
    @Test
    fun `démarrer un trajet est sans effet si un trajet est déjà en cours`() =
        runTest(dispatcher) {
            val enCours = Trip.start(id = "t1", direction = Direction.RETOUR, departureAt = now)
            val repository = FakeActiveTripRepository(enCours)
            val viewModel = HomeViewModel(repository, clock)
            advanceUntilIdle()

            viewModel.startTrip(Direction.ALLER)
            advanceUntilIdle()

            assertEquals(enCours, repository.observe().first())
        }

    @Test
    fun `démarrer un trajet est sans effet tant que l'état n'est pas chargé`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository()
            val viewModel = HomeViewModel(repository, clock)

            viewModel.startTrip(Direction.ALLER)
            advanceUntilIdle()

            assertNull(repository.observe().first())
        }
}

private class FakeActiveTripRepository(
    initial: Trip? = null,
) : ActiveTripRepository {
    private val state = MutableStateFlow(initial)

    override fun observe(): Flow<Trip?> = state

    override suspend fun get(): Trip? = state.value

    override suspend fun save(trip: Trip) {
        state.value = trip
    }

    override suspend fun clear() {
        state.value = null
    }
}
