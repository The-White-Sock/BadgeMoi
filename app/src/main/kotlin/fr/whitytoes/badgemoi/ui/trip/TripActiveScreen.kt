package fr.whitytoes.badgemoi.ui.trip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.ui.components.AbandonConfirmationDialog
import fr.whitytoes.badgemoi.ui.components.KeepScreenOn
import fr.whitytoes.badgemoi.ui.components.MilestoneList
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold
import fr.whitytoes.badgemoi.ui.components.TripProgressFrieze
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import java.time.Instant
import kotlin.time.Duration

/**
 * Écran « Trajet actif » (cahier des charges §3.2).
 *
 * La frise de progression occupe la zone haute fixe — c'est une information de statut,
 * pas une liste — la liste des jalons la zone scrollable, et la barre Valider / Passer
 * la zone basse fixe.
 */
@Composable
fun TripActiveScreen(
    resuming: Boolean,
    onNavigateHome: () -> Unit,
    onNavigateToSummary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timers = viewModel.timers.collectAsStateWithLifecycle()

    // Le trajet a disparu (abandonné depuis l'accueil, ou jamais existé) : cet écran
    // n'a plus d'objet. `Loading` est volontairement exclu, sinon on repartirait à
    // l'accueil avant même d'avoir lu le DataStore.
    LaunchedEffect(uiState) {
        if (uiState == TripUiState.NoTrip) onNavigateHome()
    }

    val trip = (uiState as? TripUiState.Active)?.trip

    // Dernier jalon posé ou ignoré : place au récapitulatif (§3.3), qui retire cet écran
    // de la pile. La correction s'y fait sur place, on n'en revient donc pas ici — d'où
    // l'absence de garde-fou contre un aller-retour.
    LaunchedEffect(trip?.isComplete) {
        if (trip?.isComplete == true) onNavigateToSummary()
    }

    // Index du jalon en cours de correction. Porté ici plutôt que dans la version sans
    // état, qui reste ainsi purement descriptive et prévisualisable.
    var correctingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    if (trip != null) {
        TripActiveScreen(
            trip = trip,
            actions =
                TripActions(
                    onValidate = viewModel::validateCurrentMilestone,
                    onSkip = viewModel::skipCurrentMilestone,
                    onMilestoneClick = { index -> correctingIndex = index },
                ),
            // Lambda plutôt que valeur : la lecture de l'état est différée au bandeau,
            // seul à se recomposer à chaque seconde.
            timers = timers::value,
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

        TripResumeOverlay(
            trip = trip,
            resuming = resuming,
            // Lecture différée, comme pour le bandeau : seule la ligne du compteur se
            // recompose à la seconde, pas l'écran qui héberge la feuille.
            elapsed = { timers.value.elapsed },
            onAbandon = viewModel::abandonTrip,
        )
    }
}

/**
 * Décision de reprise d'un trajet retrouvé (§9, écart 10), feuille et confirmation
 * comprises.
 *
 * Composant à part parce que la décision est un état à elle seule, distinct de la saisie
 * des jalons : elle se prend une fois, et l'écran continue sans elle. `rememberSaveable`
 * pour qu'une feuille écartée ne revienne pas après une rotation ou une recréation du
 * processus.
 *
 * La confirmation d'abandon reste une **fenêtre centrée** là où la reprise est une feuille
 * basse : elle doit interrompre, pas se glisser sous le pouce (`docs/ergonomie.md` §3).
 */
@Composable
private fun TripResumeOverlay(
    trip: Trip,
    resuming: Boolean,
    elapsed: () -> Duration?,
    onAbandon: () -> Unit,
) {
    var pending by rememberSaveable { mutableStateOf(resuming) }
    var confirming by rememberSaveable { mutableStateOf(false) }

    // Rien à demander sur un trajet **terminé** : il part au récapitulatif dès la
    // composition suivante, la fenêtre n'aurait fait que clignoter au passage.
    if (!pending || trip.isComplete) return

    if (confirming) {
        AbandonConfirmationDialog(
            onConfirm = {
                confirming = false
                pending = false
                onAbandon()
            },
            onDismiss = { confirming = false },
        )
    } else {
        TripResumeSheet(
            trip = trip,
            elapsed = elapsed,
            onResume = { pending = false },
            onAbandon = { confirming = true },
        )
    }
}

/**
 * Version sans état, prévisualisable.
 *
 * [correctingIndex] n'est pas de la logique mais de l'affichage : la ligne dont la feuille
 * de correction est ouverte reste allumée, ce qui rattache la feuille au jalon qu'elle
 * modifie.
 */
@Composable
internal fun TripActiveScreen(
    trip: Trip,
    actions: TripActions,
    timers: () -> TripTimers,
    correctingIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    // Écran maintenu allumé pendant toute la saisie ; le drapeau tombe de lui-même
    // quand cet écran quitte la composition (cahier §4.4).
    KeepScreenOn()

    val rows = remember(trip) { trip.milestoneRows() }

    ScreenScaffold(
        modifier = modifier,
        top = {
            Column {
                TripTimerBanner(departureAt = trip.departureAt, timers = timers)
                TripProgressFrieze(rows = rows)
            }
        },
        bottom = { TripActionBar(onValidate = actions.onValidate, onSkip = actions.onSkip) },
    ) {
        MilestoneList(
            rows = rows,
            // Ancrée en **bas** de la zone : c'est là qu'est le pouce, et la ligne
            // courante est celle sur laquelle on agit (`docs/ergonomie.md` §2). Alignée
            // dans un `Box`, la liste garde la hauteur de son contenu tant qu'il tient,
            // et se met à défiler seulement quand il déborde — à grande taille de police.
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .verticalScroll(rememberScrollState()),
            onMilestoneClick = actions.onMilestoneClick,
            // Lecture différée elle aussi : seule la ligne du jalon courant se recompose
            // à la seconde, pas la liste entière.
            runningSince = { timers().sinceLastMilestone },
            selectedIndex = correctingIndex,
        )
    }
}

/** Durée du premier tronçon des aperçus : 9 min 20 s, une valeur plausible de trajet. */
private const val PREVIEW_FIRST_LEG_SECONDS = 560L

private val previewDeparture: Instant = Instant.parse("2026-07-26T07:12:00Z")

private fun previewStart(): Trip =
    Trip.start(id = "preview", direction = Direction.ALLER, departureAt = previewDeparture)

/** Milieu de trajet, avec un jalon ignoré : deux tranchés, un courant, un à venir. */
private fun previewTrip(): Trip =
    previewStart()
        .poseMilestone(1, previewDeparture.plusSeconds(PREVIEW_FIRST_LEG_SECONDS))
        .skipMilestone(2)

/** Dernier jalon : plus rien à venir sous la ligne courante. */
@Suppress("MagicNumber") // Données d'aperçu : durées en clair.
private fun previewLastMilestone(): Trip =
    previewTrip()
        .poseMilestone(3, previewDeparture.plusSeconds(1_920))

@Preview(name = "Trajet actif — nuit", showBackground = true)
@Composable
private fun TripActiveScreenNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        TripActiveScreen(
            trip = previewTrip(),
            actions = TripActions(onValidate = {}, onSkip = {}, onMilestoneClick = {}),
            timers = { TripTimers.EMPTY },
        )
    }
}

@Preview(name = "Trajet actif — jour", showBackground = true)
@Composable
private fun TripActiveScreenDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        TripActiveScreen(
            trip = previewTrip(),
            actions = TripActions(onValidate = {}, onSkip = {}, onMilestoneClick = {}),
            timers = { TripTimers.EMPTY },
        )
    }
}

/** Début de trajet : un seul jalon tranché, le départ. */
@Preview(name = "Trajet actif — départ", showBackground = true)
@Composable
private fun TripActiveScreenStartPreview() {
    BadgeMoiTheme(darkTheme = true) {
        TripActiveScreen(
            trip = previewStart(),
            actions = TripActions(onValidate = {}, onSkip = {}, onMilestoneClick = {}),
            timers = { TripTimers.EMPTY },
        )
    }
}

@Preview(name = "Trajet actif — dernier jalon", showBackground = true)
@Composable
private fun TripActiveScreenLastPreview() {
    BadgeMoiTheme(darkTheme = true) {
        TripActiveScreen(
            trip = previewLastMilestone(),
            actions = TripActions(onValidate = {}, onSkip = {}, onMilestoneClick = {}),
            timers = { TripTimers.EMPTY },
        )
    }
}
