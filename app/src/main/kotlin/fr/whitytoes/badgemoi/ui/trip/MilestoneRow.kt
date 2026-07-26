package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.MilestoneIcon
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * État d'affichage d'un jalon. [PENDING] et [CURRENT] sont tous deux « pas encore
 * traités », mais seul le second est celui que l'utilisateur doit valider maintenant :
 * les distinguer permet de le mettre en évidence sans recalculer `currentStep` dans les
 * composables.
 */
enum class MilestoneStatus { POSED, SKIPPED, CURRENT, PENDING }

/**
 * Une ligne de la liste des jalons, prête à afficher.
 *
 * [sincePrevious] est la durée écoulée depuis le jalon précédent — le cahier §3.2 est
 * explicite : la liste montre l'écart inter-jalons, l'heure absolue étant réservée au
 * bandeau.
 */
data class MilestoneRow(
    val index: Int,
    val label: String,
    val icon: MilestoneIcon,
    val status: MilestoneStatus,
    val sincePrevious: Duration?,
)

/**
 * Dérive les lignes à afficher pour ce trajet.
 *
 * Le point délicat est [MilestoneRow.sincePrevious] : un jalon **ignoré** n'a pas
 * d'horodatage, l'écart se mesure donc depuis le dernier jalon réellement **posé**, pas
 * depuis le jalon d'index précédent. Sauter cette subtilité afficherait des durées
 * nulles dès qu'un jalon est passé.
 */
fun Trip.milestoneRows(): List<MilestoneRow> {
    val definitions = Routes.forDirection(direction).milestones
    val step = currentStep

    return definitions.map { definition ->
        val index = definition.index
        MilestoneRow(
            index = index,
            label = definition.label,
            icon = definition.icon,
            status =
                when {
                    skipped[index] -> MilestoneStatus.SKIPPED
                    times[index] != null -> MilestoneStatus.POSED
                    index == step -> MilestoneStatus.CURRENT
                    else -> MilestoneStatus.PENDING
                },
            sincePrevious = durationSincePreviousPosed(index),
        )
    }
}

private fun Trip.durationSincePreviousPosed(index: Int): Duration? {
    val current = times[index]
    val previous = (index - 1 downTo 0).firstNotNullOfOrNull { times[it] }

    return if (current != null && previous != null) {
        (current.toEpochMilli() - previous.toEpochMilli()).milliseconds
    } else {
        null
    }
}
