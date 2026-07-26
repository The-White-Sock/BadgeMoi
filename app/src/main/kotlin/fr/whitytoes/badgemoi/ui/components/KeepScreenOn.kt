package fr.whitytoes.badgemoi.ui.components

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Maintient l'écran allumé tant que ce composable est présent (cahier des charges §4.4).
 *
 * Le cahier retient `FLAG_KEEP_SCREEN_ON` plutôt qu'un `PowerManager.WakeLock` explicite
 * précisément parce qu'il ne réclame aucune permission supplémentaire.
 *
 * Le retrait passe par un [DisposableEffect] : le drapeau tombe dès que l'écran quitte
 * la composition, quel que soit le chemin emprunté — arrivée au récapitulatif, abandon
 * depuis l'accueil, retour arrière système. Le poser et le retirer à la main aux endroits
 * « prévus » laisserait forcément passer un cas, et un écran resté allumé viderait la
 * batterie en silence.
 */
@Composable
fun KeepScreenOn() {
    val activity = LocalActivity.current

    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
