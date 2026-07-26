@file:Suppress("MagicNumber") // Données de test : durées et indices en clair.

package fr.whitytoes.badgemoi.data.local

import fr.whitytoes.badgemoi.domain.Direction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Le trajet en cours transite par le DataStore sous forme de JSON. Ce mapping est
 * la frontière entre le modèle du domaine et sa forme stockée : toute perte ici
 * se traduirait par un trajet repris dans un état faux après redémarrage.
 */
class StoredTripTest {
    @Test
    fun `un trajet fait l'aller-retour sans rien perdre`() {
        val trip = mixedTrip()

        val restored = trip.toStored().toDomain()

        assertEquals(trip, restored)
    }

    @Test
    fun `la forme stockée expose le sens par son nom et les horodatages en millisecondes`() {
        val stored = mixedTrip().toStored()

        assertEquals(Direction.RETOUR.name, stored.direction)
        assertEquals(testBase.toEpochMilli(), stored.createdAtEpochMs)
        assertEquals(testBase.toEpochMilli(), stored.times[0])
        assertNull("le jalon ignoré ne porte pas d'horodatage", stored.times[2])
        assertNull("le jalon jamais traité ne porte pas d'horodatage", stored.times[3])
    }

    @Test
    fun `les jalons ignorés sont conservés distinctement des jalons en attente`() {
        val stored = mixedTrip().toStored()

        assertEquals(listOf(false, false, true, false, false), stored.skipped)
    }

    @Test
    fun `un trajet survit à la sérialisation JSON réellement utilisée par le DataStore`() {
        val trip = mixedTrip()

        val json = Json.encodeToString(trip.toStored())
        val restored = Json.decodeFromString<StoredTrip>(json).toDomain()

        assertEquals(trip, restored)
    }
}
