package fr.whitytoes.badgemoi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Heure sur 24 h, `HH:mm`, et date `JJ/MM/AA` : les deux formats français attendus par le
 * cahier des charges.
 *
 * Les motifs sont **figés**, là où `ofLocalizedTime` suivait la locale de l'appareil et
 * affichait « 7:29 PM » sur un téléphone configuré en anglais. Les chiffres sont ceux de
 * [Locale.ROOT], pour qu'une locale à chiffres non arabes n'aille pas rendre un
 * chronomètre illisible.
 */
private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ROOT)

/**
 * Heure d'un horodatage au format `HH:mm`, lue dans [zone].
 *
 * Le fuseau est résolu à l'affichage plutôt que figé : les trajets sont stockés en
 * [Instant] (lot 1) et doivent se lire à l'heure locale de l'utilisateur.
 */
internal fun formatTimeAt(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): String = TimeFormatter.withZone(zone).format(instant)

/**
 * Date d'un horodatage au format `JJ/MM/AA`, lue dans [zone].
 *
 * Sert aux écrans qui datent un trajet — récapitulatif (§3.3) et historique (§3.6) —
 * l'écran actif, lui, n'affichant que des heures.
 */
internal fun formatDateAt(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): String = DateFormatter.withZone(zone).format(instant)

/** Version composable de [formatTimeAt], mémorisée sur l'instant affiché. */
@Composable
internal fun formatTime(instant: Instant): String = remember(instant) { formatTimeAt(instant) }

/** Version composable de [formatDateAt], mémorisée sur l'instant affiché. */
@Composable
internal fun formatDate(instant: Instant): String = remember(instant) { formatDateAt(instant) }
