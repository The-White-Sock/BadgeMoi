package fr.whitytoes.badgemoi.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.ui.components.LabelledValue
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold
import fr.whitytoes.badgemoi.ui.components.SegmentList
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.formatTime
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import fr.whitytoes.badgemoi.ui.trip.MilestoneCorrectionActions
import fr.whitytoes.badgemoi.ui.trip.MilestoneCorrectionDialog
import fr.whitytoes.badgemoi.ui.trip.correctionSeedInstant
import fr.whitytoes.badgemoi.ui.trip.measuredDuration
import fr.whitytoes.badgemoi.ui.trip.milestoneRows
import java.time.Instant

private val ActionHeight = 56.dp

/** Coin de la cellule « Départ » du bandeau, qui est cliquable. */
private val HeaderCellShape = RoundedCornerShape(12.dp)

/** Le jalon de départ : le seul que les tronçons ne peuvent pas ouvrir. */
private const val DEPARTURE_INDEX = 0

/**
 * Écran « Récapitulatif » (cahier des charges §3.3) : dernière relecture avant qu'un
 * trajet ne rejoigne l'archive.
 *
 * La correction d'un jalon se fait **sur place**, comme dans le POC : on ne quitte jamais
 * cet écran pour corriger, l'écran actif étant de toute façon retiré de la pile en
 * arrivant ici. Elle passe par les **tronçons** — chacun ouvre le jalon qui le ferme — et
 * par la cellule « Départ » du bandeau pour le jalon 0. Les cinq jalons sont ainsi
 * atteignables, sans qu'un clic soit jamais ambigu.
 *
 * @param onNavigateHome appelé une fois le trajet archivé ou abandonné — ou s'il a disparu
 *   entre-temps, abandonné depuis l'accueil.
 */
@Composable
fun SummaryScreen(
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // `Loading` est volontairement exclu : y réagir renverrait à l'accueil avant même
    // d'avoir lu le trajet.
    LaunchedEffect(uiState) {
        if (uiState == SummaryUiState.NoTrip) onNavigateHome()
    }

    // Index du jalon en cours de correction, porté ici pour que la version sans état reste
    // purement descriptive — même découpage que sur l'écran actif.
    var correctingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val ready = uiState as? SummaryUiState.Ready
    if (ready != null) {
        val trip = ready.trip

        SummaryScreen(
            trip = trip,
            archiving = ready.archiving,
            actions =
                SummaryActions(
                    onArchive = viewModel::archiveTrip,
                    onDiscard = viewModel::discardTrip,
                    onMilestoneClick = { index -> correctingIndex = index },
                ),
            correctingIndex = correctingIndex,
            modifier = modifier,
        )

        val index = correctingIndex
        if (index != null) {
            MilestoneCorrectionDialog(
                label = trip.milestoneRows()[index].label,
                seedAt = trip.correctionSeedInstant(index),
                actions =
                    MilestoneCorrectionActions(
                        onSave = { hour, minute ->
                            viewModel.correctMilestone(index, hour, minute)
                            correctingIndex = null
                        },
                        onSkip = {
                            viewModel.skipMilestone(index)
                            correctingIndex = null
                        },
                        onClear = {
                            viewModel.clearMilestone(index)
                            correctingIndex = null
                        },
                        onDismiss = { correctingIndex = null },
                    ),
            )
        }
    }
}

/**
 * Version sans état, prévisualisable.
 *
 * L'écran ne montre que des **tronçons** : ce sont des durées, et c'est une durée qu'on
 * vient relire avant d'archiver. La liste des jalons de l'écran actif empruntait ses
 * valeurs à celle-ci, et affichait donc deux fois les mêmes nombres (§9, écart 9).
 */
@Composable
internal fun SummaryScreen(
    trip: Trip,
    archiving: Boolean,
    actions: SummaryActions,
    correctingIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    val segments = remember(trip) { trip.segmentRows() }

    ScreenScaffold(
        modifier = modifier,
        top = {
            SummaryHeader(
                trip = trip,
                onDepartureClick = actions.onMilestoneClick?.let { { it(DEPARTURE_INDEX) } },
                departureSelected = correctingIndex == DEPARTURE_INDEX,
            )
        },
        bottom = {
            SummaryActionBar(
                archiving = archiving,
                onArchive = actions.onArchive,
                onDiscard = actions.onDiscard,
            )
        },
    ) {
        // Le défilement appartient à cet écran et non à la liste : à grande taille de
        // police, quatre tronçons dépassent la zone. C'est aussi pourquoi [SegmentList]
        // n'est pas une liste paresseuse — elle y serait mesurée sous une hauteur
        // maximale infinie, ce que Compose refuse.
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            SegmentList(
                rows = segments,
                onSegmentClick =
                    actions.onMilestoneClick?.let { click ->
                        { position -> click(segments[position].toIndex) }
                    },
                // L'état de l'écran est un index de **jalon** : on retrouve le tronçon
                // qui s'y termine, s'il y en a un — le départ n'en a pas.
                selectedIndex = segments.indexOfFirst { it.toIndex == correctingIndex }.takeIf { it >= 0 },
            )
        }
    }
}

/**
 * Bandeau Départ / Arrivée (§3.3).
 *
 * La cellule de droite **bascule** : l'heure d'arrivée quand le dernier jalon est
 * horodaté, la durée mesurée sinon. C'est le `departArrivalFlap` du POC — un trajet dont
 * l'arrivée a été ignorée n'a pas d'heure d'arrivée, mais reste mesurable jusqu'à son
 * dernier pointage, et afficher deux tirets perdrait cette information.
 *
 * La cellule **Départ** est cliquable : elle est le seul accès au jalon 0, les tronçons
 * n'ouvrant que leur jalon d'arrivée. Sans elle, une heure de départ fausse ne serait plus
 * rattrapable une fois l'écran actif retiré de la pile.
 */
@Composable
private fun SummaryHeader(
    trip: Trip,
    onDepartureClick: (() -> Unit)?,
    departureSelected: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val departure = trip.departureAt

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        LabelledValue(
            labelRes = R.string.trip_departure,
            value = departure?.let { formatTime(it) },
            modifier =
                Modifier
                    .clip(HeaderCellShape)
                    .background(
                        if (departureSelected) colors.secondaryContainer else Color.Transparent,
                    ).then(
                        // Un départ absent n'a rien à corriger : la cellule reste inerte
                        // plutôt que d'ouvrir une fenêtre sur un horodatage inexistant.
                        if (onDepartureClick != null && departure != null) {
                            Modifier.clickable(
                                interactionSource = null,
                                indication = ripple(color = colors.secondary),
                                onClick = onDepartureClick,
                            )
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = 8.dp, vertical = 4.dp),
        )
        val arrival = trip.arrivalAt
        if (arrival != null) {
            LabelledValue(labelRes = R.string.summary_arrival, value = formatTime(arrival))
        } else {
            LabelledValue(
                labelRes = R.string.trip_elapsed,
                value = trip.measuredDuration()?.let(::formatDuration),
            )
        }
    }
}

@Composable
private fun SummaryActionBar(
    archiving: Boolean,
    onArchive: () -> Unit,
    onDiscard: () -> Unit,
) {
    // L'abandon détruit le trajet, et cet écran est atteint **automatiquement** en fin de
    // parcours : la confirmation n'est pas une politesse, c'est un garde-fou. Le POC s'en
    // passe ; l'accueil, lui, en demande déjà une pour la même action.
    var confirming by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { confirming = true },
            modifier = Modifier.heightIn(min = ActionHeight),
        ) {
            Text(
                text = stringResource(R.string.abandon_action),
                color = MaterialTheme.colorScheme.error,
            )
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

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.abandon_dialog_title)) },
            text = { Text(stringResource(R.string.abandon_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onDiscard()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.abandon_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.abandon_dialog_dismiss))
                }
            },
        )
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
        SummaryScreen(
            trip = previewTrip(),
            archiving = false,
            actions = SummaryActions(onArchive = {}, onDiscard = {}),
        )
    }
}

@Preview(name = "Récapitulatif — jour", showBackground = true)
@Composable
private fun SummaryScreenDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        SummaryScreen(
            trip = previewTrip(),
            archiving = false,
            actions = SummaryActions(onArchive = {}, onDiscard = {}),
        )
    }
}
