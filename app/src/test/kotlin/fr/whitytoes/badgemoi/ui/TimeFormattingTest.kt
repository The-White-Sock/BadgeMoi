package fr.whitytoes.badgemoi.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Formats d'heure et de date. Le point à verrouiller est leur **indépendance à la
 * locale** : `ofLocalizedTime` affichait « 7:29 PM » sur un appareil configuré en
 * anglais, alors que l'application est francophone de bout en bout.
 */
class TimeFormattingTest {
    private val paris = ZoneId.of("Europe/Paris")

    /** 26/07/2026, 19h29 heure de Paris. */
    private val soir = Instant.parse("2026-07-26T17:29:00Z")

    @Test
    fun `l'heure est affichée sur 24 heures`() {
        assertEquals("19:29", formatTimeAt(soir, paris))
    }

    @Test
    fun `l'heure est complétée à deux chiffres`() {
        assertEquals("07:05", formatTimeAt(Instant.parse("2026-07-26T05:05:00Z"), paris))
    }

    @Test
    fun `minuit s'affiche 00 et non 24`() {
        assertEquals("00:05", formatTimeAt(Instant.parse("2026-07-26T22:05:00Z"), paris))
    }

    @Test
    fun `la date est au format jour mois année sur deux chiffres`() {
        assertEquals("26/07/26", formatDateAt(soir, paris))
    }

    /**
     * Le défaut signalé : sur un appareil en anglais, le format localisé rendait
     * « 7:29 PM » et « 7/26/26 ». Les motifs figés ne bougent plus avec la locale.
     */
    @Test
    fun `les formats ne dépendent pas de la locale de l'appareil`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)

            assertEquals("19:29", formatTimeAt(soir, paris))
            assertEquals("26/07/26", formatDateAt(soir, paris))
        } finally {
            Locale.setDefault(previous)
        }
    }

    /** L'horodatage est stocké en UTC : il doit se lire dans le fuseau de l'utilisateur. */
    @Test
    fun `l'heure est lue dans le fuseau demandé, pas en UTC`() {
        assertEquals("17:29", formatTimeAt(soir, ZoneId.of("UTC")))
    }
}
