@file:Suppress("MagicNumber") // Données de test : indices de jalons et durées en clair.

package fr.whitytoes.badgemoi.ui.history

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes

/** Horloge figée : seul le nom du fichier d'export s'en sert. */
private val CLOCK: Clock = Clock.fixed(Instant.parse("2026-07-26T09:00:00Z"), ZoneOffset.UTC)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val day = Instant.parse("2026-07-26T07:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Trajet complet de [durationMinutes] minutes dans le sens demandé. */
    private fun trip(
        id: String,
        durationMinutes: Long,
        daysAgo: Long = 0,
        direction: Direction = Direction.ALLER,
    ): Trip {
        val departure = day.minusSeconds(daysAgo * 86_400)
        val start = Trip.start(id = id, direction = direction, departureAt = departure)
        return (1 until Routes.MILESTONE_COUNT).fold(start) { trip, index ->
            val offset = durationMinutes * index / (Routes.MILESTONE_COUNT - 1)
            trip.poseMilestone(index, departure.plusSeconds(offset * 60))
        }
    }

    private fun viewModel(archive: TripArchiveRepository) = HistoryViewModel(archive, CLOCK)

    private fun ready(model: HistoryViewModel) = model.uiState.value as HistoryUiState.Ready

    @Test
    fun `l'état initial est le chargement`() =
        runTest(dispatcher) {
            val model = viewModel(FakeArchiveRepository())

            assertEquals(HistoryUiState.Loading, model.uiState.value)
        }

    /**
     * L'archive vide n'a pas d'état à elle : c'est un [HistoryUiState.Ready] sans
     * trajet. Elle doit rester **distincte du chargement**, faute de quoi le premier
     * lancement annoncerait « aucun trajet » avant d'avoir lu le dépôt.
     */
    @Test
    fun `une archive vide donne des moyennes nulles et zéro trajet`() =
        runTest(dispatcher) {
            val model = viewModel(FakeArchiveRepository())

            advanceUntilIdle()

            val state = ready(model)
            assertEquals(0, state.statistics.tripCount)
            assertNull(state.statistics.totalAverage)
            assertEquals(emptyList<RecentTripRow>(), state.recentTrips)
        }

    @Test
    fun `les moyennes portent sur le sens sélectionné`() =
        runTest(dispatcher) {
            val model =
                viewModel(
                    FakeArchiveRepository(
                        trip("a", durationMinutes = 30),
                        trip("b", durationMinutes = 50, direction = Direction.RETOUR),
                    ),
                )

            advanceUntilIdle()

            assertEquals(30.minutes, ready(model).statistics.totalAverage)
        }

    /**
     * Critère du lot : basculer de sens est un recalcul en mémoire. Le dépôt n'est
     * collecté qu'une fois, `combine` réutilisant sa dernière valeur.
     */
    @Test
    fun `changer de sens recalcule sans relire le dépôt`() =
        runTest(dispatcher) {
            val archive =
                FakeArchiveRepository(
                    trip("a", durationMinutes = 30),
                    trip("b", durationMinutes = 50, direction = Direction.RETOUR),
                )
            val model = viewModel(archive)
            advanceUntilIdle()
            val collectionsBefore = archive.collectionCount

            model.selectDirection(Direction.RETOUR)
            advanceUntilIdle()

            assertEquals(50.minutes, ready(model).statistics.totalAverage)
            assertEquals(Direction.RETOUR, ready(model).statistics.direction)
            assertEquals("le dépôt n'est pas recollecté", collectionsBefore, archive.collectionCount)
        }

    @Test
    fun `purger vide l'archive et remet les statistiques à zéro`() =
        runTest(dispatcher) {
            val archive = FakeArchiveRepository(trip("a", durationMinutes = 30))
            val model = viewModel(archive)
            advanceUntilIdle()

            model.clearArchive()
            advanceUntilIdle()

            assertEquals(emptyList<Trip>(), archive.trips)
            assertEquals(0, ready(model).statistics.tripCount)
            assertNull(ready(model).statistics.totalAverage)
        }

    /**
     * Purge **partielle** : les moyennes doivent suivre le retrait. La propriété découle
     * de la réactivité du flux, ce qui se vérifie plutôt que se suppose.
     */
    @Test
    fun `supprimer un trajet ajuste la moyenne`() =
        runTest(dispatcher) {
            val archive =
                FakeArchiveRepository(
                    trip("court", durationMinutes = 20),
                    trip("long", durationMinutes = 40, daysAgo = 1),
                )
            val model = viewModel(archive)
            advanceUntilIdle()
            assertEquals(30.minutes, ready(model).statistics.totalAverage)

            archive.delete("long")
            advanceUntilIdle()

            assertEquals(20.minutes, ready(model).statistics.totalAverage)
            assertEquals(listOf("court"), ready(model).recentTrips.map { it.id })
        }

    @Test
    fun `un double appui ne purge qu'une fois`() =
        runTest(dispatcher) {
            val archive = FakeArchiveRepository(trip("a", durationMinutes = 30))
            val model = viewModel(archive)
            advanceUntilIdle()

            model.clearArchive()
            model.clearArchive()
            advanceUntilIdle()

            assertEquals(1, archive.clearCount)
        }

    /**
     * Le verrou doit être **visible** et pas seulement effectif, pour que l'écran puisse
     * désactiver le bouton. L'écriture est retenue par une barrière, sans quoi tout
     * s'enchaînerait dans le même tour et l'état intermédiaire serait inobservable.
     */
    @Test
    fun `la purge est signalée dans l'état, pour que le bouton se désactive`() =
        runTest(dispatcher) {
            val barriere = CompletableDeferred<Unit>()
            val archive = FakeArchiveRepository(trip("a", durationMinutes = 30), barriere = barriere)
            val model = viewModel(archive)
            advanceUntilIdle()

            model.clearArchive()
            advanceUntilIdle()

            assertTrue("pendant l'écriture", ready(model).purging)

            barriere.complete(Unit)
            advanceUntilIdle()

            assertEquals("une fois l'écriture finie", false, ready(model).purging)
        }

    @Test
    fun `les trajets récents suivent le sens sélectionné`() =
        runTest(dispatcher) {
            val model =
                viewModel(
                    FakeArchiveRepository(
                        trip("a", durationMinutes = 30),
                        trip("b", durationMinutes = 50, direction = Direction.RETOUR),
                    ),
                )
            advanceUntilIdle()

            assertEquals(listOf("a"), ready(model).recentTrips.map { it.id })

            model.selectDirection(Direction.RETOUR)
            advanceUntilIdle()

            assertEquals(listOf("b"), ready(model).recentTrips.map { it.id })
        }

    /**
     * L'export porte sur **toute** l'archive, les deux sens confondus, contrairement aux
     * statistiques. Le sens sélectionné ne doit donc rien y changer.
     */
    @Test
    fun `l'export couvre les deux sens, quel que soit celui affiché`() =
        runTest(dispatcher) {
            val model =
                viewModel(
                    FakeArchiveRepository(
                        trip("a", durationMinutes = 30),
                        trip("b", durationMinutes = 50, direction = Direction.RETOUR),
                    ),
                )
            advanceUntilIdle()
            model.selectDirection(Direction.ALLER)

            val csv = model.csvContent()

            assertTrue("le sens affiché", csv.contains("\"Aller\""))
            assertTrue("l'autre sens", csv.contains("\"Retour\""))
        }

    @Test
    fun `le nom du fichier vient de l'horloge`() =
        runTest(dispatcher) {
            val model = viewModel(FakeArchiveRepository())

            assertEquals("trajet-historique-2026-07-26.csv", model.csvFileName())
        }

    private class FakeArchiveRepository(
        vararg initial: Trip,
        private val barriere: CompletableDeferred<Unit>? = null,
    ) : TripArchiveRepository {
        private val state = MutableStateFlow(initial.toList())

        val trips: List<Trip> get() = state.value
        var clearCount: Int = 0
            private set

        /** Compte les **abonnements** : une bascule de sens ne doit pas en produire. */
        var collectionCount: Int = 0
            private set

        override fun observeAll(): Flow<List<Trip>> =
            object : Flow<List<Trip>> {
                override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<List<Trip>>) {
                    collectionCount++
                    state.collect(collector)
                }
            }

        override suspend fun add(trip: Trip) {
            state.value = state.value + trip
        }

        override suspend fun delete(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun clear() {
            barriere?.await()
            clearCount++
            state.value = emptyList()
        }
    }
}
