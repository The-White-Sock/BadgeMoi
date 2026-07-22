package fr.whitytoes.badgemoi.domain

import kotlin.time.Duration

/** Moyenne d'un tronçon calculée sur un ensemble de trajets. */
data class SegmentAverage(
    val segment: SegmentDefinition,
    val average: Duration?,
    val sampleCount: Int,
)

/** Statistiques agrégées pour un sens de trajet (écran Historique, lot 5). */
data class DirectionStatistics(
    val direction: Direction,
    val tripCount: Int,
    val totalAverage: Duration?,
    val segmentAverages: List<SegmentAverage>,
)

/** Position d'un trajet par rapport à la moyenne (code couleur de l'historique). */
enum class TripPace { FASTER, SLOWER, TYPICAL }

/**
 * Calculs d'historique portés depuis le `renderStats` du POC : moyennes par tronçon
 * et durée totale moyenne, par sens, en ne considérant que les durées mesurables.
 */
object TripStatistics {
    /** Seuils repris du POC : < moyenne × 0.95 → plus rapide ; > moyenne × 1.1 → plus lent. */
    private const val FASTER_RATIO = 0.95
    private const val SLOWER_RATIO = 1.10

    fun forDirection(
        trips: List<Trip>,
        direction: Direction,
    ): DirectionStatistics {
        val relevant = trips.filter { it.direction == direction }
        val route = Routes.forDirection(direction)

        val segmentAverages =
            route.segments.map { segment ->
                val samples = relevant.mapNotNull { it.durationOf(segment) }
                SegmentAverage(segment, samples.averageOrNull(), samples.size)
            }
        val totals = relevant.mapNotNull { it.totalDuration }

        return DirectionStatistics(
            direction = direction,
            tripCount = relevant.size,
            totalAverage = totals.averageOrNull(),
            segmentAverages = segmentAverages,
        )
    }

    /** Positionne la durée totale d'un trajet par rapport à une moyenne. */
    fun paceOf(
        total: Duration?,
        average: Duration?,
    ): TripPace {
        if (total == null || average == null) return TripPace.TYPICAL
        return when {
            total < average * FASTER_RATIO -> TripPace.FASTER
            total > average * SLOWER_RATIO -> TripPace.SLOWER
            else -> TripPace.TYPICAL
        }
    }
}

private fun List<Duration>.averageOrNull(): Duration? =
    if (isEmpty()) null else fold(Duration.ZERO, Duration::plus) / size
