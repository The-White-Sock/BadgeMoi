package fr.whitytoes.badgemoi.ui

import androidx.annotation.StringRes
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction

/**
 * Libellé d'un sens côté interface. [Direction] porte déjà un libellé, mais il décrit
 * le modèle : le texte affiché passe par les ressources, seules traduisibles.
 *
 * Vit au niveau du paquet `ui` et non dans celui d'un écran : l'accueil le nomme sur ses
 * boutons de démarrage, la fenêtre de reprise sur le trajet retrouvé.
 */
@StringRes
internal fun Direction.labelRes(): Int =
    when (this) {
        Direction.ALLER -> R.string.direction_aller
        Direction.RETOUR -> R.string.direction_retour
    }
