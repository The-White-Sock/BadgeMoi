package fr.whitytoes.badgemoi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme

/**
 * Patron commun à tous les écrans (cahier des charges §1.6), pensé pour l'usage au
 * pouce en roulant :
 *
 * - [top] : zone fixe haute, informations de statut — jamais une liste ;
 * - [content] : **unique** zone scrollable, réservée aux listes de données ;
 * - [bottom] : zone fixe basse, boutons d'action — toujours visibles.
 *
 * La zone centrale porte le poids ([androidx.compose.foundation.layout.ColumnScope.weight]) :
 * c'est donc elle qui se réduit quand la place manque, jamais les zones fixes. Un
 * contenu volumineux ne peut ainsi pas repousser un bouton d'action hors de l'écran,
 * qui est la régression constatée côté web.
 *
 * Le scroll de la zone centrale appartient à l'appelant (`verticalScroll` ou
 * `LazyColumn`) : l'imposer ici interdirait les listes paresseuses et provoquerait
 * des scrolls imbriqués.
 *
 * Les encarts système sont appliqués ici (l'application est en edge-to-edge), sans
 * quoi les boutons de la zone basse passeraient sous la barre de navigation.
 *
 * Le comportement sur très petit écran — cas où les seules zones fixes dépassent la
 * hauteur disponible, par exemple à fort grossissement de police — est traité au
 * lot 7 (« tests sur petit écran », cahier §7).
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    top: @Composable (() -> Unit)? = null,
    bottom: @Composable (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (top != null) {
            Box(modifier = Modifier.fillMaxWidth()) { top() }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            content = content,
        )
        if (bottom != null) {
            Box(modifier = Modifier.fillMaxWidth()) { bottom() }
        }
    }
}

@Preview(name = "Nuit", showBackground = true)
@Composable
private fun ScreenScaffoldNightPreview() {
    BadgeMoiTheme(darkTheme = true) { ScreenScaffoldPreviewContent() }
}

@Preview(name = "Jour", showBackground = true)
@Composable
private fun ScreenScaffoldDayPreview() {
    BadgeMoiTheme(darkTheme = false) { ScreenScaffoldPreviewContent() }
}

@Composable
private fun ScreenScaffoldPreviewContent() {
    ScreenScaffold(
        top = { Text(text = "Zone fixe haute", color = MaterialTheme.colorScheme.onBackground) },
        bottom = { Text(text = "Zone fixe basse", color = MaterialTheme.colorScheme.onBackground) },
    ) {
        Text(text = "Zone scrollable", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
