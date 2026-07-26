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

    /** On corrige toujours après coup : l'heure courante par défaut est 08h00. */
    private val now = Instant.parse("2026-07-26T06:00:00Z")

    private fun trip(departureAt: Instant = departure) =
        Trip.start(id = "t1", direction = Direction.ALLER, departureAt = departureAt)

    @Test
    fun `l'heure saisie est placée sur le jour du jalon de référence`() {
        val corrected = trip().correctionInstant(1, hour = 7, minute = 25, now = now, zone = paris)

        assertEquals(Instant.parse("2026-07-26T05:25:00Z"), corrected)
    }

    @Test
    fun `la correction est exprimée dans le fuseau local, pas en UTC`() {
        // 07h25 à Paris en été correspond à 05h25 UTC : une conversion naïve en UTC
        // décalerait tous les jalons de deux heures.
        val corrected = trip().correctionInstant(1, hour = 7, minute = 25, now = now, zone = paris)

        assertEquals(7, corrected.atZone(paris).hour)
        assertEquals(25, corrected.atZone(paris).minute)
    }

    /**
     * Le cas piégeux : un trajet qui franchit minuit. Placer 00h05 sur le jour du départ
     * le situerait *avant* celui-ci, soit un décalage de 24 heures.
     */
    @Test
    fun `un jalon après minuit est reporté au lendemain`() {
        // Départ à 23h50 heure de Paris, correction passée 00h20 — donc après le jalon.
        val nuit = trip(Instant.parse("2026-07-26T21:50:00Z"))
        val apresMinuit = Instant.parse("2026-07-26T22:20:00Z")

        val corrected = nuit.correctionInstant(1, hour = 0, minute = 5, now = apresMinuit, zone = paris)

        assertEquals(Instant.parse("2026-07-26T22:05:00Z"), corrected)
        assertEquals("le jalon suit bien le départ", true, corrected.isAfter(nuit.departureAt))
    }

    @Test
    fun `la référence est le dernier jalon posé, pas le départ`() {
        val nuit =
            trip(Instant.parse("2026-07-26T21:50:00Z"))
                .poseMilestone(1, Instant.parse("2026-07-26T22:30:00Z")) // 00h30 le 27
        val apresMinuit = Instant.parse("2026-07-26T23:00:00Z")

        val corrected = nuit.correctionInstant(2, hour = 0, minute = 45, now = apresMinuit, zone = paris)

        assertEquals(Instant.parse("2026-07-26T22:45:00Z"), corrected)
    }

    /**
     * Corriger le départ vers une heure antérieure est légitime — l'avancer d'un jour
     * serait absurde.
     */
    @Test
    fun `corriger le départ vers une heure antérieure ne le reporte pas au lendemain`() {
        val corrected = trip().correctionInstant(0, hour = 6, minute = 30, now = now, zone = paris)

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
        val soir = Instant.parse("2026-07-26T17:35:00Z")

        val corrected = trajet.correctionInstant(2, hour = 19, minute = 0, now = soir, zone = paris)

        assertEquals(Instant.parse("2026-07-26T17:00:00Z"), corrected)
    }

    /** Même règle pour un jalon déjà posé que l'on rectifie de quelques minutes. */
    @Test
    fun `rectifier un jalon déjà posé vers l'arrière ne change pas de jour`() {
        val trajet =
            trip()
                .poseMilestone(1, Instant.parse("2026-07-26T05:20:00Z")) // 07h20
                .poseMilestone(2, Instant.parse("2026-07-26T05:35:00Z")) // 07h35

        val corrected = trajet.correctionInstant(2, hour = 7, minute = 22, now = now, zone = paris)

        assertEquals(Instant.parse("2026-07-26T05:22:00Z"), corrected)
    }

    /**
     * Le dérapage du critère de proximité : au-delà de 12 h d'écart entre l'heure saisie
     * et celle de la référence, le lendemain devient « plus proche » que le même jour.
     * Référence à 19h00, saisie à 06h00 : +11 h contre −13 h. Le jalon partait dans le
     * futur et le temps écoulé du bandeau, borné à zéro, se figeait.
     */
    @Test
    fun `au-delà de douze heures d'écart le jalon ne bascule pas au lendemain`() {
        // Départ à 19h00, on corrige le jalon 1 à 06h00, alors qu'il est 19h25.
        val soir = trip(Instant.parse("2026-07-26T17:00:00Z"))
        val maintenant = Instant.parse("2026-07-26T17:25:00Z")

        val corrected = soir.correctionInstant(1, hour = 6, minute = 0, now = maintenant, zone = paris)

        assertEquals("le même jour, pas le lendemain", 26, corrected.atZone(paris).dayOfMonth)
        assertEquals(6, corrected.atZone(paris).hour)
        assertEquals("le jalon reste dans le passé", false, corrected.isAfter(maintenant))
    }

    /**
     * Contre-partie du garde-fou : une heure saisie légèrement en avance sur l'horloge —
     * arrondi à la minute supérieure, montre en avance — doit rester le jour même. Un
     * refus strict du futur la renverrait à la veille, soit 24 h d'erreur.
     */
    @Test
    fun `une heure saisie juste en avance sur l'horloge reste le jour même`() {
        val trajet = trip().poseMilestone(1, Instant.parse("2026-07-26T17:50:00Z")) // 19h50
        val maintenant = Instant.parse("2026-07-26T18:05:00Z") // 20h05

        // 20h10, cinq minutes après l'horloge.
        val corrected = trajet.correctionInstant(2, hour = 20, minute = 10, now = maintenant, zone = paris)

        assertEquals(Instant.parse("2026-07-26T18:10:00Z"), corrected)
    }

    /**
     * La tolérance reste étroite : une saisie très en avance sur l'horloge relève de
     * l'erreur, pas de l'arrondi, et ne doit pas placer le jalon dans le futur.
     */
    @Test
    fun `une heure saisie très en avance sur l'horloge est ramenée en arrière`() {
        val trajet = trip().poseMilestone(1, Instant.parse("2026-07-26T17:50:00Z")) // 19h50
        val maintenant = Instant.parse("2026-07-26T18:05:00Z") // 20h05

        // 06h00 : ni le lendemain (futur de dix heures), ni la veille — le jour même.
        val corrected = trajet.correctionInstant(2, hour = 6, minute = 0, now = maintenant, zone = paris)

        assertEquals("le même jour", 26, corrected.atZone(paris).dayOfMonth)
        assertEquals(6, corrected.atZone(paris).hour)
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
        val apresMinuit = Instant.parse("2026-10-24T22:20:00Z")

        val corrected = nuit.correctionInstant(1, hour = 0, minute = 5, now = apresMinuit, zone = paris)

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
