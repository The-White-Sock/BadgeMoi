package fr.whitytoes.badgemoi.ui.summary

import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import kotlin.time.Duration

/**
 * Une ligne de la liste des tronçons, prête à afficher (cahier des charges §3.3).
 *
 * Les deux tronçons **Ride** portent le même nom : seules leurs extrémités les
 * distinguent. Elles sont donc transportées ici, comme le POC transporte le couple
 * d'icônes de ses `pairIcon` — le nom du tronçon n'y étant qu'une mention secondaire.
 *
 * [duration] est volontairement une donnée d'entrée et non un calcul : l'écran des
 * moyennes (lot 5, §3.4) affiche la même structure avec des durées moyennées. Concevoir
 * la ligne autour d'une durée reçue évite d'écrire une seconde liste là-bas.
 *
 * @property duration `null` quand le tronçon n'est **pas mesurable** — l'un de ses deux
 *   jalons n'a pas été posé. Ce n'est pas une durée nulle et cela ne doit jamais
 *   s'afficher comme telle.
 */
data class SegmentRow(
    val index: Int,
    val label: String,
    val fromLabel: String,
    val toLabel: String,
    val duration: Duration?,
)

/**
 * Dérive les lignes de tronçons de ce trajet.
 *
 * Tout le calcul vient du domaine (lot 1) : `Trip.durationOf` renvoie déjà `null` dès
 * qu'un des deux jalons manque, ce qui couvre le cas d'un jalon ignoré.
 */
fun Trip.segmentRows(): List<SegmentRow> {
    val route = Routes.forDirection(direction)

    return route.segments.mapIndexed { index, segment ->
        SegmentRow(
            index = index,
            label = segment.label.label,
            fromLabel = route.milestones[segment.fromIndex].label,
            toLabel = route.milestones[segment.toIndex].label,
            duration = durationOf(segment),
        )
    }
}
