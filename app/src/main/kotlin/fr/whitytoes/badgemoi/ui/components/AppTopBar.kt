package fr.whitytoes.badgemoi.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme

/** Minimum Material pour une cible tactile (`docs/ergonomie.md` §4). */
private val TabMinHeight = 48.dp

/**
 * Bandeau haut de l'application (cahier des charges §3.1) : onglets de navigation et
 * bascule de thème sur **une seule ligne**. L'espace pris ici l'est sur la zone utile
 * de l'écran, qui sert à saisir un trajet d'une main en roulant — d'où la compacité.
 *
 * La navigation reste **en haut** alors que la règle générale la veut en bas : la zone
 * difficile n'est disqualifiée que pour ce qui est à la fois fréquent et tactile, et l'on
 * bascule d'onglet quelques fois par semaine quand on valide un jalon plusieurs fois par
 * trajet. Le raisonnement complet est en décision 2 de `docs/ergonomie.md`.
 *
 * Composant sans état : l'onglet actif et le thème courant sont fournis par l'appelant.
 * Les onglets sont dérivés de [TopLevelDestination], de sorte qu'en ajouter un plus tard
 * ne change pas la signature.
 */
@Composable
fun AppTopBar(
    selected: TopLevelDestination,
    isDarkTheme: Boolean,
    onSelectDestination: (TopLevelDestination) -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            DestinationTab(
                labelRes = destination.labelRes,
                selected = destination == selected,
                onClick = { onSelectDestination(destination) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onToggleTheme, modifier = Modifier.heightIn(min = TabMinHeight)) {
            // Libellé textuel provisoire : l'icône soleil/lune fait partie du jeu
            // vectoriel définitif livré au lot 7 (cahier §1.5).
            Text(
                text =
                    stringResource(
                        if (isDarkTheme) R.string.theme_switch_to_day else R.string.theme_switch_to_night,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DestinationTab(
    @StringRes labelRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Un `TextButton` mesure 40 dp de haut par défaut, sous le minimum de 48 dp. Les
    // onglets sont rares, pas facultatifs (`docs/ergonomie.md` §5, décision 2).
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = TabMinHeight)) {
        Text(
            text = stringResource(labelRes),
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
        )
    }
}

@Preview(name = "Nuit — onglet Trajet", showBackground = true)
@Composable
private fun AppTopBarNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        AppTopBar(
            selected = TopLevelDestination.TRIP,
            isDarkTheme = true,
            onSelectDestination = {},
            onToggleTheme = {},
        )
    }
}

@Preview(name = "Jour — onglet Historique", showBackground = true)
@Composable
private fun AppTopBarDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        AppTopBar(
            selected = TopLevelDestination.HISTORY,
            isDarkTheme = false,
            onSelectDestination = {},
            onToggleTheme = {},
        )
    }
}
