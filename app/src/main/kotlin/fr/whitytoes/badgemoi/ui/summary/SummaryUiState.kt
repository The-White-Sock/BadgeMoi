package fr.whitytoes.badgemoi.ui.summary

import fr.whitytoes.badgemoi.domain.Trip

/**
 * États de l'écran « Récapitulatif » (cahier des charges §3.3).
 *
 * [Loading] est distinct de [NoTrip], comme sur les autres écrans : tant que le DataStore
 * n'a pas répondu, on ignore s'il existe un trajet, et les confondre renverrait à
 * l'accueil avant même la lecture.
 */
sealed interface SummaryUiState {
    data object Loading : SummaryUiState

    /**
     * Plus de trajet à relire. Deux chemins y mènent — l'enregistrement vient d'aboutir,
     * ou le trajet a été abandonné depuis l'accueil — et tous deux appellent la même
     * suite : cet écran n'a plus d'objet, retour à l'accueil.
     */
    data object NoTrip : SummaryUiState

    /**
     * Trajet prêt à être relu puis archivé.
     *
     * @property archiving vrai entre l'appui sur « Enregistrer » et la fin de l'écriture.
     *   Porté dans l'état plutôt que gardé dans le ViewModel pour que l'écran puisse
     *   désactiver le bouton : le verrou est alors visible, et non seulement effectif.
     * @property archived vrai quand le trajet vient de l'**archive**, rouvert depuis
     *   l'historique. L'écran est le même, ses issues non : un trajet archivé se supprime
     *   et se referme, là où un trajet en cours s'archive ou s'abandonne.
     */
    data class Ready(
        val trip: Trip,
        val archiving: Boolean = false,
        val archived: Boolean = false,
    ) : SummaryUiState
}
