package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Trip
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * Convertit l'heure locale saisie pour le jalon [index] en [Instant], à l'instant [now].
 *
 * Le sélecteur ne rend qu'une heure et des minutes : le **jour** doit être déduit. La
 * référence est le jalon posé précédent — ou le départ pour le premier jalon.
 *
 * Le critère principal est l'**occurrence la plus proche de la référence**, parmi celles
 * de la veille, du jour de la référence et du lendemain. Il traite les deux cas courants :
 *
 * - un trajet qui franchit **minuit** — départ à 23h50, jalon corrigé à 00h05 : c'est
 *   l'occurrence du lendemain (+15 min) qui gagne, pas celle du jour du départ (−23h45) ;
 * - une correction **vers l'arrière** — jalon de référence à 19h29, saisie à 19h00 :
 *   c'est bien le même jour (−29 min), et non le lendemain (+23h31).
 *
 * Ce critère seul dérape quand l'heure saisie s'écarte de plus de 12 h de celle de la
 * référence : avec un jalon de référence à 19h00 et une saisie à 06h00, le lendemain
 * (+11 h) l'emporte sur le même jour (−13 h), et le jalon part dans le futur. Le temps
 * écoulé du bandeau, borné à zéro, cesse alors de refléter la correction.
 *
 * D'où le garde-fou : une occurrence postérieure à [now] est écartée — on ne corrige que
 * des jalons déjà franchis (§3.5). Il est assorti d'une **tolérance** de
 * [FutureTolerance], sans quoi saisir 20h10 alors qu'il est 20h05 — arrondi à la minute
 * supérieure, montre en avance — renverrait le jalon à la veille, soit bien pire que le
 * défaut corrigé. Une heure est large pour ce genre d'écart, et reste très en deçà des
 * onze heures de dépassement du cas ci-dessus.
 *
 * Si toutes les occurrences sont écartées — horloge très décalée, référence aberrante —
 * on retombe sur la plus proche : mieux vaut une valeur discutable qu'aucune.
 *
 * Le décalage se fait sur le calendrier local (`plusDays`) et non par tranches de
 * 24 heures : lors d'un changement d'heure, l'occurrence garde ainsi l'heure murale
 * saisie.
 */
fun Trip.correctionInstant(
    index: Int,
    hour: Int,
    minute: Int,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): Instant {
    val reference = referenceInstantFor(index)
    val referenceDay = reference.atZone(zone)

    val occurrences =
        (-1L..1L).map { referenceDay.plusDays(it).with(LocalTime.of(hour, minute)).toInstant() }
    val latestAcceptable = now.plus(FutureTolerance)

    return occurrences
        .filterNot { it.isAfter(latestAcceptable) }
        .ifEmpty { occurrences }
        .minBy { abs(it.toEpochMilli() - reference.toEpochMilli()) }
}

/**
 * Marge au-delà de [java.time.Clock] tolérée pour une heure saisie : arrondi à la minute
 * supérieure, montre en avance sur le téléphone. Voir [correctionInstant].
 */
private val FutureTolerance: Duration = Duration.ofHours(1)

/** Jalon posé le plus proche en amont, à défaut le départ, à défaut la création. */
private fun Trip.referenceInstantFor(index: Int): Instant =
    (index - 1 downTo 0).firstNotNullOfOrNull { times[it] }
        ?: departureAt
        ?: createdAt

/** Heure à présélectionner dans le sélecteur pour le jalon [index]. */
fun Trip.correctionSeedInstant(index: Int): Instant = times[index] ?: referenceInstantFor(index)
