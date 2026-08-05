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
 * @property toIndex jalon d'**arrivée** du tronçon — celui qui le ferme, donc celui qui
 *   explique sa durée. C'est par lui que le récapitulatif ouvre la correction (§3.3) : les
 *   quatre tronçons couvrent ainsi les jalons 1 à 4, le départ se corrigeant depuis le
 *   bandeau. Le jalon de **départ** du tronçon n'est pas transporté : il est le jalon
 *   d'arrivée du tronçon précédent, et l'ouvrir aussi rendrait le clic ambigu.
 * @property duration `null` quand le tronçon n'est **pas mesurable** — l'un de ses deux
 *   jalons n'a pas été posé. Ce n'est pas une durée nulle et cela ne doit jamais
 *   s'afficher comme telle.
 * @property detail mention secondaire facultative. Sert à l'historique (§3.4), où une
 *   moyenne doit dire **sur combien de mesures** elle porte : un jalon ignoré rend le
 *   tronçon non mesurable, si bien qu'une moyenne calculée sur trois trajets parmi dix ne
 *   se lit pas comme une moyenne sur dix. `null` au récapitulatif, où la durée est
 *   mesurée et non moyennée.
 */
data class SegmentRow(
    val index: Int,
    val label: String,
    val fromLabel: String,
    val toLabel: String,
    val toIndex: Int,
    val duration: Duration?,
    val detail: String? = null,
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
            toIndex = segment.toIndex,
            duration = durationOf(segment),
        )
    }
}
