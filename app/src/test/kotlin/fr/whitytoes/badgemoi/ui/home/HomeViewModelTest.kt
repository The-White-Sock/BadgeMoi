package fr.whitytoes.badgemoi.ui.home

import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.FakeActiveTripRepository
import fr.whitytoes.badgemoi.domain.StartTrip
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * La règle de démarrage elle-même est éprouvée dans `StartTripTest`, sur le code
 * partagé par l'écran d'accueil et le widget. Ce qui se vérifie ici, et nulle part
 * ailleurs, c'est la traduction du dépôt en état d'écran et la délégation au cas
 * d'usage.
 */
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

    private fun viewModel(repository: ActiveTripRepository) =
        HomeViewModel(
            activeTripRepository = repository,
            startTripUseCase = StartTrip(repository, clock, newTripId = { "t-neuf" }),
        )

    @Test
    fun `l'état initial est le chargement`() =
        runTest(dispatcher) {
            val model = viewModel(FakeActiveTripRepository())

            assertEquals(HomeUiState.Loading, model.uiState.value)
        }

    @Test
    fun `sans trajet enregistré l'écran propose un démarrage`() =
        runTest(dispatcher) {
            val model = viewModel(FakeActiveTripRepository())

            advanceUntilIdle()

            assertEquals(HomeUiState.Idle, model.uiState.value)
        }

    @Test
    fun `un trajet enregistré est proposé à la reprise`() =
        runTest(dispatcher) {
            val trip = Trip.start(id = "t1", direction = Direction.RETOUR, departureAt = now)
            val model = viewModel(FakeActiveTripRepository(trip))

            advanceUntilIdle()

            assertEquals(HomeUiState.TripInProgress(trip), model.uiState.value)
        }

    @Test
    fun `démarrer un trajet délègue au cas d'usage et le persiste`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository()
            val model = viewModel(repository)
            advanceUntilIdle()

            model.startTrip(Direction.ALLER)
            advanceUntilIdle()

            assertEquals(Direction.ALLER, repository.current?.direction)
            assertEquals(now, repository.current?.departureAt)
        }

    /**
     * Reproduction du défaut que l'extraction ferme : l'ancienne garde s'appuyait sur
     * `uiState`, encore à `Loading` juste après la construction, et refusait donc un
     * démarrage pourtant légitime. La garde vivant désormais dans l'écriture, l'appui
     * aboutit — et reste sans danger, le cas « trajet déjà en cours » étant couvert
     * dans `StartTripTest`.
     */
    @Test
    fun `démarrer avant la fin du chargement aboutit quand aucun trajet n'est en cours`() =
        runTest(dispatcher) {
            val repository = FakeActiveTripRepository()
            val model = viewModel(repository)

            model.startTrip(Direction.ALLER)
            advanceUntilIdle()

            assertEquals(Direction.ALLER, repository.current?.direction)
        }
}
