package fr.whitytoes.badgemoi.ui.history

import fr.whitytoes.badgemoi.domain.Direction

/**
 * Les issues de l'écran Historique (cahier des charges §3.4), regroupées comme
 * [fr.whitytoes.badgemoi.ui.summary.SummaryActions] le fait pour le récapitulatif.
 *
 * @property onSelectDirection bascule le sens affiché.
 * @property onExport ouvre le sélecteur de fichier. L'écran ne sait rien du Storage
 *   Access Framework : il reçoit une lambda déjà câblée.
 * @property onClear vide l'archive. L'écran arme la décision par un double appui, le
 *   ViewModel protège l'écriture — deux garde-fous de nature différente.
 */
data class HistoryActions(
    val onSelectDirection: (Direction) -> Unit,
    val onExport: () -> Unit,
    val onClear: () -> Unit,
)
