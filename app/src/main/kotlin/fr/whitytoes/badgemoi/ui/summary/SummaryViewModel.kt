package fr.whitytoes.badgemoi.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import fr.whitytoes.badgemoi.ui.trip.ActiveTripStore
import fr.whitytoes.badgemoi.ui.trip.ArchivedTripStore
import fr.whitytoes.badgemoi.ui.trip.MilestoneCorrections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Nom de l'argument de route portant l'identifiant d'un trajet archivé.
 *
 * Lu depuis le [SavedStateHandle] plutôt qu'en désérialisant la route : le ViewModel n'a
 * pas à connaître le graphe de navigation pour savoir sur quel trajet il travaille.
 */
internal const val ARCHIVED_TRIP_ID_KEY = "tripId"

/**
 * Écran « Récapitulatif » (cahier des charges §3.3) : dernière relecture avant qu'un
 * trajet ne rejoigne l'archive.
 *
 * C'est ici que se referme le cycle « trajet → historique » (§7). Le
 * [TripArchiveRepository] existe et est testé depuis le lot 1, mais n'avait jusqu'ici
 * aucun appelant : aucun trajet n'était archivé.
 *
 * Les deux issues de l'écran sont ici : **archiver** le trajet, ou l'**abandonner**. C'est
 * le modèle du POC, dont `sumDiscard` appelle `discardTrip` — le second bouton jette le
 * trajet, il ne ramène pas en arrière. La correction d'un jalon se fait **sur place**,
 * les lignes du récapitulatif étant cliquables comme celles de l'écran actif.
 *
 * ## Deux modes
 *
 * Le même écran sert à relire un trajet **en cours** et à rouvrir un trajet **archivé**
 * depuis l'historique. La route porte l'identifiant dans le second cas, et c'est lui qui
 * décide de tout : la source lue, l'endroit où les corrections s'écrivent, et la nature de
 * l'action destructive — abandonner un trajet qu'on n'a pas encore rangé, ou en supprimer
 * un de l'archive.
 *
 * Les corrections restent **écrites aussitôt** dans les deux cas. C'est le principe posé
 * au lot 3 : chaque action est persistée sur-le-champ, l'application pouvant être tuée à
 * tout moment. Un bouton qui validerait un lot de corrections introduirait un état non
 * persisté que rien d'autre dans l'application ne connaît.
 */
@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        private val activeTripRepository: ActiveTripRepository,
        private val archiveRepository: TripArchiveRepository,
        clock: Clock,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        /** Identifiant du trajet archivé rouvert, `null` pour le trajet en cours. */
        private val archivedTripId: String? = savedStateHandle[ARCHIVED_TRIP_ID_KEY]

        private val corrections =
            MilestoneCorrections(
                store =
                    archivedTripId
                        ?.let { ArchivedTripStore(archiveRepository, it) }
                        ?: ActiveTripStore(activeTripRepository),
                clock = clock,
            )

        private val archiving = MutableStateFlow(false)

        val uiState: StateFlow<SummaryUiState> =
            trips()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = SummaryUiState.Loading,
                )

        /**
         * Le trajet observé, selon le mode.
         *
         * Un trajet archivé disparu — supprimé à l'instant — rend [SummaryUiState.NoTrip]
         * comme le ferait un trajet en cours effacé : l'écran n'a plus d'objet, et
         * l'appelant sait où revenir.
         */
        private fun trips(): Flow<SummaryUiState> =
            if (archivedTripId != null) {
                archiveRepository.observeAll().map { archive ->
                    archive
                        .firstOrNull { it.id == archivedTripId }
                        ?.let { SummaryUiState.Ready(trip = it, archived = true) }
                        ?: SummaryUiState.NoTrip
                }
            } else {
                combine(activeTripRepository.observe(), archiving) { trip, isArchiving ->
                    if (trip == null) {
                        SummaryUiState.NoTrip
                    } else {
                        SummaryUiState.Ready(trip = trip, archiving = isArchiving)
                    }
                }
            }

        /**
         * Archive le trajet relu, puis vide le trajet en cours.
         *
         * **L'ordre est le garde-fou** : si l'écriture en archive échoue, le trajet en
         * cours est toujours là et l'utilisateur peut réessayer. L'ordre inverse le
         * perdrait pour de bon.
         *
         * Trois refus, tous silencieux — l'application s'utilise en roulant, et une alerte
         * sur un geste sans conséquence gênerait plus qu'elle n'aiderait :
         *
         * - pas de trajet chargé, ou déjà archivé ;
         * - trajet **incomplet** : il reste des jalons à traiter, sa place est sur l'écran
         *   actif, pas dans l'archive ;
         * - archivage **déjà en cours** : `compareAndSet` ne laisse passer que le premier
         *   appui, les suivants tombent ici. Sans ce verrou, un double appui lancerait deux
         *   écritures.
         */
        fun archiveTrip() {
            val trip = (uiState.value as? SummaryUiState.Ready)?.trip ?: return
            // L'évaluation paresseuse compte : le verrou n'est pris que si le trajet est
            // bien archivable, sinon un trajet incomplet le poserait pour rien.
            if (!trip.isComplete || !archiving.compareAndSet(expect = false, update = true)) return

            viewModelScope.launch {
                try {
                    archiveRepository.add(trip)
                    activeTripRepository.clear()
                } finally {
                    // Rendu quelle que soit l'issue, pour qu'un échec d'écriture ne laisse
                    // pas le verrou coincé. Après un succès il ne sert plus à rien : le
                    // trajet en cours est vide, l'état est passé à [SummaryUiState.NoTrip]
                    // et c'est le premier refus ci-dessus qui prend le relais.
                    archiving.value = false
                }
            }
        }

        /**
         * Abandonne le trajet relu, sans l'archiver (`discardTrip` du POC).
         *
         * Irréversible, et l'écran est atteint **automatiquement** en fin de parcours :
         * l'appelant demande confirmation, comme le fait déjà l'abandon depuis l'accueil.
         */
        fun discardTrip() {
            viewModelScope.launch { activeTripRepository.clear() }
        }

        /**
         * Retire de l'archive le trajet rouvert.
         *
         * Sans effet sur un trajet en cours : celui-ci s'abandonne, il ne se supprime pas —
         * il n'a jamais été rangé nulle part.
         */
        fun deleteArchivedTrip() {
            val id = archivedTripId ?: return
            viewModelScope.launch { archiveRepository.delete(setOf(id)) }
        }

        /**
         * Corrections d'un jalon depuis le récapitulatif (cahier §3.5).
         *
         * Partagées avec l'écran actif : la relecture n'a pas de règle propre, corriger
         * ici ou là-bas doit produire le même trajet.
         */
        fun correctMilestone(
            index: Int,
            hour: Int,
            minute: Int,
        ) = correct { corrections.correct(index, hour, minute) }

        fun skipMilestone(index: Int) = correct { corrections.skip(index) }

        fun clearMilestone(index: Int) = correct { corrections.clear(index) }

        private fun correct(action: suspend () -> Unit) {
            viewModelScope.launch { action() }
        }
    }
