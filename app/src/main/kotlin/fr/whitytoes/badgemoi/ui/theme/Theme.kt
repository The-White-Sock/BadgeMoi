package fr.whitytoes.badgemoi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens n'ayant pas d'équivalent direct dans [androidx.compose.material3.ColorScheme]
 * (bordures atténuées façon POC, vert de statut "plus rapide que la moyenne").
 * Accessible via `BadgeMoiTheme.extendedColors` dans les composables enfants.
 */
data class BadgeMoiExtendedColors(
    val amberDim: Color,
    val tealDim: Color,
    val success: Color,
)

private val LocalBadgeMoiExtendedColors =
    staticCompositionLocalOf {
        BadgeMoiExtendedColors(
            amberDim = NightPalette.amberDim,
            tealDim = NightPalette.tealDim,
            success = NightPalette.green,
        )
    }

private fun nightColorScheme() =
    darkColorScheme(
        primary = NightPalette.amber,
        onPrimary = NightPalette.ctaInk,
        primaryContainer = NightPalette.amberSoft,
        onPrimaryContainer = NightPalette.amber,
        secondary = NightPalette.teal,
        onSecondary = NightPalette.ctaInk,
        secondaryContainer = NightPalette.tealSoft,
        onSecondaryContainer = NightPalette.teal,
        background = NightPalette.background,
        onBackground = NightPalette.ink,
        surface = NightPalette.panel,
        onSurface = NightPalette.ink,
        surfaceVariant = NightPalette.panelAlt,
        onSurfaceVariant = NightPalette.inkSoft,
        outline = NightPalette.line,
        error = NightPalette.red,
        onError = NightPalette.ctaInk,
        errorContainer = NightPalette.redSoft,
        onErrorContainer = NightPalette.red,
    )

private fun dayColorScheme() =
    lightColorScheme(
        primary = DayPalette.amber,
        onPrimary = DayPalette.ctaInk,
        primaryContainer = DayPalette.amberSoft,
        onPrimaryContainer = DayPalette.amber,
        secondary = DayPalette.teal,
        onSecondary = DayPalette.ctaInk,
        secondaryContainer = DayPalette.tealSoft,
        onSecondaryContainer = DayPalette.teal,
        background = DayPalette.background,
        onBackground = DayPalette.ink,
        surface = DayPalette.panel,
        onSurface = DayPalette.ink,
        surfaceVariant = DayPalette.panelAlt,
        onSurfaceVariant = DayPalette.inkSoft,
        outline = DayPalette.line,
        error = DayPalette.red,
        onError = DayPalette.ctaInk,
        errorContainer = DayPalette.redSoft,
        onErrorContainer = DayPalette.red,
    )

/**
 * Thème racine de l'application. [darkTheme] pilote nuit/jour ; par défaut suit le thème
 * système (docs/cahier-des-charges.md §4.6), mais la préférence utilisateur explicite
 * (persistée en DataStore, lot 1) doit avoir priorité sur cette valeur par défaut.
 */
@Composable
fun BadgeMoiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) nightColorScheme() else dayColorScheme()
    val extendedColors =
        if (darkTheme) {
            BadgeMoiExtendedColors(NightPalette.amberDim, NightPalette.tealDim, NightPalette.green)
        } else {
            BadgeMoiExtendedColors(DayPalette.amberDim, DayPalette.tealDim, DayPalette.green)
        }

    CompositionLocalProvider(LocalBadgeMoiExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BadgeMoiTypography,
            content = content,
        )
    }
}

object BadgeMoiTheme {
    val extendedColors: BadgeMoiExtendedColors
        @Composable
        get() = LocalBadgeMoiExtendedColors.current
}
