package fr.whitytoes.badgemoi.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import fr.whitytoes.badgemoi.ui.formatTime
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import fr.whitytoes.badgemoi.ui.theme.numericTextStyle
import java.time.Instant

/**
 * Bandeau Départ / Écoulé et mini-indicateur « depuis le dernier jalon »
 * (cahier des charges §3.2).
 *
 * [timers] est une **lambda** et non une valeur : la lecture de l'état est ainsi
 * différée jusqu'ici, ce qui cantonne la recomposition à la seconde à ce seul bandeau
 * plutôt qu'à tout l'écran.
 */
@Composable
fun TripTimerBanner(
    departureAt: Instant?,
    timers: () -> TripTimers,
    modifier: Modifier = Modifier,
) {
    val current = timers()

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            // L'heure de départ est figée : elle ne bouge plus une fois le trajet lancé.
            TimerField(
                labelRes = R.string.trip_departure,
                value = departureAt?.let { formatTime(it) },
            )
            TimerField(
                labelRes = R.string.trip_elapsed,
                value = current.elapsed?.let(::formatDuration),
            )
        }

        // Discret par nature : c'est une information de confort, pas le repère principal.
        val sinceLast = current.sinceLastMilestone
        if (sinceLast != null) {
            Text(
                text = stringResource(R.string.trip_since_last_milestone, formatDuration(sinceLast)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimerField(
    labelRes: Int,
    value: String?,
) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: stringResource(R.string.milestone_no_value),
            style = numericTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun previewTrip(): Trip {
    val departure = Instant.parse("2026-07-26T07:12:00Z")
    return Trip.start(id = "preview", direction = Direction.ALLER, departureAt = departure)
}

@Preview(name = "Bandeau — nuit", showBackground = true)
@Composable
private fun TripTimerBannerNightPreview() {
    val trip = previewTrip()
    BadgeMoiTheme(darkTheme = true) {
        TripTimerBanner(
            departureAt = trip.departureAt,
            timers = { trip.timersAt(Instant.parse("2026-07-26T07:24:34Z")) },
        )
    }
}

@Preview(name = "Bandeau — jour", showBackground = true)
@Composable
private fun TripTimerBannerDayPreview() {
    val trip = previewTrip()
    BadgeMoiTheme(darkTheme = false) {
        TripTimerBanner(
            departureAt = trip.departureAt,
            timers = { trip.timersAt(Instant.parse("2026-07-26T07:24:34Z")) },
        )
    }
}
