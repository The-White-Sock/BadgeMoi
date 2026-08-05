package fr.whitytoes.badgemoi.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** Cadence de rafraîchissement des compteurs (cahier §3.2 : mise à jour à la seconde). */
private val TICK = 1.seconds

/** Délai avant d'arrêter de cadencer quand l'écran n'est plus observé. */
private const val STOP_TICKING_AFTER_MILLIS = 5_000L

/**
 * Écran « Trajet actif » (cahier des charges §3.2).
 *
 * Toute la manipulation d'un trajet vient du domaine (lot 1) : ce ViewModel se contente
 * d'appliquer `poseMilestone` / `skipMilestone` / `clearMilestone` et de persister le
 * résultat. Chaque action est écrite immédiatement dans l'[ActiveTripRepository] :
 * l'application peut être tuée à tout moment pendant un trajet.
 */

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TripViewModel
    @Inject
    constructor(
        private val activeTripRepository: ActiveTripRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val corrections = MilestoneCorrections(ActiveTripStore(activeTripRepository), clock)

        val uiState: StateFlow<TripUiState> =
            activeTripRepository
                .observe()
                .map { trip -> if (trip == null) TripUiState.NoTrip else TripUiState.Active(trip) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = TripUiState.Loading,
                )

        /**
         * Compteurs de l'écran, réévalués chaque seconde.
         *
         * Exposés séparément de [uiState] pour que la recomposition à la seconde reste
         * cantonnée au bandeau : recomposer tout l'écran, frise et liste comprises, à
         * chaque tic serait gaspiller la batterie d'un appareil dont l'écran reste
         * volontairement allumé.
         *
         * Le cadencement s'arrête de lui-même quand le trajet est terminé — les
         * compteurs sont alors figés — et quand plus personne n'observe l'écran.
         */
        val timers: StateFlow<TripTimers> =
            uiState
                .flatMapLatest { state ->
                    val trip = (state as? TripUiState.Active)?.trip
                    when {
                        trip == null -> flowOf(TripTimers.EMPTY)
                        trip.isComplete -> flowOf(trip.timersAt(clock.instant()))
                        else -> ticking(trip)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TICKING_AFTER_MILLIS),
                    initialValue = TripTimers.EMPTY,
                )

        private fun ticking(trip: Trip): Flow<TripTimers> =
            flow {
                while (true) {
                    emit(trip.timersAt(clock.instant()))
                    delay(TICK)
                }
            }

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

        /**
         * Corrections d'un jalon quelconque (cahier §3.5).
         *
         * Déléguées à [MilestoneCorrections], que le récapitulatif partage : le POC rend
         * les lignes de jalon cliquables sur les deux écrans.
         */
        fun poseMilestone(
            index: Int,
            at: Instant,
        ) = correct { corrections.pose(index, at) }

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

        /**
         * Abandonne le trajet en cours : il est effacé sans rejoindre l'archive.
         *
         * L'action est offerte par la fenêtre de reprise, qui demande confirmation au
         * préalable. Elle vivait à l'accueil ; elle a suivi la fenêtre (§9, écart 10).
         */
        fun abandonTrip() {
            viewModelScope.launch { activeTripRepository.clear() }
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
