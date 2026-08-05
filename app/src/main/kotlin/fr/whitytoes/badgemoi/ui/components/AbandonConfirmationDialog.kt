package fr.whitytoes.badgemoi.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme

/**
 * Confirmation d'abandon d'un trajet.
 *
 * L'abandon efface le trajet sans retour possible et l'application s'utilise en roulant :
 * la confirmation n'est pas une politesse. Elle est demandée partout où l'action est
 * offerte — fenêtre de reprise (§3.2) et récapitulatif (§3.3) — d'où une définition
 * unique plutôt que deux fenêtres identiques à maintenir de front.
 *
 * Les libellés sont paramétrables pour la seule raison qu'un trajet **archivé** se
 * supprime là où un trajet en cours s'abandonne : le geste est le même, le mot non.
 */
@Composable
fun AbandonConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    @StringRes titleRes: Int = R.string.abandon_dialog_title,
    @StringRes messageRes: Int = R.string.abandon_dialog_message,
    @StringRes confirmRes: Int = R.string.abandon_action,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(confirmRes),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.abandon_dialog_dismiss))
            }
        },
    )
}

@Preview(name = "Abandon — nuit")
@Composable
private fun AbandonConfirmationDialogNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        AbandonConfirmationDialog(onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "Abandon — jour")
@Composable
private fun AbandonConfirmationDialogDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        AbandonConfirmationDialog(onConfirm = {}, onDismiss = {})
    }
}
