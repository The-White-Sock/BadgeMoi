package fr.whitytoes.badgemoi.domain

import kotlinx.coroutines.flow.Flow

/**
 * Accès au trajet en cours (objet unique). Partagé entre l'application et le
 * widget (cahier §3.6, §4.2) : la même source de vérité alimente les deux.
 */
interface ActiveTripRepository {
    fun observe(): Flow<Trip?>

    suspend fun get(): Trip?

    suspend fun save(trip: Trip)

    suspend fun clear()
}
