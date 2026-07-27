package fr.whitytoes.badgemoi.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.ui.components.LabelledValue
import fr.whitytoes.badgemoi.ui.components.MilestoneList
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold
import fr.whitytoes.badgemoi.ui.components.SegmentList
import fr.whitytoes.badgemoi.ui.formatTime
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import fr.whitytoes.badgemoi.ui.trip.milestoneRows
import java.time.Instant

private val ActionHeight = 56.dp

/**
 * Écran « Récapitulatif » (cahier des charges §3.3) : dernière relecture avant qu'un
 * trajet ne rejoigne l'archive.
 *
 * @param onNavigateHome appelé une fois le trajet archivé — ou s'il a disparu entre-temps,
 *   abandonné depuis l'accueil.
 * @param onNavigateBackToTrip retour à l'écran actif pour corriger un jalon. La liste des
 *   jalons est en lecture seule ici : la correction se fait là-bas, sur l'écran qui la
 *   porte déjà (§3.5).
 */
@Composable
fun SummaryScreen(
    onNavigateHome: () -> Unit,
    onNavigateBackToTrip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // `Loading` est volontairement exclu : y réagir renverrait à l'accueil avant même
    // d'avoir lu le trajet.
    LaunchedEffect(uiState) {
        if (uiState == SummaryUiState.NoTrip) onNavigateHome()
    }

    val ready = uiState as? SummaryUiState.Ready
    if (ready != null) {
        SummaryScreen(
            trip = ready.trip,
            archiving = ready.archiving,
            onArchive = viewModel::archiveTrip,
            onCancel = onNavigateBackToTrip,
            modifier = modifier,
        )
    }
}

/** Version sans état, prévisualisable. */
@Composable
internal fun SummaryScreen(
    trip: Trip,
    archiving: Boolean,
    onArchive: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = remember(trip) { trip.segmentRows() }
    val milestones = remember(trip) { trip.milestoneRows() }

    ScreenScaffold(
        modifier = modifier,
        top = { SummaryHeader(trip = trip) },
        bottom = {
            SummaryActionBar(archiving = archiving, onArchive = onArchive, onCancel = onCancel)
        },
    ) {
        // Le défilement appartient à cet écran et non aux deux listes : elles se suivent
        // dans une même colonne, et leur en donner un chacune produirait des défilements
        // imbriqués. C'est aussi pourquoi [SegmentList] n'est pas une liste paresseuse.
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            SegmentList(rows = segments)
            MilestoneList(rows = milestones)
        }
    }
}

/**
 * Bandeau Départ / Arrivée. Les deux heures sont connues à ce stade, contrairement à
 * l'écran actif qui ne peut afficher qu'un temps qui court (§3.3).
 */
@Composable
private fun SummaryHeader(trip: Trip) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        LabelledValue(
            labelRes = R.string.trip_departure,
            value = trip.departureAt?.let { formatTime(it) },
        )
        // Le dernier jalon a pu être ignoré : l'heure d'arrivée manque alors, et le champ
        // affiche son tiret plutôt que de laisser croire à une mesure.
        LabelledValue(
            labelRes = R.string.summary_arrival,
            value = trip.arrivalAt?.let { formatTime(it) },
        )
    }
}

@Composable
private fun SummaryActionBar(
    archiving: Boolean,
    onArchive: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = ActionHeight)) {
            Text(text = stringResource(R.string.summary_cancel))
        }
        Button(
            onClick = onArchive,
            // Le verrou du ViewModel empêche déjà la double insertion ; celui-ci le rend
            // visible, pour que l'appui suivant ne semble pas ignoré.
            enabled = !archiving,
            modifier = Modifier.weight(1f).heightIn(min = ActionHeight),
        ) {
            Text(
                text = stringResource(R.string.summary_save),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** Trajet d'aperçu comportant un jalon ignoré, donc deux tronçons non mesurables. */
@Suppress("MagicNumber") // Données d'aperçu : indices de jalons et durées en clair.
private fun previewTrip(): Trip {
    val departure = Instant.parse("2026-07-26T07:12:00Z")
    return Trip
        .start(id = "preview", direction = Direction.ALLER, departureAt = departure)
        .poseMilestone(1, departure.plusSeconds(560))
        .skipMilestone(2)
        .poseMilestone(3, departure.plusSeconds(1_920))
        .poseMilestone(4, departure.plusSeconds(2_400))
}

@Preview(name = "Récapitulatif — nuit", showBackground = true)
@Composable
private fun SummaryScreenNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        SummaryScreen(trip = previewTrip(), archiving = false, onArchive = {}, onCancel = {})
    }
}

@Preview(name = "Récapitulatif — jour", showBackground = true)
@Composable
private fun SummaryScreenDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        SummaryScreen(trip = previewTrip(), archiving = false, onArchive = {}, onCancel = {})
    }
}
