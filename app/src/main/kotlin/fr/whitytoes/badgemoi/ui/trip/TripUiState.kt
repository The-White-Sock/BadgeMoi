package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Trip

/**
 * États de l'écran « Trajet actif » (cahier des charges §3.2).
 *
 * [Loading] est distinct de [NoTrip] : tant que le DataStore n'a pas répondu, on ignore
 * s'il existe un trajet. Les confondre renverrait l'utilisateur à l'accueil à chaque
 * ouverture de l'écran, avant même que le trajet en cours ne soit lu.
 */
sealed interface TripUiState {
    data object Loading : TripUiState

    /** Aucun trajet : l'écran n'a plus lieu d'être, il faut revenir à l'accueil. */
    data object NoTrip : TripUiState

    data class Active(
        val trip: Trip,
    ) : TripUiState
}
