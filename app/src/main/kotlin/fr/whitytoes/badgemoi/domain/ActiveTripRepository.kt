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

    /**
     * Enregistre [trip] **si et seulement si** aucun trajet n'est en cours, et répond
     * `true` quand l'enregistrement a eu lieu.
     *
     * Le test et l'écriture doivent être **atomiques** vis-à-vis des autres écritures :
     * c'est cette indivisibilité, et non un `get()` préalable, qui garantit que deux
     * appuis rapprochés ne créent pas deux trajets (#114). Toute implémentation qui
     * relirait l'état avant d'écrire rouvrirait la fenêtre.
     */
    suspend fun saveIfNoneInProgress(trip: Trip): Boolean

    suspend fun clear()
}
