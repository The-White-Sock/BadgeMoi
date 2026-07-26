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
 * Écran d'accueil : démarrage d'un trajet, reprise ou abandon de celui en cours
 * (cahier des charges §3.1). S'appuie sur l'[ActiveTripRepository] du lot 1, seule
 * source de vérité — partagée avec le widget (§4.2).
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

        /** Abandonne le trajet en cours. L'écran demande confirmation au préalable. */
        fun abandonTrip() {
            viewModelScope.launch { activeTripRepository.clear() }
        }
    }
