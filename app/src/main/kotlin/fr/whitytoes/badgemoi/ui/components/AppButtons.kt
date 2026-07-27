package fr.whitytoes.badgemoi.ui.components

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

/**
 * Boutons de l'application : ceux de Material 3, dont l'ondulation est remplacée par
 * [solidRipple].
 *
 * Le détour est imposé par Material : ses composants n'appellent pas `LocalIndication`
 * mais `ripple()` en dur — vérifié dans le bytecode, `MaterialThemeKt` est la seule classe
 * du module à référencer `LocalIndication`, et c'est pour le *fournir*. Redéfinir cette
 * composition locale ne les atteint donc pas.
 *
 * Le montage tient en trois pièces :
 *
 * 1. `LocalRippleConfiguration provides null` éteint l'ondulation interne du bouton, la
 *    seule qui puisse encore scintiller ;
 * 2. la même [MutableInteractionSource] est passée au bouton et à `Modifier.indication`,
 *    de sorte que notre ondulation voie les appuis que le bouton reçoit ;
 * 3. `clip(shape)` borne le disque à la forme du bouton.
 *
 * La composition locale est neutralisée **ici seulement**, et non dans le thème : les
 * composants Material que l'on n'enveloppe pas — le sélecteur d'heure de l'overlay de
 * correction, par exemple — gardent ainsi leur retour d'appui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Button(
            onClick = onClick,
            modifier = modifier.clip(shape).indication(interactionSource, solidRipple()),
            shape = shape,
            colors = colors,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

/** Pendant de [AppButton] pour un bouton sans fond. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ButtonDefaults.textShape,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        TextButton(
            onClick = onClick,
            modifier = modifier.clip(shape).indication(interactionSource, solidRipple()),
            shape = shape,
            interactionSource = interactionSource,
            content = content,
        )
    }
}
