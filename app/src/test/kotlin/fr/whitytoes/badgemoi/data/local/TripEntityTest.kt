@file:Suppress("MagicNumber") // Données de test : durées et indices en clair.

package fr.whitytoes.badgemoi.data.local

import fr.whitytoes.badgemoi.domain.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Mapping entre le modèle du domaine et la ligne Room de l'archive. Le sens et la
 * date de création sont des colonnes propres (filtrage/tri) ; le reste passe par
 * les convertisseurs (voir [ConvertersTest]).
 */
class TripEntityTest {
    @Test
    fun `un trajet fait l'aller-retour sans rien perdre`() {
        val trip = mixedTrip()

        val restored = trip.toEntity().toDomain()

        assertEquals(trip, restored)
    }

    @Test
    fun `les colonnes de filtrage et de tri reprennent le sens et la date de création`() {
        val entity = mixedTrip().toEntity()

        assertEquals("trip-1", entity.id)
        assertEquals(Direction.RETOUR.name, entity.direction)
        assertEquals(testBase.toEpochMilli(), entity.createdAtEpochMs)
    }

    @Test
    fun `les jalons non posés restent nuls en base`() {
        val entity = mixedTrip().toEntity()

        assertNull(entity.times[2])
        assertNull(entity.times[3])
        assertEquals(testBase.plusSeconds(3_600).toEpochMilli(), entity.times[4])
    }
}
