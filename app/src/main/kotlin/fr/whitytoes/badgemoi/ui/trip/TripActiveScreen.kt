package fr.whitytoes.badgemoi.ui.trip

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.components.ScreenScaffold

/**
 * Écran « Trajet actif » (cahier des charges §3.2).
 *
 * Cette version pose la plomberie : état, persistance et sorties de l'écran. La frise
 * de progression et la liste des jalons arrivent avec l'issue dédiée du lot 3, la barre
 * d'action Valider / Passer avec la sienne.
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

    ScreenScaffold(modifier = modifier) {
        if (trip != null) {
            Text(
                text = stringResource(R.string.trip_active_coming_soon),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            )
        }
    }
}
