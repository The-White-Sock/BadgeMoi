package fr.whitytoes.badgemoi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Heure d'un horodatage, au format court de la locale de l'appareil.
 *
 * Le fuseau est résolu à l'affichage plutôt que figé : les trajets sont stockés en
 * [Instant] (lot 1) et doivent se lire à l'heure locale de l'utilisateur.
 *
 * Partagé par l'accueil et les écrans des lots 3 à 5, qui affichent tous des heures
 * de jalon.
 */
@Composable
internal fun formatTime(instant: Instant): String =
    remember(instant) {
        DateTimeFormatter
            .ofLocalizedTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
