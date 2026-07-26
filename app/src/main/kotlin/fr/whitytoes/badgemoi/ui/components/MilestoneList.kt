package fr.whitytoes.badgemoi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.theme.numericTextStyle
import fr.whitytoes.badgemoi.ui.trip.MilestoneRow
import fr.whitytoes.badgemoi.ui.trip.MilestoneStatus

private val BadgeSize = 12.dp
private val CurrentBadgeSize = 16.dp
private val RowMinHeight = 56.dp

/**
 * Liste des jalons d'un trajet (cahier des charges §3.2).
 *
 * Chaque ligne affiche le libellé du jalon et la **durée écoulée depuis le jalon
 * précédent** — jamais l'heure absolue, réservée au bandeau. Ce n'est pas un détail de
 * présentation mais une règle d'ergonomie du cahier.
 *
 * Le même composant sert au récapitulatif du lot 4 (§3.3). [onMilestoneClick] est
 * facultatif : le récapitulatif est en lecture seule, la correction d'un jalon (§3.5) se
 * faisant depuis l'écran actif.
 */
@Composable
fun MilestoneList(
    rows: List<MilestoneRow>,
    modifier: Modifier = Modifier,
    onMilestoneClick: ((Int) -> Unit)? = null,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = rows, key = { it.index }) { row ->
            MilestoneListRow(row = row, onClick = onMilestoneClick?.let { { it(row.index) } })
        }
    }
}

@Composable
private fun MilestoneListRow(
    row: MilestoneRow,
    onClick: (() -> Unit)?,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(clickable)
                .heightIn(min = RowMinHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(status = row.status)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor(row.status),
            )
        }
        TrailingValue(row = row)
    }
}

/**
 * Marqueur d'état. Le jeu de 24 icônes vectorielles du cahier §1.5 arrive au lot 7 ;
 * d'ici là, l'état est porté par la forme et la couleur, et le sens du jalon par son
 * libellé. [MilestoneRow.icon] est déjà transporté pour que ce remplacement soit local.
 */
@Composable
private fun StatusBadge(status: MilestoneStatus) {
    val colors = MaterialTheme.colorScheme
    val color =
        when (status) {
            MilestoneStatus.POSED -> colors.primary
            MilestoneStatus.SKIPPED -> colors.outline
            MilestoneStatus.CURRENT -> colors.secondary
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
private fun TrailingValue(row: MilestoneRow) {
    val colors = MaterialTheme.colorScheme

    when {
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
                color = colors.onSurfaceVariant,
            )
    }
}

@Composable
private fun labelColor(status: MilestoneStatus) =
    when (status) {
        MilestoneStatus.CURRENT -> MaterialTheme.colorScheme.secondary
        MilestoneStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
