package fr.whitytoes.badgemoi.ui.summary

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
 * Emplacement de l'écran Récapitulatif. Le contenu réel — relecture avant archivage,
 * durées par tronçon, boutons Annuler / Enregistrer — est livré au lot 4 (cahier §3.3).
 */
@Composable
fun SummaryScreen(modifier: Modifier = Modifier) {
    ScreenScaffold(modifier = modifier) {
        Text(
            text = stringResource(R.string.summary_coming_soon),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).padding(16.dp),
        )
    }
}
