package fr.whitytoes.badgemoi.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** Type MIME du document créé. Le sélecteur s'en sert pour filtrer les fournisseurs. */
private const val CSV_MIME_TYPE = "text/csv"

/**
 * Export de l'archive vers un fichier choisi par l'utilisateur (cahier §3.4).
 *
 * Passe par le **Storage Access Framework** plutôt que par une feuille de partage : aucune
 * modification du manifeste, aucun fichier temporaire à purger, et l'utilisateur choisit
 * l'emplacement **et** le nom. Le sélecteur propose Google Drive comme fournisseur quand
 * l'application est installée.
 *
 * Contrepartie assumée : l'e-mail n'est pas atteignable directement, il faut passer par le
 * gestionnaire de fichiers. Une feuille de partage (`ACTION_SEND` + `FileProvider`) reste
 * ajoutable ensuite sans rien réécrire — elle consommerait la même sérialisation. Ne pas
 * la déclarer par anticipation : ce serait un composant exporté de plus à justifier pour
 * la recette F-Droid.
 *
 * @param fileName nom proposé au sélecteur. Évalué **avant** l'ouverture, donc sur le fil
 *   principal : il ne dépend que de l'horloge, pas de l'archive.
 * @param content contenu du fichier. Suspendu et évalué **après** le choix de
 *   l'emplacement, dans le fil d'entrées-sorties : inutile de sérialiser une archive que
 *   l'utilisateur s'apprête peut-être à ne pas exporter.
 * @return lambda à brancher sur le bouton. Un appel ouvre le sélecteur ; l'annuler ne fait
 *   rien et ne signale rien, ce n'est pas une erreur.
 */
@Composable
fun rememberCsvExport(
    fileName: () -> String,
    content: suspend () -> String,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(CSV_MIME_TYPE),
        ) { uri ->
            // `null` = sélecteur annulé. L'utilisateur a changé d'avis, il n'y a rien à
            // lui dire.
            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch {
                withContext(Dispatchers.IO) {
                    // L'écriture peut échouer — stockage plein, fournisseur défaillant.
                    // L'échec est aujourd'hui **silencieux**, faute de tout véhicule de
                    // message dans l'application ; il reste préférable à une exception non
                    // rattrapée sur un fil d'entrées-sorties, qui terminerait le processus.
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(content().toByteArray(Charsets.UTF_8))
                        }
                    }.onFailure { if (it !is IOException) throw it }
                }
            }
        }

    return { launcher.launch(fileName()) }
}
