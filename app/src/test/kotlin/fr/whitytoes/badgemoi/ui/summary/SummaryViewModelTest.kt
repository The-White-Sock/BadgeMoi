@file:Suppress("MagicNumber") // Données de test : indices de jalons en clair.

package fr.whitytoes.badgemoi.ui.summary

import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val departure = Instant.parse("2026-07-26T08:00:00Z")

    /** Journal partagé par les deux dépôts : il donne l'**ordre** réel des écritures. */
    private val operations = mutableListOf<String>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun trip() = Trip.start(id = "t1", direction = Direction.ALLER, departureAt = departure)

    /** Trajet dont tous les jalons sont posés : le seul cas archivable. */
    private fun completedTrip() =
        (1 until Routes.MILESTONE_COUNT).fold(trip()) { trip, index ->
            trip.poseMilestone(index, departure.plusSeconds(index * 600L))
        }

    private fun viewModel(
        active: FakeActiveTripRepository,
        archive: FakeArchiveRepository,
    ) = SummaryViewModel(active, archive)

    @Test
    fun `l'état initial est le chargement`() =
        runTest(dispatcher) {
            val model = viewModel(FakeActiveTripRepository(), FakeArchiveRepository())

            assertEquals(SummaryUiState.Loading, model.uiState.value)
        }

    @Test
    fun `sans trajet l'écran n'a plus d'objet`() =
        runTest(dispatcher) {
            val model = viewModel(FakeActiveTripRepository(), FakeArchiveRepository())

            advanceUntilIdle()

            assertEquals(SummaryUiState.NoTrip, model.uiState.value)
        }

    @Test
    fun `le trajet en cours est proposé à la relecture`() =
        runTest(dispatcher) {
            val complet = completedTrip()
            val model = viewModel(FakeActiveTripRepository(complet), FakeArchiveRepository())

            advanceUntilIdle()

            assertEquals(SummaryUiState.Ready(complet), model.uiState.value)
        }

    @Test
    fun `enregistrer archive le trajet et vide le trajet en cours`() =
        runTest(dispatcher) {
            val complet = completedTrip()
            val active = FakeActiveTripRepository(complet)
            val archive = FakeArchiveRepository()
            val model = viewModel(active, archive)
            advanceUntilIdle()

            model.archiveTrip()
            advanceUntilIdle()

            assertEquals(listOf(complet), archive.trips)
            assertEquals("le trajet en cours est vidé", null, active.current)
            assertEquals(SummaryUiState.NoTrip, model.uiState.value)
        }

    /**
     * Le garde-fou du lot : l'ordre inverse perdrait le trajet si l'écriture en archive
     * échouait. Le journal des deux dépôts rend cet ordre vérifiable.
     */
    @Test
    fun `le trajet est archivé avant d'être effacé`() =
        runTest(dispatcher) {
            val active = FakeActiveTripRepository(completedTrip())
            val model = viewModel(active, FakeArchiveRepository())
            advanceUntilIdle()

            model.archiveTrip()
            advanceUntilIdle()

            assertEquals(listOf("archive", "clear"), operations)
        }

    /**
     * L'application s'utilise en roulant : un double appui est probable. Sans verrou, il
     * lancerait deux écritures avant que la première n'ait vidé le trajet en cours.
     */
    @Test
    fun `un double appui n'archive qu'une fois`() =
        runTest(dispatcher) {
            val archive = FakeArchiveRepository()
            val model = viewModel(FakeActiveTripRepository(completedTrip()), archive)
            advanceUntilIdle()

            model.archiveTrip()
            model.archiveTrip()
            advanceUntilIdle()

            assertEquals(1, archive.addCount)
        }

    /**
     * Le verrou doit être **visible** et pas seulement effectif, pour que l'écran puisse
     * désactiver le bouton. L'écriture est retenue par une barrière, sans quoi tout
     * s'enchaînerait dans le même tour et l'état intermédiaire serait inobservable.
     */
    @Test
    fun `l'archivage est signalé dans l'état, pour que le bouton se désactive`() =
        runTest(dispatcher) {
            val barriere = CompletableDeferred<Unit>()
            val model =
                viewModel(FakeActiveTripRepository(completedTrip()), FakeArchiveRepository(barriere))
            advanceUntilIdle()

            model.archiveTrip()
            advanceUntilIdle()

            assertTrue("pendant l'écriture", (model.uiState.value as SummaryUiState.Ready).archiving)

            barriere.complete(Unit)
            advanceUntilIdle()

            assertEquals("une fois l'écriture finie", SummaryUiState.NoTrip, model.uiState.value)
        }

    /** Un trajet dont il reste des jalons à traiter a sa place sur l'écran actif. */
    @Test
    fun `un trajet incomplet n'est pas archivable`() =
        runTest(dispatcher) {
            val active = FakeActiveTripRepository(trip().poseMilestone(1, departure.plusSeconds(600)))
            val archive = FakeArchiveRepository()
            val model = viewModel(active, archive)
            advanceUntilIdle()

            model.archiveTrip()
            advanceUntilIdle()

            assertEquals(emptyList<Trip>(), archive.trips)
            assertEquals("le trajet en cours est intact", 2, active.current?.currentStep)
        }

    /** Le trajet a pu être abandonné depuis l'accueil pendant la relecture. */
    @Test
    fun `enregistrer est sans effet quand le trajet a disparu`() =
        runTest(dispatcher) {
            val archive = FakeArchiveRepository()
            val model = viewModel(FakeActiveTripRepository(), archive)
            advanceUntilIdle()

            model.archiveTrip()
            advanceUntilIdle()

            assertEquals(0, archive.addCount)
        }

    private inner class FakeActiveTripRepository(
        initial: Trip? = null,
    ) : ActiveTripRepository {
        private val state = MutableStateFlow(initial)

        val current: Trip? get() = state.value

        override fun observe(): Flow<Trip?> = state

        override suspend fun get(): Trip? = state.value

        override suspend fun save(trip: Trip) {
            state.value = trip
        }

        override suspend fun clear() {
            operations += "clear"
            state.value = null
        }
    }

    private inner class FakeArchiveRepository(
        private val barriere: CompletableDeferred<Unit>? = null,
    ) : TripArchiveRepository {
        private val state = MutableStateFlow<List<Trip>>(emptyList())

        val trips: List<Trip> get() = state.value
        var addCount: Int = 0
            private set

        override fun observeAll(): Flow<List<Trip>> = state

        override suspend fun add(trip: Trip) {
            barriere?.await()
            operations += "archive"
            addCount++
            state.value = state.value + trip
        }

        override suspend fun delete(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun clear() {
            state.value = emptyList()
        }
    }
}
