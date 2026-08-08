@file:Suppress("MagicNumber") // Données de test : indices de jalons en clair.

package fr.whitytoes.badgemoi.ui.summary

import androidx.lifecycle.SavedStateHandle
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Horloge figée : la correction d'un jalon en a besoin, l'archivage non. */
private val CLOCK: Clock = Clock.fixed(Instant.parse("2026-07-26T09:00:00Z"), ZoneOffset.UTC)

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
    ) = SummaryViewModel(active, archive, CLOCK, SavedStateHandle())

    /** Le même écran, mais ouvert sur un trajet **archivé** : la route porte son identifiant. */
    private fun archivedViewModel(
        active: FakeActiveTripRepository,
        archive: FakeArchiveRepository,
        tripId: String,
    ) = SummaryViewModel(active, archive, CLOCK, SavedStateHandle(mapOf(ARCHIVED_TRIP_ID_KEY to tripId)))

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

    /**
     * Second bouton de l'écran, modèle du POC : `sumDiscard` appelle `discardTrip`, qui
     * jette le trajet. Rien ne part à l'archive.
     */
    @Test
    fun `abandonner efface le trajet sans l'archiver`() =
        runTest(dispatcher) {
            val active = FakeActiveTripRepository(completedTrip())
            val archive = FakeArchiveRepository()
            val model = viewModel(active, archive)
            advanceUntilIdle()

            model.discardTrip()
            advanceUntilIdle()

            assertEquals("rien n'est archivé", 0, archive.addCount)
            assertEquals("le trajet en cours est effacé", null, active.current)
            assertEquals(SummaryUiState.NoTrip, model.uiState.value)
        }

    /**
     * La correction se fait **sur place** depuis le récapitulatif, les lignes y étant
     * cliquables comme sur l'écran actif. Elle passe par la même mécanique que là-bas.
     */
    @Test
    fun `corriger un jalon depuis le récapitulatif met le trajet à jour`() =
        runTest(dispatcher) {
            val active = FakeActiveTripRepository(completedTrip())
            val model = viewModel(active, FakeArchiveRepository())
            advanceUntilIdle()

            // 08h30 UTC, soit trente minutes après le départ — et avant l'horloge (09h00).
            model.correctMilestone(index = 2, hour = 8, minute = 30)
            advanceUntilIdle()

            assertEquals(departure.plusSeconds(1_800), active.current?.times?.get(2))
        }

    @Test
    fun `ignorer un jalon depuis le récapitulatif le prive de son horodatage`() =
        runTest(dispatcher) {
            val active = FakeActiveTripRepository(completedTrip())
            val model = viewModel(active, FakeArchiveRepository())
            advanceUntilIdle()

            model.skipMilestone(3)
            advanceUntilIdle()

            assertEquals(null, active.current?.times?.get(3))
            assertTrue(active.current?.skipped?.get(3) == true)
        }

    /**
     * Effacer un jalon rend le trajet **incomplet** : il n'est alors plus archivable, et
     * le garde-fou de [SummaryViewModel.archiveTrip] doit le refuser.
     */
    @Test
    fun `effacer un jalon depuis le récapitulatif rend le trajet non archivable`() =
        runTest(dispatcher) {
            val active = FakeActiveTripRepository(completedTrip())
            val archive = FakeArchiveRepository()
            val model = viewModel(active, archive)
            advanceUntilIdle()

            model.clearMilestone(2)
            advanceUntilIdle()
            model.archiveTrip()
            advanceUntilIdle()

            assertEquals(0, archive.addCount)
            assertEquals("le trajet reste à compléter", 2, active.current?.currentStep)
        }

    // --- Trajet archivé, rouvert depuis l'historique ---

    @Test
    fun `un trajet archivé est relu depuis l'archive, pas depuis le trajet en cours`() =
        runTest(dispatcher) {
            val archive = FakeArchiveRepository()
            val archivedTrip = completedTrip().copy(id = "archived")
            archive.add(archivedTrip)
            val model = archivedViewModel(FakeActiveTripRepository(), archive, "archived")

            advanceUntilIdle()

            val state = model.uiState.value as SummaryUiState.Ready
            assertEquals(archivedTrip, state.trip)
            assertTrue("le mode remonte à l'écran", state.archived)
        }

    /**
     * Le point de la seconde moitié de #108 : corriger un jalon après coup doit modifier
     * **l'archive**, sans quoi la correction serait perdue et les moyennes fausses.
     */
    @Test
    fun `corriger un jalon archivé écrit dans l'archive`() =
        runTest(dispatcher) {
            val archive = FakeArchiveRepository()
            archive.add(completedTrip().copy(id = "archived"))
            val active = FakeActiveTripRepository()
            val model = archivedViewModel(active, archive, "archived")
            advanceUntilIdle()

            // 08h30 UTC, soit trente minutes après le départ — et avant l'horloge (09h00).
            model.correctMilestone(index = 2, hour = 8, minute = 30)
            advanceUntilIdle()

            assertEquals(departure.plusSeconds(1_800), archive.trips.single().times[2])
            assertEquals("le trajet en cours n'est pas touché", null, active.current)
        }

    @Test
    fun `supprimer un trajet archivé le retire de l'archive`() =
        runTest(dispatcher) {
            val archive = FakeArchiveRepository()
            archive.add(completedTrip().copy(id = "archived"))
            val model = archivedViewModel(FakeActiveTripRepository(), archive, "archived")
            advanceUntilIdle()

            model.deleteArchivedTrip()
            advanceUntilIdle()

            assertEquals(emptyList<Trip>(), archive.trips)
            assertEquals("l'écran n'a plus d'objet", SummaryUiState.NoTrip, model.uiState.value)
        }

    /** Garde-fou : un trajet en cours s'abandonne, il ne se supprime pas. */
    @Test
    fun `supprimer est sans effet sur un trajet en cours`() =
        runTest(dispatcher) {
            val active = FakeActiveTripRepository(completedTrip())
            val model = viewModel(active, FakeArchiveRepository())
            advanceUntilIdle()

            model.deleteArchivedTrip()
            advanceUntilIdle()

            assertEquals(completedTrip(), active.current)
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

        /** Même sémantique que `DataStoreActiveTripRepository` : n'écrit que sur un dépôt vide. */
        override suspend fun saveIfNoneInProgress(trip: Trip): Boolean {
            if (state.value != null) return false
            save(trip)
            return true
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
            // Remplacement sur conflit d'identifiant, comme `OnConflictStrategy.REPLACE`
            // du DAO : c'est ce qui fait qu'enregistrer un trajet déjà archivé le met à
            // jour au lieu de le dupliquer. Un double qui empilerait laisserait passer
            // une correction perdue.
            state.value = state.value.filterNot { it.id == trip.id } + trip
        }

        override suspend fun delete(ids: Collection<String>) {
            state.value = state.value.filterNot { it.id in ids }
        }
    }
}
