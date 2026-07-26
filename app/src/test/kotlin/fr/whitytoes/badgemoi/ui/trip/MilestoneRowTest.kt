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

class MilestoneRowTest {
    private val departure = Instant.parse("2026-07-26T07:00:00Z")

    private fun trip() = Trip.start(id = "t1", direction = Direction.ALLER, departureAt = departure)

    /** Instant situé [minutesAfter] minutes après le départ. */
    private fun at(minutesAfter: Long): Instant = departure.plusSeconds(minutesAfter * 60)

    @Test
    fun `un trajet neuf expose un départ posé et le jalon suivant comme courant`() {
        val rows = trip().milestoneRows()

        assertEquals(Routes.MILESTONE_COUNT, rows.size)
        assertEquals(MilestoneStatus.POSED, rows[0].status)
        assertEquals(MilestoneStatus.CURRENT, rows[1].status)
        assertEquals(MilestoneStatus.PENDING, rows[2].status)
    }

    @Test
    fun `le départ n'a pas de durée depuis un jalon précédent`() {
        assertNull(trip().milestoneRows()[0].sincePrevious)
    }

    @Test
    fun `les libellés et icônes viennent du parcours du domaine`() {
        val definitions = Routes.forDirection(Direction.ALLER).milestones
        val rows = trip().milestoneRows()

        assertEquals(definitions.map { it.label }, rows.map { it.label })
        assertEquals(definitions.map { it.icon }, rows.map { it.icon })
    }

    @Test
    fun `la durée d'un jalon posé se mesure depuis le jalon précédent`() {
        val rows = trip().poseMilestone(1, at(9)).milestoneRows()

        assertEquals(9.minutes, rows[1].sincePrevious)
    }

    /**
     * Le piège de ce modèle : un jalon ignoré n'a pas d'horodatage. L'écart du jalon
     * suivant doit donc remonter au dernier jalon réellement **posé**, sans quoi la
     * liste afficherait des durées nulles dès qu'un jalon est passé.
     */
    @Test
    fun `un jalon ignoré est enjambé pour le calcul de la durée`() {
        val trip =
            trip()
                .poseMilestone(1, at(10))
                .skipMilestone(2)
                .poseMilestone(3, at(40))

        val rows = trip.milestoneRows()

        assertEquals(MilestoneStatus.SKIPPED, rows[2].status)
        assertNull("un jalon ignoré n'a pas de durée propre", rows[2].sincePrevious)
        assertEquals("l'écart remonte au jalon 1, pas au jalon 2", 30.minutes, rows[3].sincePrevious)
    }

    /**
     * §3.5 : seul un jalon tranché se corrige. Sur un trajet neuf, cela veut dire le
     * départ et rien d'autre — le jalon courant se valide, il ne se rectifie pas.
     */
    @Test
    fun `seuls les jalons posés ou ignorés sont corrigibles`() {
        val rows = trip().skipMilestone(1).milestoneRows()

        assertEquals("le départ, posé", true, rows[0].status.isCorrectable)
        assertEquals("le jalon ignoré", true, rows[1].status.isCorrectable)
        assertEquals("le jalon courant", false, rows[2].status.isCorrectable)
        assertEquals("un jalon à venir", false, rows[3].status.isCorrectable)
    }

    @Test
    fun `un trajet terminé n'a plus de jalon courant`() {
        val complet =
            (1 until Routes.MILESTONE_COUNT).fold(trip()) { trip, index ->
                trip.poseMilestone(index, at(index * 10L))
            }

        val statuses = complet.milestoneRows().map { it.status }

        assertEquals(List(Routes.MILESTONE_COUNT) { MilestoneStatus.POSED }, statuses)
    }
}
