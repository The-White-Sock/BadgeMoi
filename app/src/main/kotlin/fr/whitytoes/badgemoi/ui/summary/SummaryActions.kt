package fr.whitytoes.badgemoi.ui.summary

/**
 * Les issues possibles du récapitulatif (cahier des charges §3.3), regroupées comme
 * [fr.whitytoes.badgemoi.ui.trip.TripActions] le fait pour l'écran actif.
 *
 * @property onArchive enregistre le trajet dans l'archive.
 * @property onDiscard l'abandonne sans l'archiver — l'appelant confirme au préalable.
 * @property onMilestoneClick ouvre la correction d'un jalon, sans quitter l'écran. L'écran
 *   la déclenche depuis un tronçon — qui désigne son jalon d'arrivée — ou depuis la cellule
 *   « Départ » du bandeau, seul accès au jalon 0.
 */
data class SummaryActions(
    val onArchive: () -> Unit,
    val onDiscard: () -> Unit,
    val onMilestoneClick: ((Int) -> Unit)? = null,
)
