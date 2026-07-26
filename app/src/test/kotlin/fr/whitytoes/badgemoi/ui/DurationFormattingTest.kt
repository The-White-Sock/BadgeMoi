@file:Suppress("MagicNumber") // Données de test : durées en clair.

package fr.whitytoes.badgemoi.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationFormattingTest {
    @Test
    fun `une durée nulle s'affiche en minutes et secondes`() {
        assertEquals("0:00", formatDuration(Duration.ZERO))
    }

    @Test
    fun `les secondes sont complétées à deux chiffres`() {
        assertEquals("0:07", formatDuration(7.seconds))
        assertEquals("1:05", formatDuration(65.seconds))
    }

    @Test
    fun `les minutes ne sont pas complétées sous l'heure`() {
        assertEquals("12:34", formatDuration(12.minutes + 34.seconds))
    }

    @Test
    fun `au-delà de l'heure le format passe à trois champs`() {
        assertEquals("1:02:03", formatDuration(1.hours + 2.minutes + 3.seconds))
        assertEquals("1:00:00", formatDuration(1.hours))
    }

    @Test
    fun `les fractions de seconde sont tronquées, pas arrondies`() {
        assertEquals("0:01", formatDuration(1.seconds + 900.milliseconds))
    }

    /**
     * Un horodatage corrigé avant le jalon précédent produit une durée négative :
     * l'afficher tel quel n'aiderait pas, on la ramène à zéro.
     */
    @Test
    fun `une durée négative est ramenée à zéro`() {
        assertEquals("0:00", formatDuration(-30.seconds))
    }
}
