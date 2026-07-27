package fr.whitytoes.badgemoi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.theme.numericTextStyle
import fr.whitytoes.badgemoi.ui.trip.MilestoneRow
import fr.whitytoes.badgemoi.ui.trip.MilestoneStatus
import fr.whitytoes.badgemoi.ui.trip.isCorrectable
import kotlinx.coroutines.delay
import kotlin.time.Duration

private val BadgeSize = 12.dp
private val CurrentBadgeSize = 16.dp
private val RowMinHeight = 56.dp
private val RowShape = RoundedCornerShape(12.dp)
private val CurrentAccentWidth = 5.dp
private const val PRESS_FADE_MILLIS = 120

/**
 * Durée plancher du retour d'appui. Un tap dure moins que le temps de montée du fondu :
 * sans plancher, la couleur d'appui commence à peine à apparaître qu'elle repart déjà.
 * Même remède que le flash de validation de la barre d'action.
 */
private const val PRESS_HOLD_MILLIS = 180L

/**
 * Liste des jalons d'un trajet (cahier des charges §3.2).
 *
 * Chaque ligne affiche le libellé du jalon et la **durée écoulée depuis le jalon
 * précédent** — jamais l'heure absolue, réservée au bandeau. Ce n'est pas un détail de
 * présentation mais une règle d'ergonomie du cahier.
 *
 * Le même composant sert au récapitulatif du lot 4 (§3.3), d'où deux paramètres
 * facultatifs : le récapitulatif est en lecture seule et figé, alors que l'écran actif
 * corrige un jalon (§3.5) et fait courir un chronomètre.
 *
 * @param onMilestoneClick ouvre la correction d'un jalon. Seuls les jalons **déjà
 *   tranchés** (posés ou ignorés) sont cliquables : corriger un jalon qu'on n'a pas
 *   encore atteint reviendrait à inventer un passage.
 * @param runningSince temps écoulé depuis le dernier jalon posé, affiché sur la ligne du
 *   jalon **courant**. Volontairement une lambda : la lecture de l'état est différée
 *   jusqu'à cette seule ligne, qui est donc la seule à se recomposer chaque seconde.
 */
@Composable
fun MilestoneList(
    rows: List<MilestoneRow>,
    modifier: Modifier = Modifier,
    onMilestoneClick: ((Int) -> Unit)? = null,
    runningSince: (() -> Duration?)? = null,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = rows, key = { it.index }) { row ->
            MilestoneListRow(
                row = row,
                onClick = if (row.status.isCorrectable) onMilestoneClick?.let { { it(row.index) } } else null,
                runningSince = runningSince,
            )
        }
    }
}

@Composable
private fun MilestoneListRow(
    row: MilestoneRow,
    onClick: (() -> Unit)?,
    runningSince: (() -> Duration?)?,
) {
    val colors = MaterialTheme.colorScheme
    val current = row.status == MilestoneStatus.CURRENT
    val accent = colors.primary

    val interactionSource = remember { MutableInteractionSource() }
    val background = rowBackgroundColor(interactionSource = interactionSource, current = current)

    val clickable =
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
        } else {
            Modifier
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                // Découpe avant le fond : le coin arrondi vaut aussi pour l'aplat d'appui.
                .clip(RowShape)
                .background(background)
                // Barre d'accent du POC (`border-left:5px solid var(--amber)`). C'est elle
                // qui porte le « vous êtes ici » : un aplat seul se confond avec l'aplat
                // d'appui et donne au jalon courant — le seul non modifiable — l'air d'être
                // la ligne cliquable.
                .drawBehind {
                    if (current) {
                        drawRect(color = accent, size = Size(CurrentAccentWidth.toPx(), size.height))
                    }
                }.then(clickable)
                .heightIn(min = RowMinHeight)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(status = row.status)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.label,
                style = if (current) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                color = labelColor(row.status),
            )
        }
        TrailingValue(row = row, runningSince = runningSince)
    }
}

/**
 * Fond d'une ligne de jalon, appui compris.
 *
 * Le retour d'appui est un **aplat**, et non l'ondulation Material : celle-ci est un
 * dégradé radial à faible opacité, que le GPU trame sur le fond quasi noir du thème nuit
 * — le « bruit blanc » constaté sur appareil. Un aplat n'a pas de dégradé, donc rien à
 * tramer.
 *
 * Deux réglages le rendent perceptible :
 *
 * - la couleur est `secondaryContainer` et non `surfaceVariant`, un gris de panneau à
 *   deux doigts du fond en thème nuit. Un décalage de **teinte** se voit là où un
 *   décalage de luminosité passait inaperçu ;
 * - l'état est maintenu [PRESS_HOLD_MILLIS] après le relâchement, sans quoi un tap —
 *   plus bref que le fondu — ne laisserait apparaître qu'un début de couleur.
 */
@Composable
private fun rowBackgroundColor(
    interactionSource: MutableInteractionSource,
    current: Boolean,
): Color {
    val colors = MaterialTheme.colorScheme
    val pressed by interactionSource.collectIsPressedAsState()

    var held by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (pressed) {
            held = true
        } else if (held) {
            delay(PRESS_HOLD_MILLIS)
            held = false
        }
    }

    val background by
        animateColorAsState(
            targetValue =
                when {
                    held -> colors.secondaryContainer
                    // Ambre du POC (`.mrow.current{background:var(--amber-soft)}`), et non
                    // le teal : celui-ci est la couleur des jalons **posés**.
                    current -> colors.primaryContainer
                    else -> Color.Transparent
                },
            animationSpec = tween(durationMillis = PRESS_FADE_MILLIS),
            label = "fond de la ligne de jalon",
        )

    return background
}

/**
 * Marqueur d'état. Le jeu de 24 icônes vectorielles du cahier §1.5 arrive au lot 7 ;
 * d'ici là, l'état est porté par la forme et la couleur, et le sens du jalon par son
 * libellé. [MilestoneRow.icon] est déjà transporté pour que ce remplacement soit local.
 */
@Composable
private fun StatusBadge(status: MilestoneStatus) {
    val colors = MaterialTheme.colorScheme
    // Répartition du POC : teal pour ce qui est fait (`.mrow.done`), ambre pour le jalon
    // courant (`.mrow.current`). Je les avais inversées.
    val color =
        when (status) {
            MilestoneStatus.POSED -> colors.secondary
            MilestoneStatus.SKIPPED -> colors.outline
            MilestoneStatus.CURRENT -> colors.primary
            MilestoneStatus.PENDING -> colors.surfaceVariant
        }

    Box(
        modifier =
            Modifier
                .size(if (status == MilestoneStatus.CURRENT) CurrentBadgeSize else BadgeSize)
                .clip(CircleShape)
                .background(color),
    )
}

@Composable
private fun TrailingValue(
    row: MilestoneRow,
    runningSince: (() -> Duration?)?,
) {
    val colors = MaterialTheme.colorScheme
    val current = row.status == MilestoneStatus.CURRENT
    // Le chronomètre en cours vit sur la ligne du jalon à valider, là où le regard se
    // pose, plutôt que dans le bandeau où il doublait la valeur « Écoulé ».
    val running = if (current) runningSince?.invoke() else null

    when {
        running != null -> {
            val value = formatDuration(running)
            // Hors contexte, un chronomètre nu ne dit pas ce qu'il compte : la
            // description sonore reprend la phrase que portait le bandeau.
            val description = stringResource(R.string.trip_since_last_milestone, value)
            Text(
                text = value,
                style = numericTextStyle,
                color = colors.onPrimaryContainer,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }

        // « Ignoré » plutôt qu'une durée vide : le jalon a été traité, pas oublié.
        row.status == MilestoneStatus.SKIPPED ->
            Text(
                text = stringResource(R.string.milestone_skipped),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )

        row.sincePrevious != null ->
            Text(
                text = formatDuration(row.sincePrevious),
                style = numericTextStyle,
                color = colors.onSurface,
            )

        else ->
            Text(
                text = stringResource(R.string.milestone_no_value),
                style = numericTextStyle,
                color = if (current) colors.onPrimaryContainer else colors.onSurfaceVariant,
            )
    }
}

@Composable
private fun labelColor(status: MilestoneStatus) =
    when (status) {
        MilestoneStatus.CURRENT -> MaterialTheme.colorScheme.onPrimaryContainer
        MilestoneStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
