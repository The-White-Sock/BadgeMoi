package fr.whitytoes.badgemoi.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import java.time.Instant
import java.time.ZoneId

private val PrimaryActionHeight = 56.dp

/** Minimum Material pour une cible tactile (`docs/ergonomie.md` §4). */
private val TouchTargetHeight = 48.dp

/**
 * Correction d'un jalon (cahier des charges §3.5).
 *
 * **Feuille basse et non fenêtre centrée** : c'est la modale la plus utilisée de
 * l'application, et la seule qu'on ouvre en roulant. Une `AlertDialog` centrée plaçait ses
 * boutons vers le milieu de l'écran, hors de la zone naturelle du pouce
 * (`docs/ergonomie.md` §3).
 *
 * Les quatre actions du POC sont reprises telles quelles : enregistrer l'heure saisie,
 * marquer le jalon comme ignoré, l'effacer (retour en attente), ou renoncer.
 *
 * « Effacer » et « Ignorer » sont volontairement dissociés : le premier remet le jalon
 * à traiter, le second acte qu'on est passé sans pointer. Les confondre reviendrait à
 * perdre l'information qui distingue un tronçon non mesurable d'un tronçon à venir.
 *
 * « Effacer » est destructif, donc tenu à distance de « Enregistrer » : une rangée les
 * sépare, là où la fenêtre les mettait à quelques millimètres l'un de l'autre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneCorrectionSheet(
    label: String,
    seedAt: Instant,
    actions: MilestoneCorrectionActions,
) {
    val seed = seedAt.atZone(ZoneId.systemDefault())
    val state =
        rememberTimePickerState(
            initialHour = seed.hour,
            initialMinute = seed.minute,
            // Affichage 24 h : l'application est francophone et affiche partout des
            // heures de trajet, jamais de format AM/PM.
            is24Hour = true,
        )

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        // Pas de demi-hauteur : le cadran du sélecteur d'heure doit être entier dès
        // l'ouverture, sans geste préalable.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            // La feuille vit dans sa propre fenêtre : les encarts système consommés par
            // `BadgeMoiApp` ne s'y appliquent pas, il faut les reprendre ici.
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            TimePicker(state = state)
            SecondaryActions(onSkip = actions.onSkip, onClear = actions.onClear)
            PrimaryActions(
                onDismiss = actions.onDismiss,
                onSave = { actions.onSave(state.hour, state.minute) },
            )
        }
    }
}

/** « Ignorer » et « Effacer », une rangée au-dessus de l'action courante. */
@Composable
private fun SecondaryActions(
    onSkip: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = TouchTargetHeight)) {
            Text(stringResource(R.string.correction_skip))
        }
        TextButton(onClick = onClear, modifier = Modifier.heightIn(min = TouchTargetHeight)) {
            Text(
                text = stringResource(R.string.correction_clear),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * « Enregistrer » prend deux tiers de la largeur et le côté droit : c'est l'action
 * courante. Renoncer n'est pas destructif, sa proximité ne pose donc pas de problème.
 */
@Composable
private fun PrimaryActions(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).heightIn(min = PrimaryActionHeight),
        ) {
            Text(stringResource(R.string.correction_cancel))
        }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(2f).heightIn(min = PrimaryActionHeight),
        ) {
            Text(stringResource(R.string.correction_save))
        }
    }
}
