package fr.whitytoes.badgemoi.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import fr.whitytoes.badgemoi.domain.TripStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Écran Historique (cahier des charges §3.4) : moyennes par sens, trajets récents et
 * purge de l'archive.
 *
 * Tout le calcul vient du domaine (lot 1) : `TripStatistics.forDirection` produit les
 * moyennes, ce ViewModel se contente de les recombiner avec le sens sélectionné.
 *
 * Le sens est un **état d'écran** et non de navigation : le changer ne relit pas le
 * dépôt. `combine` garde une seule collecte de `observeAll()` et réutilise sa dernière
 * valeur, si bien qu'une bascule Aller / Retour est un simple recalcul en mémoire.
 */
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val archiveRepository: TripArchiveRepository,
    ) : ViewModel() {
        private val selectedDirection = MutableStateFlow(Direction.ALLER)

        /**
         * Verrou de purge, exposé dans l'état.
         *
         * Même garde-fou que l'archivage du lot 4 : sans lui, deux appuis rapprochés
         * lanceraient deux écritures. Ici la seconde ne détruirait rien de plus — l'archive
         * est déjà vide — mais le bouton doit tout de même cesser de répondre pendant
         * l'écriture, faute de quoi l'appui suivant semble ignoré.
         */
        private val purging = MutableStateFlow(false)

        val uiState: StateFlow<HistoryUiState> =
            combine(
                archiveRepository.observeAll(),
                selectedDirection,
                purging,
            ) { trips, direction, isPurging ->
                val statistics = TripStatistics.forDirection(trips, direction)
                HistoryUiState.Ready(
                    statistics = statistics,
                    // La moyenne vient des statistiques qu'on vient de calculer : la
                    // recalculer pour les lignes ferait deux vérités d'une seule.
                    recentTrips = trips.recentTripRows(direction, statistics.totalAverage),
                    purging = isPurging,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = HistoryUiState.Loading,
            )

        /** Bascule le sens affiché. Aucune relecture du dépôt : voir la note de classe. */
        fun selectDirection(direction: Direction) {
            selectedDirection.value = direction
        }

        /**
         * Vide l'archive.
         *
         * La confirmation appartient à l'écran — le bouton du POC s'arme au premier appui
         * et se désarme seul au bout de trois secondes. Ce verrou-ci est d'une autre
         * nature : il protège l'écriture, pas la décision.
         */
        fun clearArchive() {
            if (!purging.compareAndSet(expect = false, update = true)) return
            viewModelScope.launch {
                try {
                    archiveRepository.clear()
                } finally {
                    purging.value = false
                }
            }
        }
    }
