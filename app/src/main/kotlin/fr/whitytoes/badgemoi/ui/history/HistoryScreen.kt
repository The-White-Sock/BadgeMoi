package fr.whitytoes.badgemoi.ui.history

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
 * Emplacement de l'écran Historique. Le contenu réel — moyennes par tronçon, liste des
 * trajets récents, export CSV, purge — est livré au lot 5 (cahier §3.4).
 */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    ScreenScaffold(modifier = modifier) {
        Text(
            text = stringResource(R.string.history_coming_soon),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).padding(16.dp),
        )
    }
}
