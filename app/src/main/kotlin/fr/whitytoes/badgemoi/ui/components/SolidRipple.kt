package fr.whitytoes.badgemoi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.RippleDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

/** Montée de l'opacité au contact — valeur de la spécification Material. */
private const val FADE_IN_MILLIS = 75

/** Expansion du disque, du point touché jusqu'au bord. */
private const val EXPAND_MILLIS = 225

/** Disparition après le relâchement. */
private const val FADE_OUT_MILLIS = 150

/** Rayon de départ, en proportion de la plus grande dimension de la surface. */
private const val START_RADIUS_RATIO = 0.3f

/** Débord du rayon final au-delà du coin, pour que l'ondulation atteigne franchement le bord. */
private val EndRadiusOvershoot = 10.dp

/**
 * Ondulation Material **dessinée par Compose**, en aplat.
 *
 * `androidx.compose.material3.ripple()` délègue au drawable de la plateforme :
 * `UnprojectedRipple` hérite de `android.graphics.drawable.RippleDrawable`, et
 * `RippleKt` passe systématiquement par `createPlatformRippleNode`. Or depuis Android 12
 * ce drawable peut adopter le style *patterned*, qui superpose un scintillement dont la
 * couleur — `effectColor` — vaut **blanc** par défaut. Sur le fond quasi noir du thème
 * nuit, cela donne le grain constaté sur appareil. Le style se choisit côté système et
 * aucune API publique de la `RippleDrawable` ne permet de le désactiver : seul
 * `setEffectColor` est exposé, et l'instance créée par Compose est hors d'atteinte.
 *
 * D'où cette implémentation. Elle reprend **la géométrie et le rythme de la
 * spécification Material** — mêmes durées, même rayon de départ proportionnel, même
 * dérive du centre vers le milieu de la surface pendant l'expansion, opacité pressée
 * empruntée à [RippleDefaults] — mais dessine un disque d'opacité **uniforme**. Il n'y a
 * donc pas de scintillement à afficher, sur aucun appareil.
 *
 * Le détourage n'est pas géré ici : il vient du `clip` que l'appelant pose en amont du
 * `clickable`, comme pour l'ondulation d'origine.
 */
fun solidRipple(color: Color): IndicationNodeFactory = SolidRipple(color)

private data class SolidRipple(
    private val color: Color,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        SolidRippleNode(interactionSource, color)
}

private class SolidRippleNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : Modifier.Node(),
    DrawModifierNode {
    private var origin by mutableStateOf(Offset.Zero)
    private val expansion = Animatable(0f)
    private val opacity = Animatable(0f)
    private var fadingIn: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> start(interaction.pressPosition)
                    is PressInteraction.Release, is PressInteraction.Cancel -> stop()
                    else -> Unit
                }
            }
        }
    }

    private fun start(pressPosition: Offset) {
        origin = pressPosition
        coroutineScope.launch {
            expansion.snapTo(0f)
            expansion.animateTo(1f, tween(durationMillis = EXPAND_MILLIS, easing = FastOutSlowInEasing))
        }
        fadingIn =
            coroutineScope.launch {
                opacity.animateTo(1f, tween(durationMillis = FADE_IN_MILLIS, easing = LinearEasing))
            }
    }

    private fun stop() {
        coroutineScope.launch {
            // La disparition attend la fin de la montée : sans cela, un tap plus bref que
            // les 75 ms de fondu d'entrée ne laisserait apparaître qu'un début de couleur.
            fadingIn?.join()
            opacity.animateTo(0f, tween(durationMillis = FADE_OUT_MILLIS, easing = LinearEasing))
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        val alpha = opacity.value * RippleDefaults.RippleAlpha.pressedAlpha
        if (alpha <= 0f) return

        val advance = expansion.value
        val startRadius = max(size.width, size.height) * START_RADIUS_RATIO
        val endRadius = sqrt(size.width * size.width + size.height * size.height) / 2 + EndRadiusOvershoot.toPx()

        drawCircle(
            color = color.copy(alpha = alpha),
            radius = startRadius + (endRadius - startRadius) * advance,
            // Le centre glisse du doigt vers le milieu de la surface à mesure que le
            // disque s'étend, comme le fait l'ondulation Material.
            center = lerp(origin, size.center, advance),
        )
    }
}
