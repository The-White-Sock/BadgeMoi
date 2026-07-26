package fr.whitytoes.badgemoi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import fr.whitytoes.badgemoi.R

/**
 * Déclare une graisse d'une police **variable** : le même fichier sert toutes les
 * graisses, via l'axe `wght`. Exploitable dès l'API 26, en deçà du `minSdk 29`.
 *
 * Les polices sont embarquées dans l'APK (cahier §4.7). Le POC les chargeait depuis
 * Google Fonts ; les embarquer supprime toute dépendance réseau à l'exécution et évite
 * d'avoir à déclarer la permission `INTERNET` (§4.8).
 *
 * Licences SIL Open Font License 1.1, embarquées avec les polices
 * (`res/raw/ofl_manrope.txt`, `res/raw/ofl_jetbrains_mono.txt`).
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(
    resId: Int,
    weight: FontWeight,
) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Manrope : texte d'interface. */
private val SansFontFamily =
    FontFamily(
        variableFont(R.font.manrope, FontWeight.Medium),
        variableFont(R.font.manrope, FontWeight.Bold),
        variableFont(R.font.manrope, FontWeight.ExtraBold),
    )

/** JetBrains Mono : valeurs chiffrées uniquement (heures, chronomètres). */
private val MonoFontFamily =
    FontFamily(
        variableFont(R.font.jetbrains_mono, FontWeight.Medium),
        variableFont(R.font.jetbrains_mono, FontWeight.Bold),
        variableFont(R.font.jetbrains_mono, FontWeight.ExtraBold),
    )

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
