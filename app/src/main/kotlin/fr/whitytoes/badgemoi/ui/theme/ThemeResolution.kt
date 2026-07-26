package fr.whitytoes.badgemoi.ui.theme

import fr.whitytoes.badgemoi.domain.ThemeMode

/**
 * Résout la préférence de thème en un choix concret clair/sombre.
 *
 * [systemInDarkTheme] n'est consulté que pour [ThemeMode.SYSTEM] : un choix explicite
 * de l'utilisateur prime toujours sur le réglage de l'appareil (cahier §4.6).
 */
fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        ThemeMode.DAY -> false
        ThemeMode.NIGHT -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }

/**
 * Mode obtenu en actionnant la bascule du bandeau haut : on passe toujours au contraire
 * de ce qui est **affiché**. Depuis [ThemeMode.SYSTEM], la bascule fige donc la
 * préférence sur le mode opposé au thème système courant, plutôt que de laisser
 * l'utilisateur appuyer sans effet visible.
 *
 * Le choix explicite de « suivre le système » n'est pas proposé ici : cette option
 * relève du lot 7 (cahier §7).
 */
fun ThemeMode.toggled(systemInDarkTheme: Boolean): ThemeMode =
    if (isDark(systemInDarkTheme)) ThemeMode.DAY else ThemeMode.NIGHT
