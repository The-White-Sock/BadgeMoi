@file:Suppress("MagicNumber") // Données de test : durées et indices en clair.

package fr.whitytoes.badgemoi.data.local

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import java.time.Instant

/** Instant de référence des jeux d'essai. */
internal val testBase: Instant = Instant.ofEpochMilli(1_700_000_000_000L)

/**
 * Trajet volontairement « bancal » : un jalon posé, un jalon ignoré, un jalon
 * jamais traité. C'est la forme qui met le plus à l'épreuve les mappings, car
 * elle mélange horodatages présents, absents et statut « ignoré ».
 */
internal fun mixedTrip(): Trip =
    Trip
        .start(id = "trip-1", direction = Direction.RETOUR, departureAt = testBase)
        .poseMilestone(1, testBase.plusSeconds(600))
        .skipMilestone(2)
        .poseMilestone(4, testBase.plusSeconds(3_600))
