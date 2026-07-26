package fr.whitytoes.badgemoi.ui.trip

/**
 * Actions offertes par l'écran « Trajet actif ».
 *
 * Regroupées plutôt que passées une à une : l'écran en accumulera d'autres — la
 * correction d'un jalon en apporte quatre — et un paramètre par action rendrait sa
 * signature illisible.
 *
 * @property onMilestoneClick `null` rend la liste non interactive, ce dont se sert le
 *   récapitulatif du lot 4, où la correction n'a pas lieu d'être.
 */
data class TripActions(
    val onValidate: () -> Unit,
    val onSkip: () -> Unit,
    val onMilestoneClick: ((Int) -> Unit)? = null,
)
