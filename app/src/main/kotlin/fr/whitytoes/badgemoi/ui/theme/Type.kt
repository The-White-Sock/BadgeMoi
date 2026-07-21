package fr.whitytoes.badgemoi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// TODO(lot 2/7) : remplacer par les polices embarquées JetBrains Mono / Manrope
// (docs/cahier-des-charges.md §4.7) une fois les fichiers .ttf ajoutés en ressources.
private val SansFontFamily = FontFamily.Default
private val MonoFontFamily = FontFamily.Monospace

val BadgeMoiTypography =
    Typography(
        titleLarge = TextStyle(fontFamily = SansFontFamily, fontWeight = FontWeight.ExtraBold),
        titleMedium = TextStyle(fontFamily = SansFontFamily, fontWeight = FontWeight.Bold),
        bodyLarge = TextStyle(fontFamily = SansFontFamily, fontWeight = FontWeight.Medium),
        bodyMedium = TextStyle(fontFamily = SansFontFamily, fontWeight = FontWeight.Medium),
        labelLarge = TextStyle(fontFamily = SansFontFamily, fontWeight = FontWeight.Bold),
    )

/**
 * Style des valeurs chiffrées (heures, chronomètres) : toujours en monospace,
 * jamais en police d'interface (POC : `--mono`, utilisé pour tous les `.f-val`/`.mrow-val`).
 */
val numericTextStyle =
    TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
    )
