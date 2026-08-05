package fr.whitytoes.badgemoi.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.ui.labelRes
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/** Minimum Material pour une cible tactile (`docs/ergonomie.md` §4). */
private val TouchTargetHeight = 48.dp

/** Délai au bout duquel « Effacer » se désarme seul (cahier §3.4, mécanisme du POC). */
private val ClearArmingWindow = 3.seconds

/**
 * Bandeau haut de l'Historique : sélecteur de sens, et « Effacer ».
 *
 * L'action destructive siège dans la zone la moins commode, comme au récapitulatif
 * (`docs/ergonomie.md` §3). Elle a en plus son propre garde-fou : un premier appui l'arme,
 * un second dans les trois secondes purge, et elle se désarme seule sinon (§3.4).
 */
@Composable
fun HistoryHeader(
    direction: Direction,
    purging: Boolean,
    onSelectDirection: (Direction) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            Direction.entries.forEach { entry ->
                DirectionTab(
                    direction = entry,
                    selected = entry == direction,
                    onClick = { onSelectDirection(entry) },
                )
            }
        }
        ClearButton(purging = purging, onClear = onClear)
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

/**
 * « Effacer » à double appui.
 *
 * L'état d'armement est volontairement en `remember` et non `rememberSaveable` : une
 * recréation du processus doit **désarmer**, jamais laisser une purge à un appui de
 * distance.
 */
@Composable
private fun ClearButton(
    purging: Boolean,
    onClear: () -> Unit,
) {
    var armed by remember { mutableStateOf(false) }

    LaunchedEffect(armed) {
        if (armed) {
            delay(ClearArmingWindow)
            armed = false
        }
    }

    TextButton(
        onClick = {
            if (armed) {
                armed = false
                onClear()
            } else {
                armed = true
            }
        },
        // Le verrou du ViewModel empêche déjà la double écriture ; celui-ci le rend
        // visible, pour que l'appui suivant ne semble pas ignoré.
        enabled = !purging,
        modifier = Modifier.heightIn(min = TouchTargetHeight),
    ) {
        Text(
            text = stringResource(if (armed) R.string.history_clear_confirm else R.string.history_clear),
            color = MaterialTheme.colorScheme.error,
        )
    }
}
