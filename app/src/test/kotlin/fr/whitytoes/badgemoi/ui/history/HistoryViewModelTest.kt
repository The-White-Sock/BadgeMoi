@file:Suppress("MagicNumber") // Données de test : indices de jalons et durées en clair.

package fr.whitytoes.badgemoi.ui.history

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    /** Deux sens dans l'archive : le socle de la plupart des cas. */
    private fun bothDirections() =
        FakeArchiveRepository(
            trip("a", durationMinutes = 30),
            trip("b", durationMinutes = 50, direction = Direction.RETOUR),
        )

    @Test
    fun `l'état initial est le chargement`() =
        runTest(dispatcher) {
            val model = viewModel(FakeArchiveRepository())

            assertEquals(HistoryUiState.Loading, model.uiState.value)
        }

    /**
     * L'archive vide n'a pas d'état à elle : c'est un [HistoryUiState.Ready] sans trajet.
     * Elle doit rester **distincte du chargement**, faute de quoi le premier lancement
     * annoncerait « aucun trajet » avant d'avoir lu le dépôt.
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
            val model = viewModel(bothDirections())

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
            val archive = bothDirections()
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
    fun `les trajets récents suivent le sens sélectionné`() =
        runTest(dispatcher) {
            val model = viewModel(bothDirections())
            advanceUntilIdle()

            assertEquals(listOf("a"), ready(model).recentTrips.map { it.id })

            model.selectDirection(Direction.RETOUR)
            advanceUntilIdle()

            assertEquals(listOf("b"), ready(model).recentTrips.map { it.id })
        }

    /**
     * Purge **partielle** : les moyennes doivent suivre le retrait. La propriété découle
     * de la réactivité du flux, ce qui se vérifie plutôt que se suppose.
     */
    @Test
    fun `retirer un trajet ajuste la moyenne`() =
        runTest(dispatcher) {
            val archive =
                FakeArchiveRepository(
                    trip("court", durationMinutes = 20),
                    trip("long", durationMinutes = 40, daysAgo = 1),
                )
            val model = viewModel(archive)
            advanceUntilIdle()
            assertEquals(30.minutes, ready(model).statistics.totalAverage)

            archive.delete(setOf("long"))
            advanceUntilIdle()

            assertEquals(20.minutes, ready(model).statistics.totalAverage)
            assertEquals(listOf("court"), ready(model).recentTrips.map { it.id })
        }

    // --- Mode sélection ---

    /**
     * Entrer dans le mode et n'y rien cocher sont deux états distincts, et l'écran doit
     * les distinguer : `null` ne montre aucune case, l'ensemble vide en montre.
     */
    @Test
    fun `le mode sélection s'ouvre vide et se distingue de son absence`() =
        runTest(dispatcher) {
            val model = viewModel(bothDirections())
            advanceUntilIdle()
            assertFalse("hors du mode au départ", ready(model).selecting)
            assertNull(ready(model).selectedIds)

            model.startSelection()
            advanceUntilIdle()

            assertTrue(ready(model).selecting)
            assertEquals(emptySet<String>(), ready(model).selectedIds)
        }

    @Test
    fun `cocher puis décocher un trajet`() =
        runTest(dispatcher) {
            val model = viewModel(bothDirections())
            advanceUntilIdle()
            model.startSelection()

            model.toggleTripSelection("a")
            advanceUntilIdle()
            assertEquals(setOf("a"), ready(model).selectedIds)

            model.toggleTripSelection("a")
            advanceUntilIdle()
            assertEquals(emptySet<String>(), ready(model).selectedIds)
        }

    /** Hors du mode, un appui sur une ligne ne coche rien : il ouvrira le trajet. */
    @Test
    fun `cocher est sans effet hors du mode sélection`() =
        runTest(dispatcher) {
            val model = viewModel(bothDirections())
            advanceUntilIdle()

            model.toggleTripSelection("a")
            advanceUntilIdle()

            assertNull(ready(model).selectedIds)
        }

    /**
     * « Tout sélectionner » remplace la purge par sens : il doit donc porter sur
     * **l'ensemble du sens**, y compris les trajets au-delà des dix affichés.
     */
    @Test
    fun `tout sélectionner couvre le sens entier, au-delà des dix affichés`() =
        runTest(dispatcher) {
            val trips = (0 until 14).map { trip("t$it", durationMinutes = 30, daysAgo = it.toLong()) }
            val archive = FakeArchiveRepository(*trips.toTypedArray())
            val model = viewModel(archive)
            advanceUntilIdle()
            assertEquals("la liste n'en montre que dix", 10, ready(model).recentTrips.size)

            model.startSelection()
            model.selectAllTrips()
            advanceUntilIdle()

            assertEquals(14, ready(model).selectedIds?.size)
        }

    @Test
    fun `tout sélectionner ne déborde pas sur l'autre sens`() =
        runTest(dispatcher) {
            val model = viewModel(bothDirections())
            advanceUntilIdle()
            model.startSelection()

            model.selectAllTrips()
            advanceUntilIdle()

            assertEquals(setOf("a"), ready(model).selectedIds)
        }

    @Test
    fun `supprimer retire les trajets cochés et sort du mode`() =
        runTest(dispatcher) {
            val archive =
                FakeArchiveRepository(
                    trip("garde", durationMinutes = 20),
                    trip("jette", durationMinutes = 40, daysAgo = 1),
                )
            val model = viewModel(archive)
            advanceUntilIdle()
            model.startSelection()
            model.toggleTripSelection("jette")

            model.deleteSelectedTrips()
            advanceUntilIdle()

            assertEquals(listOf("jette"), archive.deletedIds)
            assertEquals(listOf("garde"), archive.trips.map { it.id })
            assertNull("le mode se referme", ready(model).selectedIds)
        }

    @Test
    fun `supprimer sans rien de coché ne touche pas à l'archive`() =
        runTest(dispatcher) {
            val archive = bothDirections()
            val model = viewModel(archive)
            advanceUntilIdle()
            model.startSelection()

            model.deleteSelectedTrips()
            advanceUntilIdle()

            assertEquals(emptyList<String>(), archive.deletedIds)
            assertTrue("on reste en mode sélection", ready(model).selecting)
        }

    @Test
    fun `annuler la sélection ne détruit rien`() =
        runTest(dispatcher) {
            val archive = bothDirections()
            val model = viewModel(archive)
            advanceUntilIdle()
            model.startSelection()
            model.toggleTripSelection("a")

            model.cancelSelection()
            advanceUntilIdle()

            assertEquals(emptyList<String>(), archive.deletedIds)
            assertNull(ready(model).selectedIds)
        }

    /**
     * Une sélection oubliée sur l'Aller ne doit pas survivre au passage sur le Retour :
     * le prochain « Supprimer » détruirait alors des trajets qu'on ne regarde plus.
     */
    @Test
    fun `changer de sens vide la sélection`() =
        runTest(dispatcher) {
            val model = viewModel(bothDirections())
            advanceUntilIdle()
            model.startSelection()
            model.toggleTripSelection("a")

            model.selectDirection(Direction.RETOUR)
            advanceUntilIdle()

            assertNull(ready(model).selectedIds)
        }

    // --- Export ---

    /**
     * L'export porte sur **toute** l'archive, les deux sens confondus, contrairement aux
     * statistiques. Le sens sélectionné ne doit donc rien y changer.
     */
    @Test
    fun `l'export couvre les deux sens, quel que soit celui affiché`() =
        runTest(dispatcher) {
            val model = viewModel(bothDirections())
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
    ) : TripArchiveRepository {
        private val state = MutableStateFlow(initial.toList())

        val trips: List<Trip> get() = state.value

        /** Identifiants effectivement supprimés, dans l'ordre des appels. */
        val deletedIds = mutableListOf<String>()

        /** Compte les **abonnements** : une bascule de sens ne doit pas en produire. */
        var collectionCount: Int = 0
            private set

        override fun observeAll(): Flow<List<Trip>> =
            object : Flow<List<Trip>> {
                override suspend fun collect(collector: FlowCollector<List<Trip>>) {
                    collectionCount++
                    state.collect(collector)
                }
            }

        override suspend fun add(trip: Trip) {
            state.value = state.value + trip
        }

        override suspend fun delete(ids: Collection<String>) {
            deletedIds += ids
            state.value = state.value.filterNot { it.id in ids }
        }
    }
}
