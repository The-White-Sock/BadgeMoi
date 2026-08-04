@file:Suppress("MagicNumber") // Données de test : indices de jalons et durées en clair.

package fr.whitytoes.badgemoi.ui.summary

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class SegmentRowTest {
    private val departure = Instant.parse("2026-07-26T07:00:00Z")

    private fun trip(direction: Direction = Direction.ALLER) =
        Trip.start(id = "t1", direction = direction, departureAt = departure)

    /** Instant situé [minutesAfter] minutes après le départ. */
    private fun at(minutesAfter: Long): Instant = departure.plusSeconds(minutesAfter * 60)

    @Test
    fun `un trajet expose les quatre tronçons nommés du parcours`() {
        val rows = trip().segmentRows()

        assertEquals(4, rows.size)
        assertEquals(listOf("Ride", "Attente", "Train", "Ride"), rows.map { it.label })
    }

    /**
     * Le point de l'issue : les deux tronçons **Ride** portent le même nom. Seules leurs
     * extrémités les distinguent, elles doivent donc être transportées jusqu'à l'affichage.
     */
    @Test
    fun `les deux tronçons Ride se distinguent par leurs extrémités`() {
        val rows = trip().segmentRows()

        assertEquals("Ride", rows[0].label)
        assertEquals("Domicile", rows[0].fromLabel)
        assertEquals("Gare", rows[0].toLabel)

        assertEquals("Ride", rows[3].label)
        assertEquals("Gare", rows[3].fromLabel)
        assertEquals("Bureau", rows[3].toLabel)
    }

    /**
     * Le récapitulatif corrige un jalon en cliquant le tronçon qui s'y termine. Les
     * quatre tronçons doivent donc couvrir les jalons 1 à 4 — le départ, lui, se corrige
     * depuis le bandeau.
     */
    @Test
    fun `les tronçons désignent leur jalon d'arrivée, du premier au dernier`() {
        assertEquals(listOf(1, 2, 3, 4), trip().segmentRows().map { it.toIndex })
    }

    @Test
    fun `les extrémités suivent le sens du trajet`() {
        val rows = trip(Direction.RETOUR).segmentRows()

        assertEquals("Bureau", rows[0].fromLabel)
        assertEquals("Domicile", rows[3].toLabel)
    }

    @Test
    fun `la durée d'un tronçon est l'écart entre ses deux jalons`() {
        val rows =
            trip()
                .poseMilestone(1, at(9))
                .poseMilestone(2, at(14))
                .segmentRows()

        assertEquals(9.minutes, rows[0].duration)
        assertEquals(5.minutes, rows[1].duration)
    }

    /**
     * Un jalon ignoré n'a pas d'horodatage : les deux tronçons qui le touchent deviennent
     * non mesurables. `null` et non zéro — ce n'est pas « 0 min », c'est « inconnu ».
     */
    @Test
    fun `un jalon ignoré rend ses deux tronçons non mesurables`() {
        val rows =
            trip()
                .poseMilestone(1, at(9))
                .skipMilestone(2)
                .poseMilestone(3, at(32))
                .segmentRows()

        assertEquals("le tronçon avant le départ ignoré", 9.minutes, rows[0].duration)
        assertNull("Attente, qui finit sur le jalon ignoré", rows[1].duration)
        assertNull("Train, qui en part", rows[2].duration)
    }

    @Test
    fun `un trajet neuf n'a aucune durée mesurable`() {
        assertEquals(List(4) { null }, trip().segmentRows().map { it.duration })
    }
}
