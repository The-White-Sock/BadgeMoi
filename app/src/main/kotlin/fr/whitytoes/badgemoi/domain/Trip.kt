package fr.whitytoes.badgemoi.domain

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Un trajet, en cours ou archivé. Porté depuis le modèle du POC : un trajet est
 * identifié par son sens, son horodatage de départ, la liste des horodatages par
 * jalon et la liste des jalons ignorés.
 *
 * @property times horodatage de chaque jalon (index 0 = départ) ; `null` tant que
 *   le jalon n'est pas posé. Taille = [Routes.MILESTONE_COUNT].
 * @property skipped pour chaque jalon, `true` s'il a été ignoré. Même taille.
 */
data class Trip(
    val id: String,
    val direction: Direction,
    val createdAt: Instant,
    val times: List<Instant?>,
    val skipped: List<Boolean>,
) {
    init {
        require(times.size == Routes.MILESTONE_COUNT) {
            "times doit contenir ${Routes.MILESTONE_COUNT} entrées, reçu ${times.size}"
        }
        require(skipped.size == Routes.MILESTONE_COUNT) {
            "skipped doit contenir ${Routes.MILESTONE_COUNT} entrées, reçu ${skipped.size}"
        }
    }

    val route: RouteDefinition get() = Routes.forDirection(direction)

    /**
     * Index du jalon courant = premier jalon ni posé ni ignoré. Vaut
     * [Routes.MILESTONE_COUNT] quand tous les jalons sont traités (trajet terminé) —
     * équivalent du `recomputeStep` du POC.
     */
    val currentStep: Int
        get() {
            for (i in 0 until Routes.MILESTONE_COUNT) {
                if (times[i] == null && !skipped[i]) return i
            }
            return Routes.MILESTONE_COUNT
        }

    /** Vrai quand tous les jalons sont posés ou ignorés. */
    val isComplete: Boolean get() = currentStep == Routes.MILESTONE_COUNT

    /** Horodatage de départ (jalon 0). */
    val departureAt: Instant? get() = times.first()

    /** Horodatage d'arrivée (dernier jalon), `null` tant qu'il n'est pas posé. */
    val arrivalAt: Instant? get() = times.last()

    /**
     * Durée d'un tronçon = intervalle entre ses deux jalons, `null` si l'un des deux
     * n'est pas posé (fidèle au `durOf` du POC).
     */
    fun durationOf(segment: SegmentDefinition): Duration? = durationBetween(segment.fromIndex, segment.toIndex)

    /** Durée totale du trajet (départ → arrivée), `null` si l'arrivée manque. */
    val totalDuration: Duration?
        get() = durationBetween(0, Routes.MILESTONE_COUNT - 1)

    /** Pose (ou repose) le jalon [index] à l'instant [at], en levant un éventuel « ignoré ». */
    fun poseMilestone(
        index: Int,
        at: Instant,
    ): Trip =
        copy(
            times = times.withValueAt(index, at),
            skipped = skipped.withValueAt(index, false),
        )

    /** Marque le jalon [index] comme ignoré et efface son horodatage. */
    fun skipMilestone(index: Int): Trip =
        copy(
            times = times.withValueAt(index, null),
            skipped = skipped.withValueAt(index, true),
        )

    /** Efface le jalon [index] (ni posé, ni ignoré). */
    fun clearMilestone(index: Int): Trip =
        copy(
            times = times.withValueAt(index, null),
            skipped = skipped.withValueAt(index, false),
        )

    private fun durationBetween(
        fromIndex: Int,
        toIndex: Int,
    ): Duration? {
        val from = times[fromIndex]
        val to = times[toIndex]
        return if (from != null && to != null) {
            (to.toEpochMilli() - from.toEpochMilli()).milliseconds
        } else {
            null
        }
    }

    companion object {
        /** Crée un trajet neuf : départ posé à [departureAt], jalons suivants en attente. */
        fun start(
            id: String,
            direction: Direction,
            departureAt: Instant,
        ): Trip =
            Trip(
                id = id,
                direction = direction,
                createdAt = departureAt,
                times = List(Routes.MILESTONE_COUNT) { if (it == 0) departureAt else null },
                skipped = List(Routes.MILESTONE_COUNT) { false },
            )
    }
}

private fun <T> List<T>.withValueAt(
    index: Int,
    value: T,
): List<T> = toMutableList().also { it[index] = value }
