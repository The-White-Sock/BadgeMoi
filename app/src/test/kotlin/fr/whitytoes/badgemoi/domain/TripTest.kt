@file:Suppress("MagicNumber") // Données de test : durées et indices en clair.

package fr.whitytoes.badgemoi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class TripTest {
    private val departure = Instant.ofEpochMilli(1_000_000L)

    private fun newTrip(direction: Direction = Direction.ALLER): Trip =
        Trip.start(id = "t1", direction = direction, departureAt = departure)

    @Test
    fun `un trajet neuf pose le départ et laisse les autres jalons en attente`() {
        val trip = newTrip()

        assertEquals(departure, trip.times[0])
        assertTrue(trip.times.drop(1).all { it == null })
        assertTrue(trip.skipped.none { it })
        assertEquals(1, trip.currentStep)
        assertFalse(trip.isComplete)
    }

    @Test
    fun `le step courant est le premier jalon ni posé ni ignoré`() {
        val trip =
            newTrip()
                .poseMilestone(1, departure.plusSeconds(60))
                .poseMilestone(2, departure.plusSeconds(120))

        assertEquals(3, trip.currentStep)
    }

    @Test
    fun `un jalon ignoré fait avancer le step sans horodatage`() {
        val trip = newTrip().skipMilestone(1)

        assertEquals(2, trip.currentStep)
        assertNull(trip.times[1])
        assertTrue(trip.skipped[1])
    }

    @Test
    fun `un trajet dont tous les jalons sont traités est complet`() {
        var trip = newTrip()
        for (i in 1 until Routes.MILESTONE_COUNT) {
            trip = trip.poseMilestone(i, departure.plusSeconds(60L * i))
        }

        assertTrue(trip.isComplete)
        assertEquals(Routes.MILESTONE_COUNT, trip.currentStep)
    }

    @Test
    fun `poser un jalon lève un éventuel statut ignoré`() {
        val trip =
            newTrip()
                .skipMilestone(1)
                .poseMilestone(1, departure.plusSeconds(30))

        assertFalse(trip.skipped[1])
        assertEquals(departure.plusSeconds(30), trip.times[1])
    }

    @Test
    fun `effacer un jalon le remet en attente`() {
        val trip =
            newTrip()
                .poseMilestone(1, departure.plusSeconds(30))
                .clearMilestone(1)

        assertNull(trip.times[1])
        assertFalse(trip.skipped[1])
        assertEquals(1, trip.currentStep)
    }

    @Test
    fun `la durée d'un tronçon vaut l'écart entre ses deux jalons`() {
        val trip =
            newTrip()
                .poseMilestone(1, departure.plusSeconds(300))
        val ride = trip.route.segments.first()

        assertEquals(5.minutes, trip.durationOf(ride))
    }

    @Test
    fun `la durée d'un tronçon est nulle si un jalon manque`() {
        val trip = newTrip()
        val ride = trip.route.segments.first()

        assertNull(trip.durationOf(ride))
    }

    @Test
    fun `la durée totale va du départ à l'arrivée`() {
        val trip = newTrip().poseMilestone(Routes.MILESTONE_COUNT - 1, departure.plusSeconds(600))

        assertEquals(10.minutes, trip.totalDuration)
    }

    @Test
    fun `la durée totale est nulle tant que l'arrivée n'est pas posée`() {
        assertNull(newTrip().totalDuration)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `un trajet avec un mauvais nombre de jalons est rejeté`() {
        Trip(
            id = "bad",
            direction = Direction.ALLER,
            createdAt = departure,
            times = listOf(departure),
            skipped = List(Routes.MILESTONE_COUNT) { false },
        )
    }
}
