package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Trip
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Les deux compteurs de l'écran « Trajet actif » (cahier des charges §3.2).
 *
 * @property elapsed temps écoulé depuis le départ.
 * @property sinceLastMilestone temps depuis le dernier jalon **posé** ; `null` une fois
 *   le trajet terminé, où plus aucun jalon n'est attendu.
 */
data class TripTimers(
    val elapsed: Duration?,
    val sinceLastMilestone: Duration?,
) {
    companion object {
        val EMPTY = TripTimers(elapsed = null, sinceLastMilestone = null)
    }
}

/**
 * Calcule les compteurs à l'instant [now].
 *
 * Les valeurs sont **déduites des horodatages**, jamais incrémentées pas à pas : un
 * retour d'arrière-plan de plusieurs minutes affiche donc la bonne durée, là où un
 * compteur qui s'auto-incrémente aurait pris du retard.
 *
 * Deux subtilités du modèle sont traitées ici :
 *
 * - un jalon **ignoré** n'a pas d'horodatage, donc le « depuis le dernier jalon »
 *   remonte au dernier jalon réellement posé ;
 * - un trajet terminé **fige** ses compteurs. Laisser le temps écoulé courir après
 *   l'arrivée afficherait une durée fausse. Le dernier jalon pouvant avoir été ignoré,
 *   la référence de fin est le dernier jalon posé, pas nécessairement l'arrivée.
 */
fun Trip.timersAt(now: Instant): TripTimers {
    val departure = departureAt ?: return TripTimers.EMPTY
    val lastPosed = times.filterNotNull().lastOrNull() ?: departure

    val end = if (isComplete) lastPosed else now

    return TripTimers(
        elapsed = between(departure, end),
        sinceLastMilestone = if (isComplete) null else between(lastPosed, now),
    )
}

private fun between(
    from: Instant,
    to: Instant,
): Duration = (to.toEpochMilli() - from.toEpochMilli()).coerceAtLeast(0).milliseconds

/**
 * Durée **mesurée** du trajet : du départ au dernier jalon posé.
 *
 * Distincte de [Trip.totalDuration], qui exige que le **dernier** jalon soit posé et rend
 * `null` sinon. Un trajet dont l'arrivée a été ignorée reste mesurable jusqu'à son dernier
 * pointage — c'est ce que le bandeau du récapitulatif affiche à la place de l'heure
 * d'arrivée, comme le `departArrivalFlap` du POC bascule sur « Écoulé ».
 */
fun Trip.measuredDuration(): Duration? {
    val departure = departureAt
    val lastPosed = times.filterNotNull().lastOrNull()
    return if (departure != null && lastPosed != null) between(departure, lastPosed) else null
}
