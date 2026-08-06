package fr.whitytoes.badgemoi.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.theme.numeric

/**
 * Champ chiffré des bandeaux : un libellé discret surmontant une valeur en monospace.
 *
 * Partagé par le bandeau Départ / Écoulé de l'écran actif (§3.2) et le bandeau
 * Départ / Arrivée du récapitulatif (§3.3) — même rôle, même rendu, une seule définition.
 *
 * [value] à `null` affiche le tiret des valeurs absentes plutôt qu'un vide, pour que la
 * ligne garde sa hauteur et que l'absence se distingue d'un oubli d'affichage.
 *
 * La valeur emprunte les métriques de `titleLarge` : c'est le chiffre que l'on lit d'un
 * coup d'œil en roulant, et la zone de statut est la seule qui puisse lui donner cette
 * taille sans bousculer une liste.
 *
 * @param computed distingue une valeur **calculée** d'une valeur **saisie**. Le bandeau du
 *   récapitulatif pose les deux côte à côte : le départ et l'arrivée s'y corrigent d'un
 *   appui, la durée du trajet s'en déduit et ne s'édite pas. Sans marque visible, rien ne
 *   dit laquelle des trois cellules répondra au doigt. Le poids et la couleur portent donc
 *   cette différence — la taille, elle, ne bouge pas, sous peine de rompre l'alignement
 *   des lignes de base.
 */
@Composable
fun LabelledValue(
    @StringRes labelRes: Int,
    value: String?,
    modifier: Modifier = Modifier,
    computed: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: stringResource(R.string.milestone_no_value),
            style =
                MaterialTheme.typography.titleLarge.numeric(
                    weight = if (computed) FontWeight.Medium else FontWeight.ExtraBold,
                ),
            color =
                if (computed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
    }
}
