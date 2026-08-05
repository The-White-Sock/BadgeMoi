package fr.whitytoes.badgemoi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold
import fr.whitytoes.badgemoi.ui.labelRes
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme

/** Hauteur minimale des boutons de démarrage : cible tactile utilisable au pouce. */
private val ActionButtonHeight = 72.dp

/**
 * @param onNavigateToActiveTrip mène à l'écran des jalons. Le drapeau distingue une
 *   **reprise** — le trajet existait déjà au lancement — d'un démarrage, ce dont dépend
 *   l'ouverture de la fenêtre de reprise là-bas.
 */
@Composable
fun HomeScreen(
    onNavigateToActiveTrip: (resuming: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Un trajet en cours n'a pas besoin d'un écran pour être repris : l'accueil s'efface
    // devant lui. La décision reprendre/abandonner se prend dans une fenêtre posée sur
    // l'écran des jalons, où l'état du trajet est déjà affiché (§9, écart 10).
    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.TripInProgress) onNavigateToActiveTrip(true)
    }

    HomeScreen(
        uiState = uiState,
        // Démarrer puis naviguer : la persistance est asynchrone, mais l'écran de
        // trajet observe le même dépôt et affichera le trajet dès son écriture. Les
        // boutons de démarrage n'existent qu'à l'état Idle, il y aura donc bien un
        // trajet à l'arrivée.
        onStartTrip = { direction ->
            viewModel.startTrip(direction)
            onNavigateToActiveTrip(false)
        },
        modifier = modifier,
    )
}

/**
 * Écran d'accueil (cahier des charges §3.1), sans état pour rester prévisualisable.
 *
 * Il ne fait plus qu'une chose : **démarrer** un trajet. Le cas « trajet en cours » que
 * décrivait le §3.1 — bannière de statut, « Reprendre », « Abandonner » — n'affichait
 * qu'une ligne d'information pour un écran entier, et l'appelant redirige désormais vers
 * l'écran des jalons (§9, écart 10).
 *
 * Les actions vivent dans la zone fixe basse de [ScreenScaffold] : c'est la zone
 * atteignable au pouce, et le cahier exclut explicitement de les placer en haut.
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onStartTrip: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        modifier = modifier,
        bottom = {
            when (uiState) {
                HomeUiState.Idle -> StartTripActions(onStartTrip = onStartTrip)
                // Rien tant que l'existence d'un trajet en cours est inconnue : afficher
                // les boutons de démarrage puis basculer ferait clignoter l'écran au
                // lancement. Rien non plus avec un trajet en cours — on part d'ici.
                HomeUiState.Loading, is HomeUiState.TripInProgress -> Unit
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

@Preview(name = "Accueil — nuit", showBackground = true)
@Composable
private fun HomeScreenIdleNightPreview() {
    BadgeMoiTheme(darkTheme = true) {
        HomeScreen(uiState = HomeUiState.Idle, onStartTrip = {})
    }
}

@Preview(name = "Accueil — jour", showBackground = true)
@Composable
private fun HomeScreenIdleDayPreview() {
    BadgeMoiTheme(darkTheme = false) {
        HomeScreen(uiState = HomeUiState.Idle, onStartTrip = {})
    }
}
