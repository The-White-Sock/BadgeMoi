package fr.whitytoes.badgemoi.ui.summary

/**
 * Les issues possibles du récapitulatif (cahier des charges §3.3), regroupées comme
 * [fr.whitytoes.badgemoi.ui.trip.TripActions] le fait pour l'écran actif.
 *
 * @property onArchive enregistre le trajet dans l'archive.
 * @property onDiscard l'abandonne sans l'archiver — l'appelant confirme au préalable.
 * @property onMilestoneClick ouvre la correction d'un jalon, sans quitter l'écran.
 */
data class SummaryActions(
    val onArchive: () -> Unit,
    val onDiscard: () -> Unit,
    val onMilestoneClick: ((Int) -> Unit)? = null,
)
