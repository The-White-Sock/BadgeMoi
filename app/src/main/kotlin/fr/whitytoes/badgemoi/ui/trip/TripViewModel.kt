package fr.whitytoes.badgemoi.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Écran « Trajet actif » (cahier des charges §3.2).
 *
 * Toute la manipulation d'un trajet vient du domaine (lot 1) : ce ViewModel se contente
 * d'appliquer `poseMilestone` / `skipMilestone` / `clearMilestone` et de persister le
 * résultat. Chaque action est écrite immédiatement dans l'[ActiveTripRepository] :
 * l'application peut être tuée à tout moment pendant un trajet.
 */
@HiltViewModel
class TripViewModel
    @Inject
    constructor(
        private val activeTripRepository: ActiveTripRepository,
        private val clock: Clock,
    ) : ViewModel() {
        val uiState: StateFlow<TripUiState> =
            activeTripRepository
                .observe()
                .map { trip -> if (trip == null) TripUiState.NoTrip else TripUiState.Active(trip) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = TripUiState.Loading,
                )

        /** Pose le jalon courant à l'heure de l'horloge. Sans effet si le trajet est terminé. */
        fun validateCurrentMilestone() {
            updateTrip { trip ->
                if (trip.isComplete) null else trip.poseMilestone(trip.currentStep, clock.instant())
            }
        }

        /** Ignore le jalon courant. Sans effet si le trajet est terminé. */
        fun skipCurrentMilestone() {
            updateTrip { trip ->
                if (trip.isComplete) null else trip.skipMilestone(trip.currentStep)
            }
        }

        /** Corrige l'heure d'un jalon quelconque (cahier §3.5). */
        fun poseMilestone(
            index: Int,
            at: Instant,
        ) {
            updateTrip { trip -> trip.poseMilestone(index, at) }
        }

        /** Marque un jalon quelconque comme ignoré (cahier §3.5). */
        fun skipMilestone(index: Int) {
            updateTrip { trip -> trip.skipMilestone(index) }
        }

        /** Remet un jalon quelconque en attente (cahier §3.5). */
        fun clearMilestone(index: Int) {
            updateTrip { trip -> trip.clearMilestone(index) }
        }

        /**
         * Applique [transform] au trajet en cours et persiste le résultat.
         *
         * Sans effet si aucun trajet n'est chargé, ou si [transform] renvoie `null` —
         * ce qui permet à chaque action de décliner sans dupliquer la garde.
         */
        private fun updateTrip(transform: (Trip) -> Trip?) {
            val current = (uiState.value as? TripUiState.Active)?.trip ?: return
            val updated = transform(current) ?: return
            viewModelScope.launch { activeTripRepository.save(updated) }
        }
    }
