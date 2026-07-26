package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Trip
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Convertit l'heure locale saisie pour le jalon [index] en [Instant].
 *
 * Le sélecteur ne rend qu'une heure et des minutes : le **jour** doit être déduit. Il
 * est pris sur le jalon posé précédent — ou sur le départ pour le premier jalon.
 *
 * Le cas piégeux est le trajet qui franchit **minuit** : un départ à 23h50 et une
 * correction à 00h05 tomberaient, sur le jour du départ, *avant* celui-ci. La date est
 * donc décalée d'un jour lorsque le résultat précède sa référence, ce qui reflète l'ordre
 * séquentiel du parcours.
 *
 * Le jalon de départ échappe à ce décalage : le corriger vers une heure antérieure est
 * légitime, et l'avancer d'un jour serait absurde.
 */
fun Trip.correctionInstant(
    index: Int,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Instant {
    val reference = referenceInstantFor(index)
    val candidate =
        reference
            .atZone(zone)
            .with(LocalTime.of(hour, minute))
            .toInstant()

    return if (index > 0 && candidate.isBefore(reference)) {
        candidate.plus(1, ChronoUnit.DAYS)
    } else {
        candidate
    }
}

/** Jalon posé le plus proche en amont, à défaut le départ, à défaut la création. */
private fun Trip.referenceInstantFor(index: Int): Instant =
    (index - 1 downTo 0).firstNotNullOfOrNull { times[it] }
        ?: departureAt
        ?: createdAt

/** Heure à présélectionner dans le sélecteur pour le jalon [index]. */
fun Trip.correctionSeedInstant(index: Int): Instant = times[index] ?: referenceInstantFor(index)
