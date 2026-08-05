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
     * @property selectedIds identifiants cochés en **mode sélection**. `null` hors de ce
     *   mode : distinguer l'absence de mode d'une sélection vide est ce qui permet à
     *   l'écran de savoir s'il doit afficher des coches et si « Supprimer » a une prise.
     *   La sélection peut désigner des trajets **au-delà des dix affichés**, « Tout
     *   sélectionner » portant sur l'ensemble du sens.
     */
    data class Ready(
        val statistics: DirectionStatistics,
        val recentTrips: List<RecentTripRow>,
        val selectedIds: Set<String>? = null,
    ) : HistoryUiState {
        val selecting: Boolean get() = selectedIds != null
    }
}
