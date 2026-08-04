package fr.whitytoes.badgemoi.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
 */
@Composable
fun LabelledValue(
    @StringRes labelRes: Int,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: stringResource(R.string.milestone_no_value),
            style = MaterialTheme.typography.titleLarge.numeric(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
