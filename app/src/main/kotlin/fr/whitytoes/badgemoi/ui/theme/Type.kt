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
        variableFont(R.font.manrope, FontWeight.Normal),
        variableFont(R.font.manrope, FontWeight.Medium),
        variableFont(R.font.manrope, FontWeight.Bold),
        variableFont(R.font.manrope, FontWeight.ExtraBold),
    )

/** JetBrains Mono : valeurs chiffrées uniquement (heures, chronomètres). */
private val MonoFontFamily =
    FontFamily(
        variableFont(R.font.jetbrains_mono, FontWeight.Normal),
        variableFont(R.font.jetbrains_mono, FontWeight.Medium),
        variableFont(R.font.jetbrains_mono, FontWeight.Bold),
        variableFont(R.font.jetbrains_mono, FontWeight.ExtraBold),
    )

/** L'échelle typographique Material 3, dont on ne reprend que les **métriques**. */
private val MaterialScale = Typography()

/**
 * Reprend un style de l'échelle Material dans la police d'interface, en ne touchant qu'à
 * la **police** et, au besoin, à la **graisse** : taille, interligne et interlettrage
 * restent ceux de Material.
 *
 * C'est le point du correctif : la déclaration précédente construisait des `TextStyle`
 * neufs, qui n'héritaient donc d'aucune métrique. Toutes les tailles étaient `Unspecified`
 * et retombaient sur la valeur par défaut du moteur de texte — `titleLarge` rendait à la
 * même taille que `bodyMedium`, et la hiérarchie ne tenait plus qu'à la graisse.
 */
private fun TextStyle.sans(weight: FontWeight? = null) =
    copy(fontFamily = SansFontFamily, fontWeight = weight ?: fontWeight)

/**
 * Échelle Material 3 habillée de Manrope.
 *
 * **Tous** les styles sont déclinés, y compris ceux qu'aucun écran n'appelle
 * directement : les composants Material y puisent d'eux-mêmes — `AlertDialog` prend
 * `headlineSmall` pour son titre, `Tab` prend `titleSmall` — et un style laissé de côté
 * s'afficherait dans la police système au milieu du reste.
 *
 * Les graisses appuyées sont conservées sur les cinq rôles que les écrans emploient ;
 * les autres gardent celle de Material.
 */
val BadgeMoiTypography =
    Typography(
        displayLarge = MaterialScale.displayLarge.sans(),
        displayMedium = MaterialScale.displayMedium.sans(),
        displaySmall = MaterialScale.displaySmall.sans(),
        headlineLarge = MaterialScale.headlineLarge.sans(),
        headlineMedium = MaterialScale.headlineMedium.sans(),
        headlineSmall = MaterialScale.headlineSmall.sans(FontWeight.Bold),
        titleLarge = MaterialScale.titleLarge.sans(FontWeight.ExtraBold),
        titleMedium = MaterialScale.titleMedium.sans(FontWeight.Bold),
        titleSmall = MaterialScale.titleSmall.sans(),
        bodyLarge = MaterialScale.bodyLarge.sans(FontWeight.Medium),
        bodyMedium = MaterialScale.bodyMedium.sans(FontWeight.Medium),
        bodySmall = MaterialScale.bodySmall.sans(),
        labelLarge = MaterialScale.labelLarge.sans(FontWeight.Bold),
        labelMedium = MaterialScale.labelMedium.sans(),
        labelSmall = MaterialScale.labelSmall.sans(),
    )

/**
 * Décline un style du thème en **valeur chiffrée** : monospace, et jamais la police
 * d'interface (POC : `--mono`, employé pour tous les `.f-val`/`.mrow-val`).
 *
 * Une extension et non un style autonome, pour la même raison que [sans] : la valeur doit
 * emprunter ses métriques au rôle Material qui lui convient — un chronomètre de bandeau
 * n'a pas la taille d'une durée de liste — au lieu de retomber sur la taille par défaut du
 * moteur de texte, la même pour toutes.
 */
fun TextStyle.numeric(weight: FontWeight = FontWeight.ExtraBold) =
    copy(fontFamily = MonoFontFamily, fontWeight = weight)
