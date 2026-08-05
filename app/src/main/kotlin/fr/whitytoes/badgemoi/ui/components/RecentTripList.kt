package fr.whitytoes.badgemoi.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.TripPace
import fr.whitytoes.badgemoi.ui.formatDate
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.history.RecentTripRow
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import fr.whitytoes.badgemoi.ui.theme.numeric
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Hauteur de ligne. La liste n'est **pas** interactive : ce n'est donc pas une contrainte
 * de cible tactile mais de lisibilité, l'écran se consultant à l'arrêt.
 */
private val RowMinHeight = 48.dp

/**
 * Liste des trajets récents avec leur écart à la moyenne (cahier des charges §3.4).
 *
 * La couleur porte l'écart : vert plus rapide, rouge plus lent, encre neutre dans la
 * moyenne. Les seuils viennent du POC et sont appliqués en amont — cette liste ne décide
 * de rien, elle rend ce que `RecentTripRow.pace` a tranché.
 *
 * Pas de `LazyColumn` : dix lignes au plus, dans la zone déjà défilante de l'écran. Une
 * liste paresseuse y serait mesurée sous une hauteur maximale infinie, ce que Compose
 * refuse.
 */
@Composable
fun RecentTripList(
    rows: List<RecentTripRow>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { position, row ->
            RecentTripListRow(row = row)
            if (position < rows.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun RecentTripListRow(row: RecentTripRow) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDate(row.at),
            style = MaterialTheme.typography.bodyLarge.numeric(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(12.dp))
        TripTotal(row = row)
    }
}

@Composable
private fun TripTotal(row: RecentTripRow) {
    val total = row.total

    if (total == null) {
        // Un trajet dont l'arrivée n'a pas été pointée n'est ni rapide ni lent : il n'est
        // pas mesuré. Le peindre en neutre le ferait passer pour « dans la moyenne ».
        Text(
            text = stringResource(R.string.segment_not_measured),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = formatDuration(total),
            style = MaterialTheme.typography.bodyLarge.numeric(),
            color = paceColor(row.pace),
        )
    }
}

/**
 * Le vert est le `success` des couleurs étendues — défini au lot 2, sans emploi jusqu'ici.
 * Le rouge est celui des erreurs du thème. Aucune couleur littérale (cahier §5).
 */
@Composable
private fun paceColor(pace: TripPace?): Color =
    when (pace) {
        TripPace.FASTER -> BadgeMoiTheme.extendedColors.success
        TripPace.SLOWER -> MaterialTheme.colorScheme.error
        TripPace.TYPICAL, null -> MaterialTheme.colorScheme.onSurface
    }

/** Les quatre cas de la liste, dans l'ordre où on veut pouvoir les distinguer. */
@Suppress("MagicNumber") // Données d'aperçu : durées en clair.
private fun previewRows(): List<RecentTripRow> {
    val day = Instant.parse("2026-07-26T07:12:00Z")
    return listOf(
        RecentTripRow("1", day, 24.minutes, TripPace.FASTER),
        RecentTripRow("2", day.minusSeconds(86_400), 31.minutes, TripPace.SLOWER),
        RecentTripRow("3", day.minusSeconds(172_800), 28.minutes, TripPace.TYPICAL),
        RecentTripRow("4", day.minusSeconds(259_200), null, null),
    )
}

@Preview(name = "Trajets récents — nuit", showBackground = true)
@Composable
private fun RecentTripListNightPreview() {
    BadgeMoiTheme(darkTheme = true) { RecentTripList(rows = previewRows()) }
}

@Preview(name = "Trajets récents — jour", showBackground = true)
@Composable
private fun RecentTripListDayPreview() {
    BadgeMoiTheme(darkTheme = false) { RecentTripList(rows = previewRows()) }
}
