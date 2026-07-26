package fr.whitytoes.badgemoi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.ui.trip.MilestoneRow
import fr.whitytoes.badgemoi.ui.trip.MilestoneStatus

private val NodeSize = 14.dp
private val CurrentNodeSize = 20.dp
private val ConnectorHeight = 2.dp

/** Un jalon est « réglé » dès qu'il est posé ou ignoré : dans les deux cas, on est passé. */
private fun MilestoneStatus.isSettled(): Boolean = this == MilestoneStatus.POSED || this == MilestoneStatus.SKIPPED

/**
 * Frise de progression façon plan de ligne (cahier des charges §3.2) : les jalons sont
 * reliés par un trait, le jalon courant est agrandi, les jalons traités se distinguent
 * de ceux restant à faire.
 *
 * Un jalon **ignoré** est rendu comme un jalon traité mais atténué : il fait avancer le
 * trajet sans avoir été pointé, et le confondre avec un jalon en attente laisserait
 * croire qu'il reste à valider.
 */
@Composable
fun TripProgressFrieze(
    rows: List<MilestoneRow>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        rows.forEachIndexed { position, row ->
            if (position > 0) {
                // Le segment reflète le jalon d'où l'on vient, pas celui où l'on va.
                Connector(reached = rows[position - 1].status.isSettled())
            }
            Node(status = row.status)
        }
    }
}

@Composable
private fun Node(status: MilestoneStatus) {
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
                .size(if (status == MilestoneStatus.CURRENT) CurrentNodeSize else NodeSize)
                .clip(CircleShape)
                .background(color),
    )
}

@Composable
private fun RowScope.Connector(reached: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(ConnectorHeight)
                .background(if (reached) colors.primary else colors.surfaceVariant),
    )
}
