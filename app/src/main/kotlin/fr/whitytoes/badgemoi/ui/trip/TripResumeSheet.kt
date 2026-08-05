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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.ui.components.LabelledValue
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.formatTime
import fr.whitytoes.badgemoi.ui.labelRes
import kotlin.time.Duration

private val PrimaryActionHeight = 56.dp

/** Minimum Material pour une cible tactile (`docs/ergonomie.md` §4). */
private val TouchTargetHeight = 48.dp

/**
 * Reprise d'un trajet déjà entamé (§9, écart 10).
 *
 * Elle remplace l'écran d'accueil de reprise, qui affichait deux lignes pour un écran
 * entier. Posée sur l'écran des jalons, elle a derrière elle la frise, les compteurs et
 * les jalons déjà pointés : l'information qui manquait est là, sans être dupliquée.
 *
 * **Feuille basse** plutôt que fenêtre centrée : « Reprendre » tombe ainsi dans la zone
 * naturelle du pouce (`docs/ergonomie.md` §3). « Abandonner » est relégué en tête de
 * feuille, là où il est le moins commode — c'est précisément ce qu'on veut d'une action
 * irréversible ouverte automatiquement au lancement.
 *
 * Écarter la feuille revient à **reprendre** : c'est l'issue de loin la plus fréquente, et
 * la seule qui ne détruise rien.
 *
 * @param elapsed lambda et non valeur : le temps écoulé avance à la seconde, et le lire
 *   ici cantonne la recomposition à cette ligne plutôt qu'à l'écran qui héberge la
 *   feuille. Ce compteur qui court dit à lui seul que le trajet est toujours en cours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripResumeSheet(
    trip: Trip,
    elapsed: () -> Duration?,
    onResume: () -> Unit,
    onAbandon: () -> Unit,
) {
    // Le jalon attendu : c'est ce qu'on vient chercher en rouvrant l'application.
    val next = trip.milestoneRows().firstOrNull { it.status == MilestoneStatus.CURRENT }?.label

    ModalBottomSheet(
        onDismissRequest = onResume,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader(onAbandon = onAbandon)
            TripRecall(trip = trip, elapsed = elapsed, next = next)
            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryActionHeight),
            ) {
                Text(
                    text = stringResource(R.string.home_resume),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

/** Titre et « Abandonner », en tête de feuille : le point le moins commode du geste. */
@Composable
private fun SheetHeader(onAbandon: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.resume_dialog_title),
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onAbandon, modifier = Modifier.heightIn(min = TouchTargetHeight)) {
            Text(
                text = stringResource(R.string.abandon_action),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Où en est le trajet : direction, départ, temps écoulé, jalon attendu. */
@Composable
private fun TripRecall(
    trip: Trip,
    elapsed: () -> Duration?,
    next: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Direction dans son accent, comme la bannière de l'accueil la portait : même
        // langue visuelle, à un endroit près.
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
}
