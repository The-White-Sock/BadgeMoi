package fr.whitytoes.badgemoi.ui.history

import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.ui.labelRes

/** Minimum Material pour une cible tactile (`docs/ergonomie.md` §4). */
internal val TouchTargetHeight = 48.dp

/**
 * Bandeau haut de l'Historique : le **sélecteur de sens**, et rien d'autre.
 *
 * La suppression y siégeait ; elle est descendue au niveau du titre « Trajets récents »,
 * qui est ce sur quoi elle agit.
 */
@Composable
fun HistoryHeader(
    direction: Direction,
    onSelectDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Direction.entries.forEach { entry ->
            DirectionTab(
                direction = entry,
                selected = entry == direction,
                onClick = { onSelectDirection(entry) },
            )
        }
    }
}

@Composable
private fun DirectionTab(
    direction: Direction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    // Même langue visuelle que les onglets du bandeau haut : l'accent porte la sélection,
    // le reste est atténué.
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = TouchTargetHeight)) {
        Text(
            text = stringResource(direction.labelRes()),
            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
        )
    }
}
