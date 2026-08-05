package fr.whitytoes.badgemoi.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.ui.components.LabelledValue
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.formatTime
import fr.whitytoes.badgemoi.ui.labelRes
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Fenêtre de reprise d'un trajet déjà entamé (§9, écart 10).
 *
 * Elle remplace l'écran d'accueil de reprise, qui affichait deux lignes pour un écran
 * entier. Posée sur l'écran des jalons, elle a derrière elle la frise, les compteurs et
 * les jalons déjà pointés : l'information qui manquait est là, sans être dupliquée.
 *
 * Écarter la fenêtre revient à **reprendre** — c'est l'issue de loin la plus fréquente, et
 * la seule qui ne détruise rien. « Abandonner » demande confirmation par-dessus.
 *
 * @param elapsed lambda et non valeur : le temps écoulé avance à la seconde, et le lire
 *   ici cantonne la recomposition à cette ligne plutôt qu'à l'écran qui héberge la
 *   fenêtre. Ce compteur qui court dit à lui seul que le trajet est toujours en cours.
 */
@Composable
fun TripResumeDialog(
    trip: Trip,
    elapsed: () -> Duration?,
    onResume: () -> Unit,
    onAbandon: () -> Unit,
) {
    // Le jalon attendu : c'est ce qu'on vient chercher en rouvrant l'application.
    val next = trip.milestoneRows().firstOrNull { it.status == MilestoneStatus.CURRENT }?.label

    AlertDialog(
        onDismissRequest = onResume,
        title = { Text(stringResource(R.string.resume_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Direction dans son accent, comme la bannière de l'accueil la portait :
                // même langue visuelle, à un endroit près.
                Text(
                    text = stringResource(trip.direction.labelRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    LabelledValue(
                        labelRes = R.string.trip_departure,
                        value = trip.departureAt?.let { formatTime(it) },
                    )
                    LabelledValue(
                        labelRes = R.string.trip_elapsed,
                        value = elapsed()?.let(::formatDuration),
                    )
                }
                if (next != null) {
                    Text(
                        text = stringResource(R.string.resume_next_milestone, next),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onResume) {
                Text(stringResource(R.string.home_resume))
            }
        },
        dismissButton = {
            TextButton(onClick = onAbandon) {
                Text(
                    text = stringResource(R.string.abandon_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

/** Trajet d'aperçu : deux jalons pointés, le troisième attendu. */
@Suppress("MagicNumber") // Données d'aperçu : indices de jalons et durées en clair.
private fun previewTrip(): Trip {
    val departure = Instant.parse("2026-07-26T07:12:00Z")
    return Trip
        .start(id = "preview", direction = Direction.ALLER, departureAt = departure)
        .poseMilestone(1, departure.plusSeconds(560))
}

@Preview(name = "Reprise — nuit")
@Composable
private fun TripResumeDialogNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        TripResumeDialog(
            trip = previewTrip(),
            elapsed = { 12.minutes },
            onResume = {},
            onAbandon = {},
        )
    }
}

@Preview(name = "Reprise — jour")
@Composable
private fun TripResumeDialogDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        TripResumeDialog(
            trip = previewTrip(),
            elapsed = { 12.minutes },
            onResume = {},
            onAbandon = {},
        )
    }
}
