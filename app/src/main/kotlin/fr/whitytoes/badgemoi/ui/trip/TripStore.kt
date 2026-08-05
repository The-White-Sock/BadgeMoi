package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.Trip
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import kotlinx.coroutines.flow.first

/**
 * Le trajet sur lequel une correction s'applique, et l'endroit où elle s'écrit.
 *
 * Un jalon se corrige sur le trajet **en cours** comme sur un trajet **archivé** : la
 * règle est la même, seule la source diffère. Cette abstraction est ce qui permet à
 * [MilestoneCorrections] de l'ignorer.
 */
interface TripStore {
    suspend fun get(): Trip?

    suspend fun save(trip: Trip)
}

/** Le trajet en cours (écran actif, et récapitulatif de fin de parcours). */
class ActiveTripStore(
    private val repository: ActiveTripRepository,
) : TripStore {
    override suspend fun get(): Trip? = repository.get()

    override suspend fun save(trip: Trip) = repository.save(trip)
}

/**
 * Un trajet de l'archive, désigné par son identifiant.
 *
 * L'écriture passe par `add` : le DAO remplace sur conflit d'identifiant, si bien
 * qu'enregistrer un trajet déjà archivé le met à jour. Pas de seconde opération à
 * introduire pour cela.
 */
class ArchivedTripStore(
    private val repository: TripArchiveRepository,
    private val tripId: String,
) : TripStore {
    override suspend fun get(): Trip? = repository.observeAll().first().firstOrNull { it.id == tripId }

    override suspend fun save(trip: Trip) = repository.add(trip)
}
