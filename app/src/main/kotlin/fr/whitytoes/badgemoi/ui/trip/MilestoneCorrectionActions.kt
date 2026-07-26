package fr.whitytoes.badgemoi.ui.trip

/**
 * Les quatre issues possibles de l'overlay de correction (cahier des charges §3.5).
 *
 * @property onSave enregistre l'heure locale saisie.
 * @property onSkip marque le jalon comme ignoré : on est passé sans pointer.
 * @property onClear remet le jalon en attente, donc à traiter de nouveau.
 * @property onDismiss referme sans rien changer.
 */
data class MilestoneCorrectionActions(
    val onSave: (hour: Int, minute: Int) -> Unit,
    val onSkip: () -> Unit,
    val onClear: () -> Unit,
    val onDismiss: () -> Unit,
)
