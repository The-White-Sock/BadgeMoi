package fr.whitytoes.badgemoi.ui.history

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.TripPace
import fr.whitytoes.badgemoi.ui.formatDate
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.labelRes
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Confirmation de suppression d'un trajet archivé (cahier des charges §3.4).
 *
 * **Fenêtre centrée** et non feuille basse : `docs/ergonomie.md` §3 réserve la feuille aux
 * confirmations et aux saisies courantes, alors qu'une suppression irréversible doit
 * interrompre plutôt que se glisser sous le pouce. Même raisonnement que la confirmation
 * d'abandon, restée centrée quand la reprise et la correction sont descendues.
 *
 * Elle **nomme** ce qu'elle va détruire — sens, date, durée. Une liste de dates se
 * ressemble, et l'appui qui a ouvert cette fenêtre peut avoir manqué sa ligne.
 *
 * Pas de double appui à réarmement : ce mécanisme protège la purge de toute l'archive.
 * Ouvrir délibérément une fenêtre qui désigne un trajet est une intention déjà explicite.
 */
@Composable
fun TripDeletionDialog(
    row: RecentTripRow,
    direction: Direction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Un trajet dont l'arrivée n'a pas été pointée reste nommable : « Non mesuré » plutôt
    // qu'un blanc, sinon la phrase se termine sur une virgule en suspens.
    val duration = row.total?.let(::formatDuration) ?: stringResource(R.string.segment_not_measured)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.history_delete_message,
                    stringResource(direction.labelRes()),
                    formatDate(row.at),
                    duration,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.history_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.history_delete_dismiss))
            }
        },
    )
}

@Suppress("MagicNumber") // Données d'aperçu : durée en clair.
private fun previewRow(total: kotlin.time.Duration?) =
    RecentTripRow(
        id = "preview",
        at = Instant.parse("2026-07-26T07:12:00Z"),
        total = total,
        pace = total?.let { TripPace.FASTER },
    )

@Preview(name = "Suppression — nuit")
@Composable
private fun TripDeletionDialogNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        TripDeletionDialog(
            row = previewRow(24.minutes),
            direction = Direction.ALLER,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Suppression — jour")
@Composable
private fun TripDeletionDialogDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        TripDeletionDialog(
            row = previewRow(24.minutes),
            direction = Direction.ALLER,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

/** Trajet dont l'arrivée a été ignorée : la fenêtre doit rester une phrase complète. */
@Preview(name = "Suppression — trajet non mesuré")
@Composable
private fun TripDeletionDialogUnmeasuredPreview() {
    BadgeMoiTheme(darkTheme = true) {
        TripDeletionDialog(
            row = previewRow(null),
            direction = Direction.RETOUR,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
