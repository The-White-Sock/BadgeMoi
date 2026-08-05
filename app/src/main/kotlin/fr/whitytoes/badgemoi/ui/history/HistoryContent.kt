package fr.whitytoes.badgemoi.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.DirectionStatistics
import fr.whitytoes.badgemoi.ui.components.LabelledValue
import fr.whitytoes.badgemoi.ui.components.RecentTripList
import fr.whitytoes.badgemoi.ui.components.SegmentList
import fr.whitytoes.badgemoi.ui.formatDuration

/**
 * Zone défilante de l'Historique : trajet complet, moyennes par tronçon, trajets récents.
 *
 * Le défilement appartient à cet écran et non aux deux listes : elles se suivent dans une
 * même colonne, et leur en donner un chacune produirait des défilements imbriqués.
 */
@Composable
fun HistoryContent(
    state: HistoryUiState.Ready,
    modifier: Modifier = Modifier,
) {
    val statistics = state.statistics

    // `map` est en ligne, donc l'appel composable au pluriel y est légal — ce qu'une
    // lambda passée à `segmentRows` ne permettrait pas.
    val sampleLabels =
        statistics.segmentAverages.map { average ->
            pluralStringResource(R.plurals.history_sample_count, average.sampleCount, average.sampleCount)
        }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TotalBlock(statistics = statistics)
        SectionTitle(titleRes = R.string.history_segment_averages)
        // Le composant du lot 4, sans duplication : il prend une liste de lignes et non
        // un trajet, précisément pour que des durées **moyennées** y entrent aussi bien
        // que des durées mesurées.
        SegmentList(rows = statistics.segmentRows(sampleLabels))
        SectionTitle(titleRes = R.string.history_recent_trips)
        RecentTripList(rows = state.recentTrips)
    }
}

/** Durée moyenne du trajet complet et volume de l'archive. */
@Composable
private fun TotalBlock(statistics: DirectionStatistics) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        LabelledValue(
            labelRes = R.string.history_total,
            value = statistics.totalAverage?.let(::formatDuration),
        )
        Text(
            text =
                pluralStringResource(
                    R.plurals.history_trip_count,
                    statistics.tripCount,
                    statistics.tripCount,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(
    @StringRes titleRes: Int,
) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * Archive vide — l'état du premier lancement, donc le premier écran que voit un nouvel
 * utilisateur. Il mérite une phrase, pas une page de tirets.
 */
@Composable
fun EmptyArchive(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.history_empty),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(32.dp),
    )
}
