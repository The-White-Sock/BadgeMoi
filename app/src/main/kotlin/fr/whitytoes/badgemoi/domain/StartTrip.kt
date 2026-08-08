package fr.whitytoes.badgemoi.domain

import java.time.Clock

/**
 * Démarre un trajet dans le sens demandé.
 *
 * C'est **la** règle de démarrage, et elle n'existe qu'ici : l'écran d'accueil
 * (§3.1) et le widget (§3.6) l'appellent tous les deux. Deux copies auraient
 * divergé, et c'est précisément le genre d'écart qu'un widget rend invisible —
 * l'utilisateur ne voit pas le résultat immédiatement.
 *
 * La classe est du Kotlin pur, sans annotation d'injection : `domain/` ne connaît ni
 * Dagger ni `javax.inject`, et c'est `di/DomainModule` qui la construit.
 *
 * @property newTripId fabrique d'identifiants, injectée plutôt qu'appelée en dur.
 *   `java.util.UUID` n'est pas une dépendance tolérée du domaine, et un identifiant
 *   figé rend le cas d'usage vérifiable.
 */
class StartTrip(
    private val activeTripRepository: ActiveTripRepository,
    private val clock: Clock,
    private val newTripId: () -> String,
) {
    /**
     * Enregistre un trajet neuf et répond `true` s'il a bien été créé, `false` si un
     * trajet était déjà en cours.
     *
     * La garde « déjà en cours » n'est **pas** lue ici avant d'écrire : elle vit dans
     * [ActiveTripRepository.saveIfNoneInProgress], donc dans l'écriture elle-même. Un
     * `get()` suivi d'un `save()` laisserait passer deux appuis rapprochés entre les
     * deux appels — et sur un widget, deux appuis rapides sont plus probables que
     * dans l'application.
     */
    suspend operator fun invoke(direction: Direction): Boolean =
        activeTripRepository.saveIfNoneInProgress(
            Trip.start(
                id = newTripId(),
                direction = direction,
                departureAt = clock.instant(),
            ),
        )
}
