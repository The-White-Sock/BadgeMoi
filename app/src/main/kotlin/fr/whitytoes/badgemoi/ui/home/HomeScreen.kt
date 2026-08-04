package fr.whitytoes.badgemoi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold
import fr.whitytoes.badgemoi.ui.formatTime
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import fr.whitytoes.badgemoi.ui.theme.numericTextStyle
import java.time.Instant

/** Hauteur minimale des boutons de démarrage : cible tactile utilisable au pouce. */
private val ActionButtonHeight = 72.dp

@Composable
fun HomeScreen(
    onNavigateToActiveTrip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        // Démarrer puis naviguer : la persistance est asynchrone, mais l'écran de
        // trajet observe le même dépôt et affichera le trajet dès son écriture. Les
        // boutons de démarrage n'existent qu'à l'état Idle, il y aura donc bien un
        // trajet à l'arrivée.
        onStartTrip = { direction ->
            viewModel.startTrip(direction)
            onNavigateToActiveTrip()
        },
        onResumeTrip = onNavigateToActiveTrip,
        onAbandonTrip = viewModel::abandonTrip,
        modifier = modifier,
    )
}

/**
 * Écran d'accueil (cahier des charges §3.1), sans état pour rester prévisualisable.
 *
 * Les actions vivent dans la zone fixe basse de [ScreenScaffold] : c'est la zone
 * atteignable au pouce, et le cahier exclut explicitement de les placer en haut.
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onStartTrip: (Direction) -> Unit,
    onResumeTrip: () -> Unit,
    onAbandonTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip = (uiState as? HomeUiState.TripInProgress)?.trip
    val banner: @Composable (() -> Unit)? =
        if (trip != null) {
            { TripStatusBanner(trip = trip) }
        } else {
            null
        }

    ScreenScaffold(
        modifier = modifier,
        top = banner,
        bottom = {
            when (uiState) {
                // Rien tant que l'existence d'un trajet en cours est inconnue :
                // afficher les boutons de démarrage puis les remplacer par la
                // bannière de reprise ferait clignoter l'écran au lancement.
                HomeUiState.Loading -> Unit
                HomeUiState.Idle -> StartTripActions(onStartTrip = onStartTrip)
                is HomeUiState.TripInProgress ->
                    ResumeTripActions(
                        onResumeTrip = onResumeTrip,
                        onAbandonTrip = onAbandonTrip,
                    )
            }
        },
        content = {},
    )
}

@Composable
private fun StartTripActions(
    onStartTrip: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Direction.entries.forEach { direction ->
            StartTripButton(direction = direction, onClick = { onStartTrip(direction) })
        }
    }
}

@Composable
private fun StartTripButton(
    direction: Direction,
    onClick: () -> Unit,
) {
    // Aller en ambre, Retour en teal : les deux accents du POC, pour distinguer les
    // deux sens d'un coup d'œil. Les couleurs de contenu viennent du thème, jamais
    // d'un littéral (cahier §5).
    val colors =
        when (direction) {
            Direction.ALLER ->
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            Direction.RETOUR ->
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
        }
    val preview = remember(direction) { routePreview(direction) }

    Button(
        onClick = onClick,
        colors = colors,
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionButtonHeight),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(direction.labelRes()),
                style = MaterialTheme.typography.titleLarge,
            )
            // Aperçu textuel du parcours ; la frise d'icônes du cahier §3.1 le
            // remplacera avec le jeu vectoriel du lot 7.
            Text(text = preview, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResumeTripActions(
    onResumeTrip: () -> Unit,
    onAbandonTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingAbandon by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onResumeTrip,
            modifier = Modifier.fillMaxWidth().heightIn(min = ActionButtonHeight),
        ) {
            Text(
                text = stringResource(R.string.home_resume),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        TextButton(onClick = { confirmingAbandon = true }) {
            Text(
                text = stringResource(R.string.abandon_action),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (confirmingAbandon) {
        // L'abandon efface le trajet sans retour possible, et l'application s'utilise
        // en roulant : une confirmation explicite s'impose.
        AlertDialog(
            onDismissRequest = { confirmingAbandon = false },
            title = { Text(stringResource(R.string.abandon_dialog_title)) },
            text = { Text(stringResource(R.string.abandon_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingAbandon = false
                        onAbandonTrip()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.abandon_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingAbandon = false }) {
                    Text(stringResource(R.string.abandon_dialog_dismiss))
                }
            },
        )
    }
}

@Composable
private fun TripStatusBanner(
    trip: Trip,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(trip.direction.labelRes()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        val departure = trip.departureAt
        if (departure != null) {
            Text(
                text = stringResource(R.string.home_departure_at, formatTime(departure)),
                style = numericTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Sans trajet — nuit", showBackground = true)
@Composable
private fun HomeScreenIdleNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        HomeScreen(
            uiState = HomeUiState.Idle,
            onStartTrip = {},
            onResumeTrip = {},
            onAbandonTrip = {},
        )
    }
}

@Preview(name = "Sans trajet — jour", showBackground = true)
@Composable
private fun HomeScreenIdleDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        HomeScreen(
            uiState = HomeUiState.Idle,
            onStartTrip = {},
            onResumeTrip = {},
            onAbandonTrip = {},
        )
    }
}

@Preview(name = "Trajet en cours — nuit", showBackground = true)
@Composable
private fun HomeScreenInProgressNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        HomeScreen(
            uiState =
                HomeUiState.TripInProgress(
                    Trip.start(
                        id = "preview",
                        direction = Direction.RETOUR,
                        departureAt = Instant.parse("2026-07-26T07:12:00Z"),
                    ),
                ),
            onStartTrip = {},
            onResumeTrip = {},
            onAbandonTrip = {},
        )
    }
}
