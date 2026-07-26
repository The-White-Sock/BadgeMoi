@file:Suppress("MagicNumber") // Données de test : instants en millisecondes, en clair.

package fr.whitytoes.badgemoi.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Verrou anti-double-tap de « Valider » (400 ms, seuil du POC). Le scénario redouté est
 * celui du board qui vibre : un appui unique enregistré deux fois poserait deux jalons.
 */
class TapGuardTest {
    private fun guard() = TapGuard(400.milliseconds)

    @Test
    fun `le premier appui est toujours accepté`() {
        assertTrue(guard().accept(nowMillis = 0))
    }

    @Test
    fun `un second appui trop rapproché est rejeté`() {
        val guard = guard()
        guard.accept(1_000)

        assertFalse(guard.accept(1_100))
    }

    @Test
    fun `un appui au-delà de la fenêtre est accepté`() {
        val guard = guard()
        guard.accept(1_000)

        assertTrue(guard.accept(1_400))
    }

    @Test
    fun `la borne de la fenêtre est inclusive`() {
        val guard = guard()
        guard.accept(0)

        assertFalse("399 ms est encore dans la fenêtre", guard.accept(399))
        assertTrue("400 ms la referme", guard.accept(400))
    }

    /**
     * Un appui rejeté ne doit pas décaler la fenêtre : sinon une rafale de rebonds
     * repousserait le verrou indéfiniment et l'utilisateur ne pourrait plus valider.
     */
    @Test
    fun `une rafale d'appuis rejetés ne prolonge pas le verrou`() {
        val guard = guard()
        guard.accept(0)

        (50L until 400L step 50).forEach { assertFalse(guard.accept(it)) }

        assertTrue("le verrou expire bien 400 ms après le dernier appui accepté", guard.accept(400))
    }

    @Test
    fun `des appuis espacés sont tous acceptés`() {
        val guard = guard()

        assertTrue(guard.accept(0))
        assertTrue(guard.accept(500))
        assertTrue(guard.accept(1_000))
    }
}
