package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Trip
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * Convertit l'heure locale saisie pour le jalon [index] en [Instant].
 *
 * Le sélecteur ne rend qu'une heure et des minutes : le **jour** doit être déduit. La
 * référence est le jalon posé précédent — ou le départ pour le premier jalon.
 *
 * La règle retenue est celle de l'**occurrence la plus proche** : parmi les trois
 * occurrences possibles de l'heure saisie (veille, jour de la référence, lendemain), on
 * garde celle dont l'écart à la référence est le plus faible. Elle traite d'un même
 * mouvement les deux cas qui comptent :
 *
 * - un trajet qui franchit **minuit** — départ à 23h50, jalon corrigé à 00h05 : c'est
 *   l'occurrence du lendemain (+15 min) qui gagne, pas celle du jour du départ (−23h45) ;
 * - une correction **vers l'arrière** — jalon de référence à 19h29, saisie à 19h00 :
 *   c'est bien le même jour (−29 min), et non le lendemain (+23h31).
 *
 * Le décalage se fait sur le calendrier local (`plusDays`) et non par tranches de
 * 24 heures : lors d'un changement d'heure, l'occurrence garde ainsi l'heure murale
 * saisie.
 */
fun Trip.correctionInstant(
    index: Int,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Instant {
    val reference = referenceInstantFor(index)
    val referenceDay = reference.atZone(zone)

    return (-1L..1L)
        .map { referenceDay.plusDays(it).with(LocalTime.of(hour, minute)).toInstant() }
        .minBy { abs(it.toEpochMilli() - reference.toEpochMilli()) }
}

/** Jalon posé le plus proche en amont, à défaut le départ, à défaut la création. */
private fun Trip.referenceInstantFor(index: Int): Instant =
    (index - 1 downTo 0).firstNotNullOfOrNull { times[it] }
        ?: departureAt
        ?: createdAt

/** Heure à présélectionner dans le sélecteur pour le jalon [index]. */
fun Trip.correctionSeedInstant(index: Int): Instant = times[index] ?: referenceInstantFor(index)
