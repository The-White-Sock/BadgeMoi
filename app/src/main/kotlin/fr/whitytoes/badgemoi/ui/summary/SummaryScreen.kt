package fr.whitytoes.badgemoi.ui.summary

import androidx.annotation.StringRes
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
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.ui.components.AbandonConfirmationDialog
import fr.whitytoes.badgemoi.ui.components.LabelledValue
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold
import fr.whitytoes.badgemoi.ui.components.SegmentList
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.formatTime
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import fr.whitytoes.badgemoi.ui.trip.MilestoneCorrectionActions
import fr.whitytoes.badgemoi.ui.trip.MilestoneCorrectionSheet
import fr.whitytoes.badgemoi.ui.trip.correctionSeedInstant
import fr.whitytoes.badgemoi.ui.trip.isCorrectable
import fr.whitytoes.badgemoi.ui.trip.milestoneRows
import java.time.Instant

private val ActionHeight = 56.dp

/** Minimum Material pour une cible tactile (`docs/ergonomie.md` §4). */
private val TouchTargetHeight = 48.dp

/** Coin de la cellule « Départ » du bandeau, qui est cliquable. */
private val HeaderCellShape = RoundedCornerShape(12.dp)

/** Le jalon de départ : le seul que les tronçons ne peuvent pas ouvrir. */
private const val DEPARTURE_INDEX = 0

/** Le jalon d'arrivée, ouvert depuis le bandeau comme depuis le dernier tronçon. */
private val ARRIVAL_INDEX = Routes.MILESTONE_COUNT - 1

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
            state = ready,
            actions =
                SummaryActions(
                    onArchive = viewModel::archiveTrip,
                    onDiscard = viewModel::discardTrip,
                    onMilestoneClick = { index -> correctingIndex = index },
                    onDelete = viewModel::deleteArchivedTrip,
                    // Les corrections d'un trajet archivé sont déjà écrites : refermer,
                    // c'est simplement revenir d'où l'on vient.
                    onClose = onNavigateHome,
                ),
            correctingIndex = correctingIndex,
            modifier = modifier,
        )

        val index = correctingIndex
        if (index != null) {
            MilestoneCorrectionSheet(
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
    state: SummaryUiState.Ready,
    actions: SummaryActions,
    correctingIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    val trip = state.trip
    val archived = state.archived
    val segments = remember(trip) { trip.segmentRows() }

    // L'abandon détruit le trajet, et cet écran est atteint **automatiquement** en fin de
    // parcours : la confirmation n'est pas une politesse, c'est un garde-fou. L'état vit
    // ici parce que le bouton est dans le bandeau et la fenêtre par-dessus tout l'écran.
    var confirmingAbandon by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(
        modifier = modifier,
        top = {
            SummaryHeader(
                trip = trip,
                onMilestoneClick = actions.onMilestoneClick,
                correctingIndex = correctingIndex,
                onAbandon = { confirmingAbandon = true },
                archived = archived,
            )
        },
        bottom = {
            SummaryActionBar(
                archiving = state.archiving,
                archived = archived,
                onArchive = if (archived) actions.onClose else actions.onArchive,
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

    if (confirmingAbandon) {
        AbandonConfirmationDialog(
            onConfirm = {
                confirmingAbandon = false
                if (archived) actions.onDelete() else actions.onDiscard()
            },
            onDismiss = { confirmingAbandon = false },
            titleRes = if (archived) R.string.summary_delete_title else R.string.abandon_dialog_title,
            messageRes = if (archived) R.string.summary_delete_message else R.string.abandon_dialog_message,
            confirmRes = if (archived) R.string.summary_delete else R.string.abandon_action,
        )
    }
}

/**
 * Bandeau Départ / Arrivée / Trajet complet (§3.3).
 *
 * Les trois cellules disent la même chose sous deux régimes différents, et c'est ce que
 * le bandeau doit rendre évident : **départ et arrivée se corrigent**, la durée se
 * **déduit** des deux. La distinction passe par le poids et la couleur du chiffre
 * ([LabelledValue] `computed`), pas par la taille — l'alignement des lignes de base ne
 * doit pas en souffrir.
 *
 * L'arrivée est cliquable au même titre que le départ. C'est un doublon du dernier
 * tronçon, qui ouvre déjà ce jalon, et c'est assumé : on cherche l'heure d'arrivée là où
 * elle est écrite, pas dans la ligne qui la mesure.
 *
 * Ce que la troisième cellule remplace, c'est le `departArrivalFlap` du POC, où la cellule
 * de droite basculait sur la durée mesurée quand l'arrivée manquait. Cette bascule
 * palliait l'absence de recours : on ne pouvait ni voir ni réparer une arrivée ignorée.
 * L'arrivée étant désormais affichée **et** corrigible, un tiret y est une information
 * exacte et actionnable, et la durée peut rester ce qu'elle prétend être — départ à
 * arrivée, donc `null` tant que l'arrivée manque (§9, écart 14).
 *
 * « Abandonner » occupe sa propre ligne, tout en haut, à l'opposé du pouce. C'est la même
 * décision qu'avant — une action irréversible ne doit pas tomber sous la main
 * (`docs/ergonomie.md` §3) — appliquée à un bandeau qui porte maintenant trois valeurs :
 * les quatre éléments ne tenaient plus sur une ligne sans se serrer.
 */
@Composable
private fun SummaryHeader(
    trip: Trip,
    onMilestoneClick: ((Int) -> Unit)?,
    correctingIndex: Int?,
    onAbandon: () -> Unit,
    archived: Boolean,
) {
    val rows = remember(trip) { trip.milestoneRows() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onAbandon, modifier = Modifier.heightIn(min = TouchTargetHeight)) {
                Text(
                    // Un trajet archivé se **supprime** ; un trajet en cours s'abandonne, il
                    // n'a jamais été rangé nulle part.
                    text = stringResource(if (archived) R.string.summary_delete else R.string.abandon_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            CorrectableValue(
                labelRes = R.string.trip_departure,
                value = trip.departureAt?.let { formatTime(it) },
                correctable = rows[DEPARTURE_INDEX].status.isCorrectable,
                selected = correctingIndex == DEPARTURE_INDEX,
                onClick = onMilestoneClick?.let { { it(DEPARTURE_INDEX) } },
            )
            CorrectableValue(
                labelRes = R.string.summary_arrival,
                value = trip.arrivalAt?.let { formatTime(it) },
                correctable = rows[ARRIVAL_INDEX].status.isCorrectable,
                selected = correctingIndex == ARRIVAL_INDEX,
                onClick = onMilestoneClick?.let { { it(ARRIVAL_INDEX) } },
            )
            LabelledValue(
                labelRes = R.string.trip_total,
                value = trip.totalDuration?.let(::formatDuration),
                computed = true,
            )
        }
    }
}

/**
 * Cellule chiffrée du bandeau qui **ouvre la correction** de son jalon.
 *
 * [correctable] reprend la règle du §9 écart 6 : seul un jalon tranché — posé ou ignoré —
 * s'ouvre. Une cellule inerte vaut mieux qu'une fenêtre de correction posée sur un
 * horodatage qui n'existe pas.
 */
@Composable
private fun CorrectableValue(
    @StringRes labelRes: Int,
    value: String?,
    correctable: Boolean,
    selected: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme

    LabelledValue(
        labelRes = labelRes,
        value = value,
        modifier =
            Modifier
                .clip(HeaderCellShape)
                .background(if (selected) colors.secondaryContainer else Color.Transparent)
                .then(
                    if (onClick != null && correctable) {
                        Modifier.clickable(
                            interactionSource = null,
                            indication = ripple(color = colors.secondary),
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                    // Cible tactile : la cellule est cliquable, elle doit tenir les 48 dp
                    // comme n'importe quel bouton (`docs/ergonomie.md` §4).
                ).heightIn(min = TouchTargetHeight)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * Zone d'action basse : « Enregistrer », **seul** et pleine largeur.
 *
 * C'est la position robuste pour une action primaire : elle ne dépend pas de la main qui
 * tient l'appareil (`docs/ergonomie.md` §2). « Abandonner » lui disputait la moitié de la
 * barre ; il est remonté dans le bandeau.
 */
@Composable
private fun SummaryActionBar(
    archiving: Boolean,
    archived: Boolean,
    onArchive: () -> Unit,
) {
    Button(
        onClick = onArchive,
        // Le verrou du ViewModel empêche déjà la double insertion ; celui-ci le rend
        // visible, pour que l'appui suivant ne semble pas ignoré.
        enabled = !archiving,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = ActionHeight),
    ) {
        Text(
            // Rien à enregistrer sur un trajet archivé : ses corrections sont écrites au
            // fur et à mesure, le bouton ne fait que refermer.
            text = stringResource(if (archived) R.string.summary_done else R.string.summary_save),
            style = MaterialTheme.typography.titleLarge,
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
            state = SummaryUiState.Ready(trip = previewTrip()),
            actions = SummaryActions(onArchive = {}, onDiscard = {}),
        )
    }
}

/**
 * Le cas que la bascule du POC servait à masquer : arrivée ignorée, donc pas d'heure
 * d'arrivée et pas de durée totale. Deux tirets, et une cellule sur laquelle appuyer pour
 * y remédier (§9, écart 14).
 */
@Preview(name = "Récapitulatif — sans arrivée", showBackground = true)
@Composable
private fun SummaryScreenNoArrivalPreview() {
    BadgeMoiTheme(darkTheme = true) {
        SummaryScreen(
            state = SummaryUiState.Ready(trip = previewTrip().skipMilestone(ARRIVAL_INDEX)),
            actions = SummaryActions(onArchive = {}, onDiscard = {}, onMilestoneClick = {}),
        )
    }
}

@Preview(name = "Récapitulatif — jour", showBackground = true)
@Composable
private fun SummaryScreenDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        SummaryScreen(
            state = SummaryUiState.Ready(trip = previewTrip()),
            actions = SummaryActions(onArchive = {}, onDiscard = {}),
        )
    }
}
