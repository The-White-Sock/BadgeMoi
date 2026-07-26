package fr.whitytoes.badgemoi.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold

/**
 * Emplacement de l'écran d'accueil. Le contenu réel — boutons Aller/Retour ancrés en
 * bas, bannière de reprise d'un trajet en cours — est livré par l'issue suivante du
 * lot 2 (cahier §3.1).
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    ScreenScaffold(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_coming_soon),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).padding(16.dp),
        )
    }
}
