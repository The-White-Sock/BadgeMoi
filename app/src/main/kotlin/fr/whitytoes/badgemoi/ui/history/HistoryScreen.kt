package fr.whitytoes.badgemoi.ui.history

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.DirectionStatistics
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.SegmentAverage
import fr.whitytoes.badgemoi.domain.TripPace
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

private val ActionHeight = 56.dp

/**
 * Écran « Historique » (cahier des charges §3.4), second onglet de l'application.
 *
 * Il remplace le texte d'attente en place depuis le lot 2 et boucle le cycle : un trajet
 * archivé depuis le récapitulatif apparaît ici, et les moyennes en tiennent compte.
 *
 * Seul écran de son onglet : aucune navigation interne à prévoir.
 */
@Composable
fun HistoryScreen(
    onOpenTrip: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Le sélecteur de fichier vit dans la composition, pas dans le ViewModel : celui-ci
    // ne sait rien du Storage Access Framework et fournit seulement le nom et le contenu.
    val export =
        rememberCsvExport(
            fileName = viewModel::csvFileName,
            content = viewModel::csvContent,
        )

    // Rien tant que l'archive n'est pas lue : afficher « aucun trajet » puis basculer
    // ferait clignoter l'écran, et mentirait le temps d'une image.
    val ready = uiState as? HistoryUiState.Ready ?: return

    HistoryScreen(
        state = ready,
        actions =
            HistoryActions(
                onSelectDirection = viewModel::selectDirection,
                onExport = export,
                // Un seul geste sur la ligne, deux sens : cocher en mode sélection,
                // ouvrir le trajet sinon. Le ViewModel sait dans quel mode on est.
                onTripClick = { id ->
                    if (ready.selecting) viewModel.toggleTripSelection(id) else onOpenTrip(id)
                },
                onStartSelection = viewModel::startSelection,
                onCancelSelection = viewModel::cancelSelection,
                onSelectAll = viewModel::selectAllTrips,
                onDeleteSelected = viewModel::deleteSelectedTrips,
            ),
        modifier = modifier,
    )
}

/** Version sans état, prévisualisable. */
@Composable
internal fun HistoryScreen(
    state: HistoryUiState.Ready,
    actions: HistoryActions,
    modifier: Modifier = Modifier,
) {
    val statistics = state.statistics

    ScreenScaffold(
        modifier = modifier,
        top = {
            HistoryHeader(
                direction = statistics.direction,
                onSelectDirection = actions.onSelectDirection,
            )
        },
        // « Exporter » est seul en bas et pleine largeur : c'est l'action primaire de
        // l'écran, et cette position ne dépend pas de la main qui tient l'appareil.
        // « Effacer », qui détruit, est remonté dans le bandeau (`docs/ergonomie.md` §3).
        bottom = { ExportButton(onExport = actions.onExport) },
    ) {
        if (statistics.tripCount == 0) {
            EmptyArchive(modifier = Modifier.align(Alignment.Center))
        } else {
            HistoryContent(state = state, actions = actions)
        }
    }
}

@Composable
private fun ExportButton(onExport: () -> Unit) {
    Button(
        onClick = onExport,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = ActionHeight),
    ) {
        Text(
            text = stringResource(R.string.history_export),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

private val previewActions = HistoryActions()

/**
 * Statistiques d'aperçu comportant un tronçon **moins mesuré que les autres** — trois
 * mesures sur dix — et un trajet récent sans durée totale : les deux cas où l'écran doit
 * dire ce qu'il ne sait pas.
 */
@Suppress("MagicNumber") // Données d'aperçu : durées et effectifs en clair.
private fun previewState(): HistoryUiState.Ready {
    val route = Routes.forDirection(Direction.ALLER)
    val day = Instant.parse("2026-07-26T07:12:00Z")

    return HistoryUiState.Ready(
        statistics =
            DirectionStatistics(
                direction = Direction.ALLER,
                tripCount = 10,
                totalAverage = 28.minutes,
                segmentAverages =
                    listOf(
                        SegmentAverage(route.segments[0], 9.minutes, 10),
                        SegmentAverage(route.segments[1], 4.minutes, 3),
                        SegmentAverage(route.segments[2], 11.minutes, 10),
                        SegmentAverage(route.segments[3], 4.minutes, 10),
                    ),
            ),
        recentTrips =
            listOf(
                RecentTripRow("1", day, 24.minutes, TripPace.FASTER),
                RecentTripRow("2", day.minusSeconds(86_400), 31.minutes, TripPace.SLOWER),
                RecentTripRow("3", day.minusSeconds(172_800), null, null),
            ),
    )
}

private fun emptyState() =
    HistoryUiState.Ready(
        statistics =
            DirectionStatistics(
                direction = Direction.ALLER,
                tripCount = 0,
                totalAverage = null,
                segmentAverages = emptyList(),
            ),
        recentTrips = emptyList(),
    )

@Preview(name = "Historique — nuit", showBackground = true)
@Composable
private fun HistoryScreenNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        HistoryScreen(state = previewState(), actions = previewActions)
    }
}

@Preview(name = "Historique — jour", showBackground = true)
@Composable
private fun HistoryScreenDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        HistoryScreen(state = previewState(), actions = previewActions)
    }
}

@Preview(name = "Historique vide — nuit", showBackground = true)
@Composable
private fun HistoryScreenEmptyNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        HistoryScreen(state = emptyState(), actions = previewActions)
    }
}

@Preview(name = "Historique vide — jour", showBackground = true)
@Composable
private fun HistoryScreenEmptyDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        HistoryScreen(state = emptyState(), actions = previewActions)
    }
}
