package fr.whitytoes.badgemoi.ui.components

import kotlin.time.Duration

/**
 * Verrou anti-double-tap (cahier des charges §3.2, seuil de 400 ms repris du POC).
 *
 * L'application se manipule en roulant, sur un board qui vibre : un appui unique peut
 * être enregistré deux fois. Sans ce verrou, valider un jalon en poserait deux.
 *
 * Volontairement isolé de Compose et sans horloge interne — l'appelant fournit l'instant
 * — afin de rester vérifiable en test unitaire, sans émulateur ni temporisation réelle.
 */
class TapGuard(
    private val minimumInterval: Duration,
) {
    private var lastAcceptedAtMillis: Long? = null

    /**
     * Accepte l'appui survenu à [nowMillis] et mémorise l'instant, ou le rejette s'il
     * suit de trop près le précédent appui **accepté**.
     *
     * Un appui rejeté ne décale pas la fenêtre : une rafale d'appuis parasites ne peut
     * donc pas prolonger indéfiniment le verrou.
     */
    fun accept(nowMillis: Long): Boolean {
        val last = lastAcceptedAtMillis
        val accepted = last == null || nowMillis - last >= minimumInterval.inWholeMilliseconds

        if (accepted) {
            lastAcceptedAtMillis = nowMillis
        }
        return accepted
    }
}
