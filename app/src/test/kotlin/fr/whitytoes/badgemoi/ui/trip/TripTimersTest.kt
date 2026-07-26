@file:Suppress("MagicNumber") // Données de test : indices et durées en clair.

package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class TripTimersTest {
    private val departure = Instant.parse("2026-07-26T07:00:00Z")

    private fun trip() = Trip.start(id = "t1", direction = Direction.ALLER, departureAt = departure)

    /** Instant situé [minutesAfter] minutes après le départ. */
    private fun at(minutesAfter: Long): Instant = departure.plusSeconds(minutesAfter * 60)

    @Test
    fun `sans jalon posé au-delà du départ les deux compteurs partent du départ`() {
        val timers = trip().timersAt(at(7))

        assertEquals(7.minutes, timers.elapsed)
        assertEquals(7.minutes, timers.sinceLastMilestone)
    }

    @Test
    fun `le temps écoulé court depuis le départ, celui du jalon depuis le dernier posé`() {
        val timers = trip().poseMilestone(1, at(10)).timersAt(at(14))

        assertEquals(14.minutes, timers.elapsed)
        assertEquals(4.minutes, timers.sinceLastMilestone)
    }

    /**
     * Un jalon ignoré n'a pas d'horodatage : le compteur doit remonter au dernier jalon
     * réellement posé, sinon il repartirait de zéro à chaque « Passer ».
     */
    @Test
    fun `un jalon ignoré ne réinitialise pas le compteur inter-jalons`() {
        val trip = trip().poseMilestone(1, at(10)).skipMilestone(2)

        val timers = trip.timersAt(at(18))

        assertEquals(8.minutes, timers.sinceLastMilestone)
    }

    /**
     * Les durées sont déduites des horodatages, jamais incrémentées : un retour
     * d'arrière-plan de plusieurs minutes doit afficher la bonne valeur.
     */
    @Test
    fun `un long intervalle sans évaluation donne quand même la bonne durée`() {
        val trip = trip().poseMilestone(1, at(5))

        assertEquals(90.minutes, trip.timersAt(at(90)).elapsed)
        assertEquals(85.minutes, trip.timersAt(at(90)).sinceLastMilestone)
    }

    @Test
    fun `un trajet terminé fige le temps écoulé et n'attend plus de jalon`() {
        val complet =
            (1 until Routes.MILESTONE_COUNT).fold(trip()) { trip, index ->
                trip.poseMilestone(index, at(index * 10L))
            }

        val timers = complet.timersAt(at(200))

        assertEquals("le compteur s'arrête à l'arrivée", 40.minutes, timers.elapsed)
        assertNull("plus aucun jalon n'est attendu", timers.sinceLastMilestone)
    }

    /**
     * Cas limite : le dernier jalon a été ignoré, le trajet est donc complet sans
     * horodatage d'arrivée. La référence de fin est alors le dernier jalon posé.
     */
    @Test
    fun `un trajet terminé sur un jalon ignoré se fige sur le dernier jalon posé`() {
        val complet =
            (1 until Routes.MILESTONE_COUNT - 1)
                .fold(trip()) { trip, index -> trip.poseMilestone(index, at(index * 10L)) }
                .skipMilestone(Routes.MILESTONE_COUNT - 1)

        val timers = complet.timersAt(at(200))

        assertEquals(30.minutes, timers.elapsed)
        assertNull(timers.sinceLastMilestone)
    }

    @Test
    fun `un horodatage postérieur à l'instant courant ne produit pas de durée négative`() {
        val trip = trip().poseMilestone(1, at(30))

        val timers = trip.timersAt(at(20))

        assertEquals(kotlin.time.Duration.ZERO, timers.sinceLastMilestone)
    }
}
