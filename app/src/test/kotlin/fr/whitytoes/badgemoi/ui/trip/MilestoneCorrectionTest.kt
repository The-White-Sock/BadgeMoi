@file:Suppress("MagicNumber") // Données de test : heures et indices en clair.

package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * Conversion de l'heure locale saisie en [Instant]. Le sélecteur ne rend qu'une heure et
 * des minutes : le jour doit être déduit, et c'est là que se cachent les erreurs.
 */
class MilestoneCorrectionTest {
    private val paris = ZoneId.of("Europe/Paris")

    /** 26/07/2026, 07h00 heure de Paris. */
    private val departure = Instant.parse("2026-07-26T05:00:00Z")

    private fun trip(departureAt: Instant = departure) =
        Trip.start(id = "t1", direction = Direction.ALLER, departureAt = departureAt)

    @Test
    fun `l'heure saisie est placée sur le jour du jalon de référence`() {
        val corrected = trip().correctionInstant(index = 1, hour = 7, minute = 25, zone = paris)

        assertEquals(Instant.parse("2026-07-26T05:25:00Z"), corrected)
    }

    @Test
    fun `la correction est exprimée dans le fuseau local, pas en UTC`() {
        // 07h25 à Paris en été correspond à 05h25 UTC : une conversion naïve en UTC
        // décalerait tous les jalons de deux heures.
        val corrected = trip().correctionInstant(index = 1, hour = 7, minute = 25, zone = paris)

        assertEquals(7, corrected.atZone(paris).hour)
        assertEquals(25, corrected.atZone(paris).minute)
    }

    /**
     * Le cas piégeux : un trajet qui franchit minuit. Placer 00h05 sur le jour du départ
     * le situerait *avant* celui-ci, soit un décalage de 24 heures.
     */
    @Test
    fun `un jalon après minuit est reporté au lendemain`() {
        // Départ à 23h50 heure de Paris.
        val nuit = trip(Instant.parse("2026-07-26T21:50:00Z"))

        val corrected = nuit.correctionInstant(index = 1, hour = 0, minute = 5, zone = paris)

        assertEquals(Instant.parse("2026-07-26T22:05:00Z"), corrected)
        assertEquals("le jalon suit bien le départ", true, corrected.isAfter(nuit.departureAt))
    }

    @Test
    fun `la référence est le dernier jalon posé, pas le départ`() {
        val nuit =
            trip(Instant.parse("2026-07-26T21:50:00Z"))
                .poseMilestone(1, Instant.parse("2026-07-26T22:30:00Z")) // 00h30 le 27

        val corrected = nuit.correctionInstant(index = 2, hour = 0, minute = 45, zone = paris)

        assertEquals(Instant.parse("2026-07-26T22:45:00Z"), corrected)
    }

    /**
     * Corriger le départ vers une heure antérieure est légitime — l'avancer d'un jour
     * serait absurde.
     */
    @Test
    fun `corriger le départ vers une heure antérieure ne le reporte pas au lendemain`() {
        val corrected = trip().correctionInstant(index = 0, hour = 6, minute = 30, zone = paris)

        assertEquals(Instant.parse("2026-07-26T04:30:00Z"), corrected)
    }

    /**
     * Le défaut corrigé par l'issue #56 : rectifier un jalon vers une heure légèrement
     * antérieure — on a pointé un peu tard — le reportait au lendemain, soit 24 heures
     * d'écart affichées sur le tronçon.
     */
    @Test
    fun `corriger un jalon vers une heure antérieure le laisse le même jour`() {
        // Jalon 1 posé à 19h29 heure de Paris ; on rectifie le jalon 2 à 19h00.
        val trajet = trip().poseMilestone(1, Instant.parse("2026-07-26T17:29:00Z"))

        val corrected = trajet.correctionInstant(index = 2, hour = 19, minute = 0, zone = paris)

        assertEquals(Instant.parse("2026-07-26T17:00:00Z"), corrected)
    }

    /** Même règle pour un jalon déjà posé que l'on rectifie de quelques minutes. */
    @Test
    fun `rectifier un jalon déjà posé vers l'arrière ne change pas de jour`() {
        val trajet =
            trip()
                .poseMilestone(1, Instant.parse("2026-07-26T05:20:00Z")) // 07h20
                .poseMilestone(2, Instant.parse("2026-07-26T05:35:00Z")) // 07h35

        val corrected = trajet.correctionInstant(index = 2, hour = 7, minute = 22, zone = paris)

        assertEquals(Instant.parse("2026-07-26T05:22:00Z"), corrected)
    }

    /**
     * Le changement d'heure d'octobre : le décalage de jour se fait sur le calendrier
     * local, l'heure murale saisie est donc conservée telle quelle. Un décalage de
     * 24 heures fixes aurait rendu 00h05 en 23h05 la veille.
     */
    @Test
    fun `le report au lendemain conserve l'heure murale malgré le changement d'heure`() {
        // Départ le 24/10/2026 à 23h50 heure de Paris (UTC+2, avant le basculement).
        val nuit = trip(Instant.parse("2026-10-24T21:50:00Z"))

        val corrected = nuit.correctionInstant(index = 1, hour = 0, minute = 5, zone = paris)

        assertEquals(25, corrected.atZone(paris).dayOfMonth)
        assertEquals(0, corrected.atZone(paris).hour)
        assertEquals(5, corrected.atZone(paris).minute)
    }

    @Test
    fun `le sélecteur est présélectionné sur l'heure du jalon quand elle existe`() {
        val pose = Instant.parse("2026-07-26T05:20:00Z")

        assertEquals(pose, trip().poseMilestone(1, pose).correctionSeedInstant(1))
    }

    @Test
    fun `un jalon non posé présélectionne l'heure du jalon précédent`() {
        assertEquals(departure, trip().correctionSeedInstant(3))
    }
}
