package fr.whitytoes.badgemoi.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import fr.whitytoes.badgemoi.domain.TripCsv
import fr.whitytoes.badgemoi.domain.TripStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Écran Historique (cahier des charges §3.4) : moyennes par sens, trajets récents,
 * export et suppression par sélection.
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
        private val clock: Clock,
    ) : ViewModel() {
        private val selectedDirection = MutableStateFlow(Direction.ALLER)

        /**
         * Trajets cochés, `null` hors du mode sélection.
         *
         * Vit ici et non dans l'écran parce que « Tout sélectionner » porte sur **tout le
         * sens**, y compris les trajets que la liste des dix derniers ne montre pas :
         * l'écran ne connaît pas cet ensemble.
         */
        private val selectedIds = MutableStateFlow<Set<String>?>(null)

        val uiState: StateFlow<HistoryUiState> =
            combine(
                archiveRepository.observeAll(),
                selectedDirection,
                selectedIds,
            ) { trips, direction, selection ->
                val statistics = TripStatistics.forDirection(trips, direction)
                HistoryUiState.Ready(
                    statistics = statistics,
                    // La moyenne vient des statistiques qu'on vient de calculer : la
                    // recalculer pour les lignes ferait deux vérités d'une seule.
                    recentTrips = trips.recentTripRows(direction, statistics.totalAverage),
                    selectedIds = selection,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = HistoryUiState.Loading,
            )

        /**
         * Bascule le sens affiché. Aucune relecture du dépôt : voir la note de classe.
         *
         * La sélection retombe : la garder ferait détruire, au prochain « Supprimer », des
         * trajets cochés sur un sens qu'on ne regarde plus.
         */
        fun selectDirection(direction: Direction) {
            selectedDirection.value = direction
            selectedIds.value = null
        }

        /** Entre en mode sélection, sans rien cocher. */
        fun startSelection() {
            selectedIds.value = emptySet()
        }

        /** En sort, sans rien détruire. Tout ce qui était coché est oublié. */
        fun cancelSelection() {
            selectedIds.value = null
        }

        fun toggleTripSelection(id: String) {
            selectedIds.value = selectedIds.value?.let { if (id in it) it - id else it + id }
        }

        /**
         * Coche **tous** les trajets du sens, au-delà des dix affichés.
         *
         * C'est ce qui remplace la purge par sens : la sélection en est le seul chemin,
         * et « Supprimer » n'a donc qu'une mécanique à connaître.
         */
        fun selectAllTrips() {
            viewModelScope.launch {
                val direction = selectedDirection.value
                selectedIds.value =
                    archiveRepository
                        .observeAll()
                        .first()
                        .filter { it.direction == direction }
                        .map { it.id }
                        .toSet()
            }
        }

        /**
         * Retire les trajets cochés, et sort du mode sélection.
         *
         * La sélection est vidée **avant** l'écriture : un second appui n'a alors plus rien
         * à supprimer, ce qui rend inutile le verrou que demandait la purge.
         */
        fun deleteSelectedTrips() {
            val ids = selectedIds.value.orEmpty()
            if (ids.isEmpty()) return
            selectedIds.value = null
            viewModelScope.launch { archiveRepository.delete(ids) }
        }

        /**
         * Nom proposé au sélecteur de fichier.
         *
         * Ne dépend que de l'horloge : il est calculé avant l'ouverture du sélecteur, donc
         * sur le fil principal, sans toucher à l'archive.
         */
        fun csvFileName(): String = TripCsv.fileName(clock.instant(), clock.zone)

        /**
         * Contenu du fichier : **toute** l'archive, les deux sens confondus, contrairement
         * aux statistiques qui sont par sens.
         *
         * Suspendue et lue depuis le dépôt plutôt que depuis un état mémorisé : conserver
         * la dernière liste dans un `StateFlow` obligerait à lui donner une valeur
         * initiale, ce qui ferait passer l'écran de [HistoryUiState.Loading] à une archive
         * vide avant même la première lecture.
         */
        suspend fun csvContent(): String = TripCsv.serialize(archiveRepository.observeAll().first(), clock.zone)
    }
