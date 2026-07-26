package fr.whitytoes.badgemoi.ui

import kotlin.time.Duration

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600

/**
 * Durée sous forme de chronomètre : `m:ss`, ou `h:mm:ss` au-delà de l'heure.
 *
 * Format compact et à largeur stable, adapté à la police monospace imposée par le POC
 * pour toute valeur chiffrée — un libellé du type « 12 min 34 s » sauterait à chaque
 * changement de seconde sur un compteur qui avance.
 *
 * Une durée négative est ramenée à zéro : elle ne peut venir que d'horodatages
 * incohérents (jalon corrigé à une heure antérieure au précédent), et afficher
 * « -3:00 » n'aiderait pas l'utilisateur.
 */
fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
