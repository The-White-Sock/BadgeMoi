package fr.whitytoes.badgemoi.ui.history

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.domain.TripPace
import fr.whitytoes.badgemoi.domain.TripStatistics
import java.time.Instant
import kotlin.time.Duration

/** Nombre de trajets récents affichés (cahier §3.4, comme le POC). */
const val RECENT_TRIP_COUNT = 10

/**
 * Une ligne de la liste des trajets récents, prête à afficher (cahier §3.4).
 *
 * @property at date du trajet, servant aussi de clé de tri.
 * @property total durée totale, `null` quand l'arrivée n'a pas été pointée.
 * @property pace position par rapport à la moyenne du sens. **`null` quand la durée n'est
 *   pas mesurable**, et non [TripPace.TYPICAL] : `TripStatistics.paceOf` retombe sur
 *   « typique » faute de mieux, mais afficher un trajet non mesuré comme étant « dans la
 *   moyenne » serait une affirmation qu'on ne peut pas soutenir. Le distinguer suit ce que
 *   « Non mesuré » fait déjà pour un tronçon.
 */
data class RecentTripRow(
    val id: String,
    val at: Instant,
    val total: Duration?,
    val pace: TripPace?,
)

/**
 * Dérive les lignes des trajets récents d'un sens.
 *
 * Le tri est **explicite** : `TripArchiveRepository.observeAll()` ne promet aucun ordre,
 * et se fier à celui que rend le dépôt du jour reviendrait à dépendre d'un détail
 * d'implémentation. La date de référence est celle du départ, avec `createdAt` en
 * secours — un trajet archivé l'a toujours, là où `departureAt` est typé nullable.
 *
 * @param average moyenne du sens, servant de référence à l'allure. Elle vient des
 *   statistiques déjà calculées : la recalculer ici ferait deux vérités.
 */
fun List<Trip>.recentTripRows(
    direction: Direction,
    average: Duration?,
): List<RecentTripRow> =
    asSequence()
        .filter { it.direction == direction }
        .sortedByDescending { it.dateOf() }
        .take(RECENT_TRIP_COUNT)
        .map { trip ->
            val total = trip.totalDuration
            RecentTripRow(
                id = trip.id,
                at = trip.dateOf(),
                total = total,
                pace = total?.let { TripStatistics.paceOf(it, average) },
            )
        }.toList()

private fun Trip.dateOf(): Instant = departureAt ?: createdAt
