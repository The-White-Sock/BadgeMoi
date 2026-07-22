package fr.whitytoes.badgemoi.data.local

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Trip
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Forme sérialisable d'un [Trip] pour le DataStore (trajet en cours). Les
 * horodatages sont stockés en millisecondes epoch (`null` = jalon non posé) et le
 * sens par son nom, pour garder le modèle du domaine indépendant de la sérialisation.
 */
@Serializable
data class StoredTrip(
    val id: String,
    val direction: String,
    val createdAtEpochMs: Long,
    val times: List<Long?>,
    val skipped: List<Boolean>,
)

fun Trip.toStored(): StoredTrip =
    StoredTrip(
        id = id,
        direction = direction.name,
        createdAtEpochMs = createdAt.toEpochMilli(),
        times = times.map { it?.toEpochMilli() },
        skipped = skipped,
    )

fun StoredTrip.toDomain(): Trip =
    Trip(
        id = id,
        direction = Direction.valueOf(direction),
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        times = times.map { it?.let(Instant::ofEpochMilli) },
        skipped = skipped,
    )
