package fr.whitytoes.badgemoi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.StartTrip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        activeTripRepository: ActiveTripRepository,
        private val startTripUseCase: StartTrip,
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
         * Démarre un trajet dans le sens demandé, en déléguant au cas d'usage
         * [StartTrip] — la même règle que celle qu'appelle le widget (#114), et non
         * une seconde copie qui divergerait.
         *
         * L'ancienne garde `uiState.value != Idle` a disparu, et ce n'est pas un oubli :
         * elle relisait un état en mémoire avant d'écrire, ce qui laissait une fenêtre
         * entre la lecture et l'écriture. La garde vit désormais dans l'écriture
         * elle-même ([ActiveTripRepository.saveIfNoneInProgress]), ce qui couvre
         * strictement plus de cas — y compris l'appui survenu avant que l'état soit
         * chargé, qui ne peut plus écraser un trajet en cours.
         */
        fun startTrip(direction: Direction) {
            viewModelScope.launch { startTripUseCase(direction) }
        }
    }
