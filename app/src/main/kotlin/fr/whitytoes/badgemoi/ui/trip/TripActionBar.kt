package fr.whitytoes.badgemoi.ui.trip

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.components.AppButton
import fr.whitytoes.badgemoi.ui.components.TapGuard
import fr.whitytoes.badgemoi.ui.components.rememberTripHaptics
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Fenêtre du verrou anti-double-tap sur « Valider » (POC). */
private val ValidateLockWindow = 400.milliseconds

/** Durée d'appui maintenu requise pour « Passer » (POC). */
private const val SKIP_HOLD_MILLIS = 650

/** Durée du flash de confirmation après une validation acceptée. */
private const val FLASH_MILLIS = 160L

private val ActionHeight = 72.dp
private val ActionShape = RoundedCornerShape(16.dp)

/**
 * Barre d'action de l'écran « Trajet actif » (cahier des charges §3.2), à placer dans la
 * zone fixe basse : elle doit rester atteignable au pouce quel que soit le défilement.
 *
 * Les deux gestes sont volontairement asymétriques, et ce n'est pas cosmétique :
 * « Valider » est l'action courante, donc un simple appui ; « Passer » saute un jalon
 * sans horodatage, donc un appui **maintenu** de [SKIP_HOLD_MILLIS] ms. Le cahier §1.4
 * en donne la raison — éviter les activations accidentelles dues aux vibrations du
 * board. Le remplacer par un appui simple réintroduirait le bug qu'il corrige.
 */
@Composable
fun TripActionBar(
    onValidate: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Le retour haptique est déclenché ici, au plus près du geste : c'est souvent la
    // seule confirmation perceptible en roulant (cahier §1.4).
    val haptics = rememberTripHaptics()

    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ValidateButton(
            onValidate = {
                haptics.milestoneValidated()
                onValidate()
            },
        )
        SkipButton(
            onSkip = {
                haptics.milestoneSkipped()
                onSkip()
            },
        )
    }
}

@Composable
private fun RowScope.ValidateButton(onValidate: () -> Unit) {
    val guard = remember { TapGuard(ValidateLockWindow) }
    var flashing by remember { mutableStateOf(false) }

    // Le flash confirme visuellement l'appui *accepté* : sur un board qui vibre,
    // l'utilisateur ne peut pas toujours vérifier la liste du regard.
    LaunchedEffect(flashing) {
        if (flashing) {
            delay(FLASH_MILLIS)
            flashing = false
        }
    }

    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (flashing) colors.onPrimary else colors.primary,
        label = "validate-flash",
    )
    val content by animateColorAsState(
        targetValue = if (flashing) colors.primary else colors.onPrimary,
        label = "validate-flash-content",
    )

    AppButton(
        onClick = {
            // Le rejet est silencieux : signaler « trop rapide » ferait douter d'un
            // appui qui, lui, a bien été pris en compte.
            if (guard.accept(System.currentTimeMillis())) {
                flashing = true
                onValidate()
            }
        },
        shape = ActionShape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        modifier = Modifier.weight(1f).height(ActionHeight).testTag("trip-validate"),
    ) {
        Text(
            text = stringResource(R.string.trip_validate),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun RowScope.SkipButton(onSkip: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var pressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(pressed) {
        if (pressed) {
            progress.animateTo(1f, tween(durationMillis = SKIP_HOLD_MILLIS, easing = LinearEasing))
            // On n'arrive ici que si l'animation est allée à son terme : un relâchement
            // anticipé change `pressed`, ce qui annule cet effet avant cette ligne.
            onSkip()
            pressed = false
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(ActionHeight)
                .clip(ActionShape)
                .background(colors.surfaceVariant)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                    )
                }.testTag("trip-skip"),
        contentAlignment = Alignment.Center,
    ) {
        // Jauge de remplissage : elle rend l'appui maintenu compréhensible, sans quoi
        // l'utilisateur relâcherait avant le seuil en croyant le bouton inerte.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .background(colors.secondary),
        )
        Text(
            text = stringResource(R.string.trip_skip),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
        )
    }
}
