package fr.whitytoes.badgemoi.ui.history

import fr.whitytoes.badgemoi.domain.DirectionStatistics
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.ui.summary.SegmentRow

/**
 * Convertit les moyennes par tronçon en lignes d'affichage.
 *
 * C'est le pari de #49 qui se vérifie : [SegmentRow] porte une durée **reçue**, et non
 * calculée depuis un trajet. Le récapitulatif y met une durée mesurée, l'historique une
 * moyenne, et le même composant les rend l'un comme l'autre.
 *
 * @param sampleLabels rendu du nombre de mesures, **un par tronçon et dans le même
 *   ordre** que [DirectionStatistics.segmentAverages]. Une liste déjà construite plutôt
 *   qu'une lambda : le libellé dépend d'un pluriel de ressources, donc d'un appel
 *   composable, qu'une lambda ordinaire ne peut pas porter — et rendre cette dérivation
 *   composable la sortirait du champ des tests JVM.
 */
fun DirectionStatistics.segmentRows(sampleLabels: List<String>): List<SegmentRow> {
    val route = Routes.forDirection(direction)

    return segmentAverages.mapIndexed { index, average ->
        SegmentRow(
            index = index,
            label = average.segment.label.label,
            fromLabel = route.milestones[average.segment.fromIndex].label,
            toLabel = route.milestones[average.segment.toIndex].label,
            toIndex = average.segment.toIndex,
            duration = average.average,
            // Une moyenne sur trois trajets parmi dix ne se lit pas comme une moyenne
            // sur dix : un jalon ignoré rend le tronçon non mesurable, et l'écart entre
            // les deux nombres est une information, pas un détail.
            detail = sampleLabels.getOrNull(index),
        )
    }
}
