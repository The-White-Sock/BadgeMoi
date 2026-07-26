package fr.whitytoes.badgemoi.ui.theme

import fr.whitytoes.badgemoi.domain.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Résolution de la préférence de thème. La règle du cahier §4.6 tient en une phrase :
 * un choix explicite prime toujours sur le réglage de l'appareil.
 */
class ThemeResolutionTest {
    @Test
    fun `un choix explicite ignore le thème de l'appareil`() {
        assertTrue(ThemeMode.NIGHT.isDark(systemInDarkTheme = false))
        assertFalse(ThemeMode.DAY.isDark(systemInDarkTheme = true))
    }

    @Test
    fun `le mode système suit l'appareil`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `la bascule passe au contraire du thème affiché`() {
        assertEquals(ThemeMode.DAY, ThemeMode.NIGHT.toggled(systemInDarkTheme = false))
        assertEquals(ThemeMode.NIGHT, ThemeMode.DAY.toggled(systemInDarkTheme = true))
    }

    @Test
    fun `depuis le mode système la bascule fige le mode opposé à l'appareil`() {
        assertEquals(ThemeMode.DAY, ThemeMode.SYSTEM.toggled(systemInDarkTheme = true))
        assertEquals(ThemeMode.NIGHT, ThemeMode.SYSTEM.toggled(systemInDarkTheme = false))
    }
}
