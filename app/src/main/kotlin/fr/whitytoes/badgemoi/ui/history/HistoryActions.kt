package fr.whitytoes.badgemoi.ui.history

import fr.whitytoes.badgemoi.domain.Direction

/**
 * Les issues de l'écran Historique (cahier des charges §3.4), regroupées comme
 * [fr.whitytoes.badgemoi.ui.summary.SummaryActions] le fait pour le récapitulatif.
 *
 * @property onSelectDirection bascule le sens affiché.
 * @property onExport ouvre le sélecteur de fichier. L'écran ne sait rien du Storage
 *   Access Framework : il reçoit une lambda déjà câblée.
 * @property onTripClick action d'une ligne. Hors mode sélection elle **ouvre** le trajet,
 *   dedans elle le coche ou le décoche — c'est l'écran qui n'a qu'un seul geste à offrir,
 *   et le ViewModel qui sait ce qu'il veut dire.
 * @property onStartSelection entre en mode sélection.
 * @property onCancelSelection en sort sans rien détruire.
 * @property onSelectAll coche **tous** les trajets du sens, au-delà des dix affichés.
 *   C'est ce qui remplace la purge par sens.
 * @property onDeleteSelected retire les trajets cochés.
 *
 * Toutes les valeurs par défaut sont inertes : les aperçus n'ont rien à câbler.
 */
data class HistoryActions(
    val onSelectDirection: (Direction) -> Unit = {},
    val onExport: () -> Unit = {},
    val onTripClick: ((String) -> Unit)? = null,
    val onStartSelection: () -> Unit = {},
    val onCancelSelection: () -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
)
