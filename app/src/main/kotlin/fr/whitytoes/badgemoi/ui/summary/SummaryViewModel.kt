package fr.whitytoes.badgemoi.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Écran « Récapitulatif » (cahier des charges §3.3) : dernière relecture avant qu'un
 * trajet ne rejoigne l'archive.
 *
 * C'est ici que se referme le cycle « trajet → historique » (§7). Le
 * [TripArchiveRepository] existe et est testé depuis le lot 1, mais n'avait jusqu'ici
 * aucun appelant : aucun trajet n'était archivé.
 *
 * « Annuler » n'est pas une action de ce ViewModel : revenir à l'écran actif pour corriger
 * un jalon ne touche à rien, c'est de la seule navigation. Effacer un trajet reste une
 * action distincte, offerte depuis l'accueil sous le nom d'abandon.
 */
@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        private val activeTripRepository: ActiveTripRepository,
        private val archiveRepository: TripArchiveRepository,
    ) : ViewModel() {
        private val archiving = MutableStateFlow(false)

        val uiState: StateFlow<SummaryUiState> =
            combine(activeTripRepository.observe(), archiving) { trip, isArchiving ->
                if (trip == null) {
                    SummaryUiState.NoTrip
                } else {
                    SummaryUiState.Ready(trip = trip, archiving = isArchiving)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SummaryUiState.Loading,
            )

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
    }
