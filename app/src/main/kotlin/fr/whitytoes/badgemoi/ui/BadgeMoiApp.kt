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
import androidx.navigation.toRoute
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

/**
 * Destination « Trajet actif » : saisie des jalons (lot 3). Onglet « Trajet ».
 *
 * @property resuming vrai quand on **retrouve** un trajet déjà entamé, faux quand on
 *   vient de le démarrer. C'est ce qui décide de la fenêtre de reprise (§9, écart 10) :
 *   celui qui vient d'appuyer sur « Aller » sait où il en est, on ne le lui demande pas.
 */
@Serializable
internal data class ActiveTripRoute(
    val resuming: Boolean = false,
)

/** Destination « Récapitulatif » : relecture avant archivage (lot 4). Onglet « Trajet ». */
@Serializable
internal data object SummaryRoute

/** Destination « Historique » : moyennes et trajets récents (lot 5). */
@Serializable
internal data object HistoryRoute

/**
 * Destination « Trajet archivé » : un trajet de l'historique rouvert dans le récapitulatif.
 *
 * Appartient à l'onglet **Historique** — c'est de là qu'on y entre et c'est là qu'on
 * revient — d'où une route distincte de [SummaryRoute] plutôt qu'un argument ajouté à
 * celle-ci : le bandeau haut doit continuer d'afficher « Historique » comme onglet actif.
 *
 * Le nom de la propriété est la clé lue par le `SavedStateHandle` du ViewModel
 * ([fr.whitytoes.badgemoi.ui.summary.ARCHIVED_TRIP_ID_KEY]).
 */
@Serializable
internal data class ArchivedTripRoute(
    val tripId: String,
)

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
        backStackEntry?.destination?.hierarchy?.none {
            it.hasRoute(HistoryRoute::class) || it.hasRoute(ArchivedTripRoute::class)
        } != false

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
        BadgeMoiNavHost(navController = navController, modifier = Modifier.weight(1f))
    }
}

/** Graphe de navigation, extrait pour garder [BadgeMoiApp] lisible. */
@Composable
private fun BadgeMoiNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TripRoute,
        modifier = modifier,
    ) {
        composable<TripRoute> {
            HomeScreen(
                onNavigateToActiveTrip = { resuming ->
                    navController.replaceAll(ActiveTripRoute(resuming = resuming))
                },
            )
        }
        composable<ActiveTripRoute> { entry ->
            TripActiveScreen(
                resuming = entry.toRoute<ActiveTripRoute>().resuming,
                onNavigateHome = { navController.replaceAll(TripRoute) },
                // L'écran actif quitte la pile : on ne corrige plus depuis là-bas une
                // fois au récapitulatif, les tronçons y étant cliquables.
                onNavigateToSummary = { navController.replaceAll(SummaryRoute) },
            )
        }
        composable<SummaryRoute> {
            SummaryScreen(onNavigateHome = { navController.replaceAll(TripRoute) })
        }
        composable<HistoryRoute> {
            // Navigation ordinaire, sans vider la pile : le retour depuis un trajet
            // archivé doit ramener à l'historique, pas quitter l'application.
            HistoryScreen(onOpenTrip = { id -> navController.navigate(ArchivedTripRoute(id)) })
        }
        composable<ArchivedTripRoute> {
            SummaryScreen(onNavigateHome = { navController.popBackStack() })
        }
    }
}

/**
 * Remplace **toute** la pile par [route].
 *
 * L'onglet « Trajet » est une succession d'états d'un même parcours — accueil, saisie,
 * récapitulatif — et non une pile où l'on remonte : revenir en arrière depuis la saisie
 * ou le récapitulatif doit quitter l'application.
 *
 * On vide le graphe entier plutôt que de dépiler jusqu'à une destination nommée. Un
 * `popUpTo(destination)` ne dépile que jusqu'à la **première** correspondance : qu'une
 * seconde entrée de la même destination traîne dessous, et le retour y atterrit, où un
 * `LaunchedEffect` renavigue aussitôt vers l'avant. Le retour semble alors ne rien faire,
 * et il faut autant d'appuis que d'entrées pour sortir — c'est le défaut constaté à
 * l'usage. `launchSingleTop` interdit en plus le doublon à la source.
 */
private fun NavHostController.replaceAll(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
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
