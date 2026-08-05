package fr.whitytoes.badgemoi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Écran d'accueil : démarrage d'un trajet (cahier des charges §3.1). S'appuie sur
 * l'[ActiveTripRepository] du lot 1, seule source de vérité — partagée avec le
 * widget (§4.2).
 *
 * L'état [HomeUiState.TripInProgress] ne sert plus à afficher quoi que ce soit : il
 * déclenche la **redirection** vers l'écran des jalons, où la reprise et l'abandon se
 * décident désormais (§9, écart 10).
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val activeTripRepository: ActiveTripRepository,
        private val clock: Clock,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            activeTripRepository
                .observe()
                .map { trip -> if (trip == null) HomeUiState.Idle else HomeUiState.TripInProgress(trip) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = HomeUiState.Loading,
                )

        /**
         * Démarre un trajet dans le sens demandé.
         *
         * Sans effet si un trajet est déjà en cours : l'application s'utilise en
         * roulant, et un double appui ne doit pas effacer un trajet déjà entamé. Sans
         * effet également tant que l'état n'est pas chargé, pour la même raison.
         */
        fun startTrip(direction: Direction) {
            if (uiState.value != HomeUiState.Idle) return
            viewModelScope.launch {
                activeTripRepository.save(
                    Trip.start(
                        id = UUID.randomUUID().toString(),
                        direction = direction,
                        departureAt = clock.instant(),
                    ),
                )
            }
        }
    }
