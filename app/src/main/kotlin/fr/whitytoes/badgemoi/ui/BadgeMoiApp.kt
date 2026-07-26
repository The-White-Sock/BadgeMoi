package fr.whitytoes.badgemoi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.whitytoes.badgemoi.ui.components.AppTopBar
import fr.whitytoes.badgemoi.ui.components.TopLevelDestination
import fr.whitytoes.badgemoi.ui.history.HistoryScreen
import fr.whitytoes.badgemoi.ui.home.HomeScreen
import fr.whitytoes.badgemoi.ui.summary.SummaryScreen
import fr.whitytoes.badgemoi.ui.trip.TripActiveScreen
import kotlinx.serialization.Serializable

/** Destination « Trajet » : accueil, puis trajet actif (lot 3). */
@Serializable
internal data object TripRoute

/** Destination « Trajet actif » : saisie des jalons (lot 3). Onglet « Trajet ». */
@Serializable
internal data object ActiveTripRoute

/** Destination « Récapitulatif » : relecture avant archivage (lot 4). Onglet « Trajet ». */
@Serializable
internal data object SummaryRoute

/** Destination « Historique » : moyennes et trajets récents (lot 5). */
@Serializable
internal data object HistoryRoute

/**
 * Coquille de l'application : le bandeau haut partagé (cahier §3.1) surmonte le graphe
 * de navigation. Chaque destination applique ensuite son propre patron fixe/scroll/fixe
 * via [fr.whitytoes.badgemoi.ui.components.ScreenScaffold] — la zone basse d'actions est
 * propre à chaque écran, elle ne peut donc pas vivre ici.
 *
 * Les encarts système sont consommés à ce niveau : les scaffolds imbriqués n'y touchent
 * plus, et la zone d'action basse de chaque écran reste au-dessus de la barre système.
 *
 * Composant sans état : le thème courant et sa bascule sont fournis par l'appelant.
 */
@Composable
fun BadgeMoiApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // L'onglet « Trajet » couvre l'accueil, le trajet actif et le récapitulatif : on
    // se repère sur l'Historique, seule destination de l'autre onglet.
    val isTripSelected =
        backStackEntry?.destination?.hierarchy?.none { it.hasRoute(HistoryRoute::class) } != false

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AppTopBar(
            selected = if (isTripSelected) TopLevelDestination.TRIP else TopLevelDestination.HISTORY,
            isDarkTheme = isDarkTheme,
            onSelectDestination = { destination ->
                navController.switchTopLevelDestination(
                    when (destination) {
                        TopLevelDestination.TRIP -> TripRoute
                        TopLevelDestination.HISTORY -> HistoryRoute
                    },
                )
            },
            onToggleTheme = onToggleTheme,
        )
        NavHost(
            navController = navController,
            startDestination = TripRoute,
            modifier = Modifier.weight(1f),
        ) {
            composable<TripRoute> {
                HomeScreen(onNavigateToActiveTrip = { navController.navigate(ActiveTripRoute) })
            }
            composable<ActiveTripRoute> {
                TripActiveScreen(
                    // `popUpTo` plutôt qu'un simple navigate : sans cela, la pile
                    // accumulerait un écran de trajet mort à chaque aller-retour.
                    onNavigateHome = {
                        navController.navigate(TripRoute) {
                            popUpTo(TripRoute) { inclusive = true }
                        }
                    },
                    onNavigateToSummary = {
                        navController.navigate(SummaryRoute) {
                            popUpTo(ActiveTripRoute) { inclusive = true }
                        }
                    },
                )
            }
            composable<SummaryRoute> { SummaryScreen() }
            composable<HistoryRoute> { HistoryScreen() }
        }
    }
}

/**
 * Bascule d'onglet : conserve et restaure l'état de chaque destination, et évite
 * d'empiler des copies de la même destination à chaque aller-retour entre onglets.
 */
private fun NavHostController.switchTopLevelDestination(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
