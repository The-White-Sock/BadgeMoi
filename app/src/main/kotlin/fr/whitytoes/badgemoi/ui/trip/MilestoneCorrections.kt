package fr.whitytoes.badgemoi.ui.trip

import fr.whitytoes.badgemoi.domain.Trip
import java.time.Clock
import java.time.Instant

/**
 * Les quatre corrections possibles sur un jalon (cahier des charges §3.5).
 *
 * Partagées par l'écran actif et le récapitulatif, qui offrent tous deux la correction —
 * le POC rend les lignes de jalon cliquables sur les deux écrans. Les regrouper ici évite
 * de dupliquer la subtilité de [correctionInstant], qui a besoin à la fois de l'heure
 * courante et du fuseau, et qui doit les prendre à la **même** source.
 *
 * Chaque opération relit le trajet plutôt que de recevoir un instantané : deux écrans
 * peuvent l'observer, et écrire à partir d'une copie périmée écraserait une modification
 * faite entre-temps.
 *
 * Le [TripStore] dit **où** lire et écrire — trajet en cours ou trajet archivé. La règle
 * de correction, elle, ne dépend pas de la source : corriger un jalon après coup dans
 * l'historique doit produire le même trajet que le corriger sur le moment.
 */
class MilestoneCorrections(
    private val store: TripStore,
    private val clock: Clock,
) {
    /**
     * Fixe l'heure locale saisie dans l'overlay.
     *
     * L'horloge vit ici, pas dans les composables : l'écran se contente de transmettre
     * ce qu'affiche le sélecteur, et le fuseau vient de la même horloge que l'instant,
     * ce qu'un test peut donc fixer d'un seul geste.
     */
    suspend fun correct(
        index: Int,
        hour: Int,
        minute: Int,
    ) {
        val now = clock.instant()
        update { trip ->
            trip.poseMilestone(index, trip.correctionInstant(index, hour, minute, now, clock.zone))
        }
    }

    /** Pose le jalon à un instant déjà connu. */
    suspend fun pose(
        index: Int,
        at: Instant,
    ) = update { trip -> trip.poseMilestone(index, at) }

    /** Marque le jalon comme ignoré : on est passé sans pointer. */
    suspend fun skip(index: Int) = update { trip -> trip.skipMilestone(index) }

    /** Remet le jalon en attente, donc à traiter de nouveau. */
    suspend fun clear(index: Int) = update { trip -> trip.clearMilestone(index) }

    private suspend fun update(transform: (Trip) -> Trip) {
        val current = store.get() ?: return
        store.save(transform(current))
    }
}
