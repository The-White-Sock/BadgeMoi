package fr.whitytoes.badgemoi.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de couleur repris tel quel du POC (docs/poc/trajet.html, custom properties CSS)
 * et du cahier des charges §5. Ne pas coder de couleur en dur ailleurs dans l'appli :
 * passer par [BadgeMoiTheme] / [MaterialTheme.colorScheme] ou [LocalBadgeMoiExtendedColors].
 */
internal object NightPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF171A21)
    val panelAlt = Color(0xFF1D212B)
    val line = Color(0xFF343B49)
    val ink = Color(0xFFF1EFE8)
    val inkSoft = Color(0xFFA7AFC0)
    val inkDim = Color(0xFF5B6376)
    val amber = Color(0xFFFFB020)
    val amberSoft = Color(0x29FFB020)
    val amberDim = Color(0x73FFB020)
    val teal = Color(0xFF3FD1C8)
    val tealSoft = Color(0x293FD1C8)
    val tealDim = Color(0x663FD1C8)
    val red = Color(0xFFFF6259)
    val redSoft = Color(0x29FF6259)
    val green = Color(0xFF5FD98A)
    val ctaInk = Color(0xFF1A1300)
}

internal object DayPalette {
    val background = Color(0xFFFFFFFF)
    val panel = Color(0xFFFFFFFF)
    val panelAlt = Color(0xFFEFEEE7)
    val line = Color(0xFF9AA0AC)
    val ink = Color(0xFF0A0C10)
    val inkSoft = Color(0xFF3A4150)
    val inkDim = Color(0xFF7B8291)
    val amber = Color(0xFFB5540B)
    val amberSoft = Color(0x24B5540B)
    val amberDim = Color(0x8CB5540B)
    val teal = Color(0xFF0B7A73)
    val tealSoft = Color(0x240B7A73)
    val tealDim = Color(0x8C0B7A73)
    val red = Color(0xFFB4231A)
    val redSoft = Color(0x24B4231A)
    val green = Color(0xFF1E7A46)
    val ctaInk = Color(0xFFFFFFFF)
}
