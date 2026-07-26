package fr.whitytoes.badgemoi.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Impulsion courte de validation d'un jalon (POC). */
private const val VALIDATE_PULSE_MILLIS = 40L

private const val NO_INITIAL_DELAY = 0L
private const val SKIP_PULSE_MILLIS = 30L
private const val SKIP_GAP_MILLIS = 60L

/**
 * Triple impulsion de confirmation d'un « Passer » (POC). Le motif alterne attente et
 * vibration : délai initial nul, puis trois impulsions séparées par deux pauses.
 */
private val SKIP_PATTERN =
    longArrayOf(
        NO_INITIAL_DELAY,
        SKIP_PULSE_MILLIS,
        SKIP_GAP_MILLIS,
        SKIP_PULSE_MILLIS,
        SKIP_GAP_MILLIS,
        SKIP_PULSE_MILLIS,
    )

/** Pas de répétition : le motif est joué une fois. */
private const val NO_REPEAT = -1

/**
 * Retours haptiques de l'écran « Trajet actif » (cahier des charges §1.4, §4.3).
 *
 * Ce n'est pas un ornement : l'écran est consulté en roulant, et la vibration est
 * souvent la **seule confirmation perceptible** qu'un jalon a bien été pris en compte.
 * Les deux motifs sont volontairement distincts — une impulsion pour la validation
 * courante, trois pour le « Passer », plus rare et irréversible sans correction.
 *
 * Interface plutôt qu'appel direct au [Vibrator] : ce comportement n'est pas vérifiable
 * en test unitaire JVM, autant qu'il n'imprègne pas le reste du code.
 */
interface TripHaptics {
    fun milestoneValidated()

    fun milestoneSkipped()
}

/**
 * Implémentation liée à l'appareil courant, ou une implémentation inerte si aucun
 * vibreur n'est disponible — cas des aperçus Compose et de certains émulateurs.
 */
@Composable
fun rememberTripHaptics(): TripHaptics {
    val context = LocalContext.current
    return remember(context) {
        context.systemVibrator()?.let(::AndroidTripHaptics) ?: NoOpTripHaptics
    }
}

private class AndroidTripHaptics(
    private val vibrator: Vibrator,
) : TripHaptics {
    override fun milestoneValidated() {
        vibrator.vibrate(
            VibrationEffect.createOneShot(VALIDATE_PULSE_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE),
        )
    }

    override fun milestoneSkipped() {
        vibrator.vibrate(VibrationEffect.createWaveform(SKIP_PATTERN, NO_REPEAT))
    }
}

private object NoOpTripHaptics : TripHaptics {
    override fun milestoneValidated() = Unit

    override fun milestoneSkipped() = Unit
}

/** `VibratorManager` à partir d'Android 12, [Vibrator] historique en deçà. */
private fun Context.systemVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
