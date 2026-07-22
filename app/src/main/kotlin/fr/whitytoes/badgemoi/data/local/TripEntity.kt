package fr.whitytoes.badgemoi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import java.time.Instant

/**
 * Ligne Room de l'archive des trajets. Le sens et l'horodatage de création sont des
 * colonnes propres (filtrage par sens, tri par date) ; les horodatages par jalon et
 * les jalons ignorés sont stockés sérialisés (voir [Converters]).
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val direction: String,
    val createdAtEpochMs: Long,
    val times: List<Long?>,
    val skipped: List<Boolean>,
)

fun Trip.toEntity(): TripEntity =
    TripEntity(
        id = id,
        direction = direction.name,
        createdAtEpochMs = createdAt.toEpochMilli(),
        times = times.map { it?.toEpochMilli() },
        skipped = skipped,
    )

fun TripEntity.toDomain(): Trip =
    Trip(
        id = id,
        direction = Direction.valueOf(direction),
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        times = times.map { it?.let(Instant::ofEpochMilli) },
        skipped = skipped,
    )
