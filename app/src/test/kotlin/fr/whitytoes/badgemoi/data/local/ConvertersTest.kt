@file:Suppress("MagicNumber") // Données de test : horodatages en clair.

package fr.whitytoes.badgemoi.data.local

import fr.whitytoes.badgemoi.domain.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Les convertisseurs Room sérialisent les listes en colonne texte. Le point
 * sensible est le `null` (jalon non posé) : il doit survivre à l'aller-retour,
 * sinon un trajet relu depuis la base paraîtrait complet à tort.
 */
class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `les horodatages survivent à l'aller-retour en conservant les jalons non posés`() {
        val times = listOf(1_000L, null, 3_000L, null, 5_000L)

        val restored = converters.jsonToTimes(converters.timesToJson(times))

        assertEquals(times, restored)
    }

    @Test
    fun `une liste d'horodatages entièrement vide survit à l'aller-retour`() {
        val times = List<Long?>(Routes.MILESTONE_COUNT) { null }

        assertEquals(times, converters.jsonToTimes(converters.timesToJson(times)))
    }

    @Test
    fun `les jalons ignorés survivent à l'aller-retour`() {
        val skipped = listOf(false, true, false, true, false)

        assertEquals(skipped, converters.jsonToSkipped(converters.skippedToJson(skipped)))
    }
}
