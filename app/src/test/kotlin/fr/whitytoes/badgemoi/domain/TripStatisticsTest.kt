@file:Suppress("MagicNumber") // Données de test : durées et indices en clair.

package fr.whitytoes.badgemoi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class TripStatisticsTest {
    private val base = Instant.ofEpochMilli(0L)

    /** Trajet aller dont seul le total (départ → arrivée) est renseigné, en minutes. */
    private fun tripWithTotal(
        id: String,
        totalMinutes: Long,
    ): Trip =
        Trip
            .start(id = id, direction = Direction.ALLER, departureAt = base)
            .poseMilestone(Routes.MILESTONE_COUNT - 1, base.plusSeconds(totalMinutes * 60))

    @Test
    fun `la moyenne totale ne compte que les trajets mesurables du bon sens`() {
        val trips =
            listOf(
                tripWithTotal("a", 20),
                tripWithTotal("b", 40),
                Trip.start("c", Direction.RETOUR, base), // autre sens, ignoré
                Trip.start("d", Direction.ALLER, base), // aller mais sans arrivée, ignoré
            )

        val stats = TripStatistics.forDirection(trips, Direction.ALLER)

        assertEquals(3, stats.tripCount)
        assertEquals(30.minutes, stats.totalAverage)
    }

    @Test
    fun `sans aucun trajet la moyenne est nulle`() {
        val stats = TripStatistics.forDirection(emptyList(), Direction.ALLER)

        assertEquals(0, stats.tripCount)
        assertNull(stats.totalAverage)
        assertEquals(4, stats.segmentAverages.size)
        assertTrue(stats.segmentAverages.all { it.average == null && it.sampleCount == 0 })
    }

    @Test
    fun `la moyenne d'un tronçon ne retient que les occurrences mesurées`() {
        val measured =
            Trip
                .start("a", Direction.ALLER, base)
                .poseMilestone(1, base.plusSeconds(120)) // Ride m0->m1 = 2 min
        val unmeasured = Trip.start("b", Direction.ALLER, base) // Ride non mesuré

        val stats = TripStatistics.forDirection(listOf(measured, unmeasured), Direction.ALLER)
        val ride = stats.segmentAverages.first()

        assertEquals(2.minutes, ride.average)
        assertEquals(1, ride.sampleCount)
    }

    @Test
    fun `le rythme se compare aux seuils du POC`() {
        val average = 20.minutes

        assertEquals(TripPace.FASTER, TripStatistics.paceOf(18.minutes, average))
        assertEquals(TripPace.SLOWER, TripStatistics.paceOf(23.minutes, average))
        assertEquals(TripPace.TYPICAL, TripStatistics.paceOf(20.minutes, average))
        assertEquals(TripPace.TYPICAL, TripStatistics.paceOf(null, average))
        assertEquals(TripPace.TYPICAL, TripStatistics.paceOf(20.minutes, null))
    }
}
