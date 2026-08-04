package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.MilestoneIcon
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import java.time.Instant
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
 * Un jalon est corrigible (§3.5) une fois **tranché** : posé ou ignoré. Ouvrir l'overlay
 * sur un jalon courant ou à venir reviendrait à inventer un passage qui n'a pas eu lieu —
 * et à faire de la correction un second moyen de valider, en doublon de la barre d'action.
 */
val MilestoneStatus.isCorrectable: Boolean
    get() = this == MilestoneStatus.POSED || this == MilestoneStatus.SKIPPED

/**
 * Une ligne de la liste des jalons, prête à afficher.
 *
 * Un jalon est un **instant** — [at] — et il emprunte sa durée [sincePrevious] au tronçon
 * qui le précède. Les deux sont transportés parce que la ligne montre les deux : l'heure
 * discrète à gauche, la durée en avant à droite (§9, écart 9).
 *
 * @property at horodatage du jalon, `null` tant qu'il n'est pas posé — un jalon courant,
 *   à venir ou ignoré n'a pas d'heure à montrer.
 */
data class MilestoneRow(
    val index: Int,
    val label: String,
    val icon: MilestoneIcon,
    val status: MilestoneStatus,
    val at: Instant?,
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
            at = times[index],
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
