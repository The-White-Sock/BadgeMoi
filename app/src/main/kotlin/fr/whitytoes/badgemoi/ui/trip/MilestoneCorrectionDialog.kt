package fr.whitytoes.badgemoi.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.components.AppTextButton
import java.time.Instant
import java.time.ZoneId

/**
 * Overlay de correction d'un jalon (cahier des charges §3.5).
 *
 * Les quatre actions du POC sont reprises telles quelles : enregistrer l'heure saisie,
 * marquer le jalon comme ignoré, l'effacer (retour en attente), ou renoncer.
 *
 * « Effacer » et « Ignorer » sont volontairement dissociés : le premier remet le jalon
 * à traiter, le second acte qu'on est passé sans pointer. Les confondre reviendrait à
 * perdre l'information qui distingue un tronçon non mesurable d'un tronçon à venir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneCorrectionDialog(
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

    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(text = label, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AppTextButton(onClick = actions.onSkip) {
                        Text(stringResource(R.string.correction_skip))
                    }
                    AppTextButton(onClick = actions.onClear) {
                        Text(
                            text = stringResource(R.string.correction_clear),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = { actions.onSave(state.hour, state.minute) }) {
                Text(stringResource(R.string.correction_save))
            }
        },
        dismissButton = {
            AppTextButton(onClick = actions.onDismiss) {
                Text(stringResource(R.string.correction_cancel))
            }
        },
    )
}
