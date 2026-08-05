package fr.whitytoes.badgemoi.ui.history

import fr.whitytoes.badgemoi.domain.DirectionStatistics

/**
 * État de l'écran Historique (cahier des charges §3.4).
 *
 * [Loading] est distinct d'une archive vide : la première est une ignorance passagère, la
 * seconde un fait — et c'est le premier écran que voit un nouvel utilisateur. Les
 * confondre afficherait « aucun trajet » avant même d'avoir lu le dépôt.
 *
 * L'archive vide n'a pas d'état à elle : elle se lit dans [Ready], où `tripCount` vaut
 * zéro et les moyennes `null`. C'est la forme que `TripStatistics.forDirection` produit
 * naturellement, et lui inventer un état séparé obligerait l'écran à traiter deux fois le
 * même cas.
 */
sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    /**
     * @property statistics moyennes du sens sélectionné — lequel s'y lit, inutile de le
     *   transporter deux fois.
     * @property recentTrips les dix derniers trajets de ce sens, du plus récent au plus
     *   ancien.
     * @property purging une purge est en cours d'écriture. Rend le verrou du ViewModel
     *   **visible**, pour que l'écran puisse désactiver le bouton plutôt que d'ignorer
     *   silencieusement l'appui suivant.
     */
    data class Ready(
        val statistics: DirectionStatistics,
        val recentTrips: List<RecentTripRow>,
        val purging: Boolean = false,
    ) : HistoryUiState
}
