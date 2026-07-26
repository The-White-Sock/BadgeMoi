package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Trip
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
 * La règle combine deux critères, dans cet ordre :
 *
 * 1. **jamais dans le futur** — on ne corrige que des jalons déjà franchis (§3.5), donc
 *    une occurrence postérieure à [now] est écartée ;
 * 2. parmi celles qui restent, la **plus proche de la référence**.
 *
 * Le premier critère n'est pas théorique. Sans lui, corriger un départ de 19h00 vers
 * 08h00 retenait le lendemain 08h00 — l'occurrence la plus proche dans l'absolu (+13 h,
 * contre −11 h le même jour). Le départ passait alors dans le futur, et le temps écoulé
 * du bandeau, borné à zéro, cessait de refléter la correction.
 *
 * Les trois occurrences examinées sont celles de la veille, du jour de la référence et
 * du lendemain. Le couple de critères traite d'un même mouvement les cas qui comptent :
 *
 * - un trajet qui franchit **minuit** — départ à 23h50, jalon corrigé à 00h05 : c'est
 *   l'occurrence du lendemain (+15 min) qui gagne, pas celle du jour du départ (−23h45) ;
 * - une correction **vers l'arrière** — jalon de référence à 19h29, saisie à 19h00 :
 *   c'est bien le même jour (−29 min), et non le lendemain (+23h31) ;
 * - une correction **de grande amplitude**, qui reste du bon côté de [now].
 *
 * Si les trois occurrences sont dans le futur — horloge décalée, référence aberrante —
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

    return occurrences
        .filterNot { it.isAfter(now) }
        .ifEmpty { occurrences }
        .minBy { abs(it.toEpochMilli() - reference.toEpochMilli()) }
}

/** Jalon posé le plus proche en amont, à défaut le départ, à défaut la création. */
private fun Trip.referenceInstantFor(index: Int): Instant =
    (index - 1 downTo 0).firstNotNullOfOrNull { times[it] }
        ?: departureAt
        ?: createdAt

/** Heure à présélectionner dans le sélecteur pour le jalon [index]. */
fun Trip.correctionSeedInstant(index: Int): Instant = times[index] ?: referenceInstantFor(index)
