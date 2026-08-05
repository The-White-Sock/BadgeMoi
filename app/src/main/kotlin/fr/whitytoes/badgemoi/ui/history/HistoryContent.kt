package fr.whitytoes.badgemoi.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * Parts de hauteur des deux listes. Les trajets récents en montrent dix, les tronçons
 * quatre : la répartition suit ce rapport plutôt que de couper la hauteur en deux.
 */
private const val SEGMENTS_WEIGHT = 1f
private const val RECENT_TRIPS_WEIGHT = 2f

/**
 * Zone centrale de l'Historique : trajet complet, moyennes par tronçon, trajets récents.
 *
 * Les trois blocs défilent **indépendamment** (§9, écart 12). Le §1.6 impose une zone
 * défilante unique ; la règle visait la saisie en roulant, où un contenu qui grandit ne
 * doit pas repousser un bouton d'action hors de l'écran. L'Historique se consulte à
 * l'arrêt, et son enjeu est inverse : garder les moyennes sous les yeux **pendant** qu'on
 * parcourt les trajets qu'elles résument. Un seul défilement les chassait de l'écran.
 *
 * Chaque liste prend au plus sa part (`fill = false`) : un bloc plus court que la sienne
 * garde la hauteur de son contenu au lieu de laisser du vide sous lui, et ne défile que
 * lorsqu'il déborde.
 */
@Composable
fun HistoryContent(
    state: HistoryUiState.Ready,
    modifier: Modifier = Modifier,
    actions: HistoryActions = HistoryActions(),
) {
    val statistics = state.statistics

    // `map` est en ligne, donc l'appel composable au pluriel y est légal — ce qu'une
    // lambda passée à `segmentRows` ne permettrait pas.
    val sampleLabels =
        statistics.segmentAverages.map { average ->
            pluralStringResource(R.plurals.history_sample_count, average.sampleCount, average.sampleCount)
        }

    Column(modifier = modifier.fillMaxWidth()) {
        TotalBlock(statistics = statistics)
        SectionTitle(titleRes = R.string.history_segment_averages)
        ScrollingBlock(weight = SEGMENTS_WEIGHT) {
            // Le composant du lot 4, sans duplication : il prend une liste de lignes et
            // non un trajet, précisément pour que des durées **moyennées** y entrent
            // aussi bien que des durées mesurées.
            SegmentList(rows = statistics.segmentRows(sampleLabels))
        }
        RecentTripsBar(selectedIds = state.selectedIds, actions = actions)
        ScrollingBlock(weight = RECENT_TRIPS_WEIGHT) {
            RecentTripList(
                rows = state.recentTrips,
                onTripClick = actions.onTripClick,
                selectedIds = state.selectedIds,
            )
        }
    }
}

/**
 * Bloc défilant occupant **au plus** [weight] parts de la hauteur restante.
 *
 * `fill = false` est le point : sans lui, un bloc court étirerait sa part et laisserait du
 * vide, poussant le suivant vers le bas.
 */
@Composable
private fun ColumnScope.ScrollingBlock(
    weight: Float,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(weight, fill = false)
                .verticalScroll(rememberScrollState()),
    ) {
        content()
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
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * Titre des trajets récents, et les actions de suppression.
 *
 * Elles sont ici et non dans le bandeau haut parce que c'est **sur cette liste** qu'elles
 * agissent. Hors sélection, un seul bouton l'ouvre ; dedans, il se dédouble en « Annuler »
 * et « Supprimer », et le titre cède la place au décompte — l'écran doit dire ce qui est
 * coché, y compris ce qui ne tient pas dans les dix lignes affichées.
 */
@Composable
private fun RecentTripsBar(
    selectedIds: Set<String>?,
    actions: HistoryActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectedIds == null) {
            SectionTitle(titleRes = R.string.history_recent_trips, modifier = Modifier.weight(1f))
            TextButton(
                onClick = actions.onStartSelection,
                modifier = Modifier.heightIn(min = TouchTargetHeight),
            ) {
                Text(
                    text = stringResource(R.string.history_select),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.history_selected_count, selectedIds.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            TextButton(
                onClick = actions.onSelectAll,
                modifier = Modifier.heightIn(min = TouchTargetHeight),
            ) {
                Text(stringResource(R.string.history_select_all))
            }
            TextButton(
                onClick = actions.onCancelSelection,
                modifier = Modifier.heightIn(min = TouchTargetHeight),
            ) {
                Text(stringResource(R.string.history_select_cancel))
            }
            TextButton(
                onClick = actions.onDeleteSelected,
                // Rien de coché, rien à détruire : le bouton ne doit pas répondre.
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.heightIn(min = TouchTargetHeight),
            ) {
                Text(
                    text = stringResource(R.string.history_select),
                    color =
                        if (selectedIds.isEmpty()) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }
        }
    }
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
