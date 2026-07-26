package fr.whitytoes.badgemoi.ui.home

import fr.whitytoes.badgemoi.domain.Trip

/**
 * États de l'écran d'accueil (cahier des charges §3.1).
 *
 * [Loading] est distinct de [Idle] : tant que le DataStore n'a pas répondu, on ignore
 * s'il existe un trajet en cours. Les confondre ferait clignoter les boutons de
 * démarrage avant l'affichage de la bannière de reprise.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** Aucun trajet en cours : proposer un démarrage Aller ou Retour. */
    data object Idle : HomeUiState

    /** Un trajet est en cours : proposer de le reprendre ou de l'abandonner. */
    data class TripInProgress(
        val trip: Trip,
    ) : HomeUiState
}
