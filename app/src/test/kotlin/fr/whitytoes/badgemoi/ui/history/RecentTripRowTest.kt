@file:Suppress("MagicNumber") // Données de test : indices de jalons et durées en clair.

package fr.whitytoes.badgemoi.ui.history

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.domain.TripPace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class RecentTripRowTest {
    private val day = Instant.parse("2026-07-26T07:00:00Z")

    /** Trajet complet de [minutes] minutes, parti [daysAgo] jours avant la référence. */
    private fun trip(
        id: String,
        durationMinutes: Long,
        daysAgo: Long = 0,
        direction: Direction = Direction.ALLER,
    ): Trip {
        val departure = day.minusSeconds(daysAgo * 86_400)
        val start = Trip.start(id = id, direction = direction, departureAt = departure)
        return (1 until Routes.MILESTONE_COUNT).fold(start) { trip, index ->
            // Les jalons intermédiaires sont répartis ; seul le dernier fixe la durée.
            val offset = durationMinutes * index / (Routes.MILESTONE_COUNT - 1)
            trip.poseMilestone(index, departure.plusSeconds(offset * 60))
        }
    }

    @Test
    fun `les trajets d'un autre sens sont écartés`() {
        val trips =
            listOf(
                trip("aller", durationMinutes = 30),
                trip("retour", durationMinutes = 30, direction = Direction.RETOUR),
            )

        val rows = trips.recentTripRows(Direction.ALLER, average = 30.minutes)

        assertEquals(listOf("aller"), rows.map { it.id })
    }

    /**
     * `observeAll()` ne promet aucun ordre : le tri doit être fait ici, sans quoi
     * l'affichage dépendrait d'un détail d'implémentation du dépôt.
     */
    @Test
    fun `les trajets sortent du plus récent au plus ancien`() {
        val trips =
            listOf(
                trip("vieux", durationMinutes = 30, daysAgo = 5),
                trip("recent", durationMinutes = 30, daysAgo = 0),
                trip("moyen", durationMinutes = 30, daysAgo = 2),
            )

        val rows = trips.recentTripRows(Direction.ALLER, average = 30.minutes)

        assertEquals(listOf("recent", "moyen", "vieux"), rows.map { it.id })
    }

    @Test
    fun `seuls les dix derniers trajets sont retenus`() {
        val trips = (0 until 15).map { trip("t$it", durationMinutes = 30, daysAgo = it.toLong()) }

        val rows = trips.recentTripRows(Direction.ALLER, average = 30.minutes)

        assertEquals(RECENT_TRIP_COUNT, rows.size)
        assertEquals("le plus ancien retenu", "t9", rows.last().id)
    }

    @Test
    fun `l'allure se mesure contre la moyenne fournie`() {
        val trips =
            listOf(
                trip("rapide", durationMinutes = 20, daysAgo = 0),
                trip("lent", durationMinutes = 40, daysAgo = 1),
                trip("dans la moyenne", durationMinutes = 30, daysAgo = 2),
            )

        val rows = trips.recentTripRows(Direction.ALLER, average = 30.minutes)

        assertEquals(TripPace.FASTER, rows[0].pace)
        assertEquals(TripPace.SLOWER, rows[1].pace)
        assertEquals(TripPace.TYPICAL, rows[2].pace)
    }

    /**
     * Le point de l'issue : `paceOf` retombe sur `TYPICAL` faute de mieux, ce qui
     * afficherait un trajet non mesuré comme étant « dans la moyenne ». La ligne doit
     * porter `null`, seule façon de le distinguer à l'affichage.
     */
    @Test
    fun `un trajet sans durée totale n'a pas d'allure`() {
        val incomplet = trip("incomplet", durationMinutes = 30).skipMilestone(4)

        val rows = listOf(incomplet).recentTripRows(Direction.ALLER, average = 30.minutes)

        assertNull("la durée", rows.single().total)
        assertNull("l'allure", rows.single().pace)
    }

    /** Sans archive, il n'y a pas de moyenne : aucun trajet ne peut être situé. */
    @Test
    fun `sans moyenne aucun trajet n'est situé`() {
        val rows = listOf(trip("seul", durationMinutes = 30)).recentTripRows(Direction.ALLER, average = null)

        assertEquals(TripPace.TYPICAL, rows.single().pace)
    }

    @Test
    fun `la date de la ligne est celle du départ`() {
        val rows = listOf(trip("t", durationMinutes = 30)).recentTripRows(Direction.ALLER, average = null)

        assertEquals(day, rows.single().at)
    }
}
