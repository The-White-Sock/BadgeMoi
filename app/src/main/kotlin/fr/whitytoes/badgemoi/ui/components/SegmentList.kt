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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.summary.SegmentRow
import fr.whitytoes.badgemoi.ui.summary.segmentRows
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import fr.whitytoes.badgemoi.ui.theme.numericTextStyle
import java.time.Instant
import kotlin.time.Duration

private val RowMinHeight = 44.dp

/**
 * Liste des tronçons nommés d'un trajet avec leur durée (cahier des charges §3.3).
 *
 * Prend des [SegmentRow] et non un trajet : l'écran des moyennes (lot 5, §3.4) affiche la
 * même structure avec des durées moyennées, et pourra donc réutiliser ce composant tel
 * quel plutôt qu'en écrire un second.
 *
 * Pas de `LazyColumn` ici — quatre lignes, et la liste vit dans la zone déjà scrollable du
 * récapitulatif. Une liste paresseuse y provoquerait des défilements imbriqués.
 */
@Composable
fun SegmentList(
    rows: List<SegmentRow>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { position, row ->
            SegmentListRow(row = row)
            if (position < rows.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun SegmentListRow(row: SegmentRow) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Les extrémités d'abord : les deux tronçons « Ride » portent le même nom, et
            // seules elles les distinguent. Le POC procède de même, en montrant le couple
            // d'icônes des jalons et en reléguant le nom du tronçon au second plan.
            Text(
                text = stringResource(R.string.segment_between, row.fromLabel, row.toLabel),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        SegmentValue(duration = row.duration)
    }
}

@Composable
private fun SegmentValue(duration: Duration?) {
    val colors = MaterialTheme.colorScheme

    if (duration == null) {
        // Un tronçon non mesurable — l'un de ses jalons a été ignoré — n'est pas un
        // tronçon de durée nulle. Le dire par un libellé, et non par « 0:00 » qui se
        // lirait comme une mesure.
        Text(
            text = stringResource(R.string.segment_not_measured),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    } else {
        Text(
            text = formatDuration(duration),
            style = numericTextStyle,
            color = colors.onSurface,
        )
    }
}

/** Trajet d'aperçu : un tronçon non mesurable, faute d'un jalon ignoré. */
@Suppress("MagicNumber") // Données d'aperçu : indices de jalons et durées en clair.
private fun previewRows(): List<SegmentRow> {
    val departure = Instant.parse("2026-07-26T07:12:00Z")
    return Trip
        .start(id = "preview", direction = Direction.ALLER, departureAt = departure)
        .poseMilestone(1, departure.plusSeconds(560))
        .skipMilestone(2)
        .poseMilestone(3, departure.plusSeconds(1_920))
        .poseMilestone(4, departure.plusSeconds(2_400))
        .segmentRows()
}

@Preview(name = "Tronçons — nuit", showBackground = true)
@Composable
private fun SegmentListNightPreview() {
    BadgeMoiTheme(darkTheme = true) { SegmentList(rows = previewRows()) }
}

@Preview(name = "Tronçons — jour", showBackground = true)
@Composable
private fun SegmentListDayPreview() {
    BadgeMoiTheme(darkTheme = false) { SegmentList(rows = previewRows()) }
}
