package fr.whitytoes.badgemoi.ui.trip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.ui.components.KeepScreenOn
import fr.whitytoes.badgemoi.ui.components.MilestoneList
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold
import fr.whitytoes.badgemoi.ui.components.TripProgressFrieze
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import java.time.Instant

/**
 * Écran « Trajet actif » (cahier des charges §3.2).
 *
 * La frise de progression occupe la zone haute fixe — c'est une information de statut,
 * pas une liste — et la liste des jalons la zone scrollable. La barre d'action
 * Valider / Passer arrive avec l'issue dédiée du lot 3.
 */
@Composable
fun TripActiveScreen(
    onNavigateHome: () -> Unit,
    onNavigateToSummary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Le trajet a disparu (abandonné depuis l'accueil, ou jamais existé) : cet écran
    // n'a plus d'objet. `Loading` est volontairement exclu, sinon on repartirait à
    // l'accueil avant même d'avoir lu le DataStore.
    LaunchedEffect(uiState) {
        if (uiState == TripUiState.NoTrip) onNavigateHome()
    }

    val trip = (uiState as? TripUiState.Active)?.trip

    // Dernier jalon posé ou ignoré : place au récapitulatif (lot 4).
    LaunchedEffect(trip?.isComplete) {
        if (trip?.isComplete == true) onNavigateToSummary()
    }

    if (trip != null) {
        TripActiveScreen(
            trip = trip,
            onMilestoneClick = null,
            onValidate = viewModel::validateCurrentMilestone,
            onSkip = viewModel::skipCurrentMilestone,
            modifier = modifier,
        )
    }
}

/** Version sans état, prévisualisable. */
@Composable
internal fun TripActiveScreen(
    trip: Trip,
    onMilestoneClick: ((Int) -> Unit)?,
    onValidate: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Écran maintenu allumé pendant toute la saisie ; le drapeau tombe de lui-même
    // quand cet écran quitte la composition (cahier §4.4).
    KeepScreenOn()

    val rows = remember(trip) { trip.milestoneRows() }

    ScreenScaffold(
        modifier = modifier,
        top = { TripProgressFrieze(rows = rows) },
        bottom = { TripActionBar(onValidate = onValidate, onSkip = onSkip) },
    ) {
        MilestoneList(rows = rows, onMilestoneClick = onMilestoneClick)
    }
}

/** Durée du premier tronçon des aperçus : 9 min 20 s, une valeur plausible de trajet. */
private const val PREVIEW_FIRST_LEG_SECONDS = 560L

private fun previewTrip(): Trip {
    val departure = Instant.parse("2026-07-26T07:12:00Z")
    return Trip
        .start(id = "preview", direction = Direction.ALLER, departureAt = departure)
        .poseMilestone(1, departure.plusSeconds(PREVIEW_FIRST_LEG_SECONDS))
        .skipMilestone(2)
}

@Preview(name = "Trajet actif — nuit", showBackground = true)
@Composable
private fun TripActiveScreenNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        TripActiveScreen(trip = previewTrip(), onMilestoneClick = {}, onValidate = {}, onSkip = {})
    }
}

@Preview(name = "Trajet actif — jour", showBackground = true)
@Composable
private fun TripActiveScreenDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        TripActiveScreen(trip = previewTrip(), onMilestoneClick = {}, onValidate = {}, onSkip = {})
    }
}
