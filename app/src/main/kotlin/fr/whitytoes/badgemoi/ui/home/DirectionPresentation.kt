package fr.whitytoes.badgemoi.ui.home

import androidx.annotation.StringRes
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Routes

/** Séparateur de l'aperçu de parcours (ponctuation, pas du texte traduisible). */
private const val ROUTE_SEPARATOR = " › "

/**
 * Libellé d'un sens côté interface. [Direction] porte déjà un libellé, mais il décrit
 * le modèle : le texte affiché passe par les ressources, seules traduisibles.
 */
@StringRes
internal fun Direction.labelRes(): Int =
    when (this) {
        Direction.ALLER -> R.string.direction_aller
        Direction.RETOUR -> R.string.direction_retour
    }

/**
 * Aperçu textuel du parcours, enchaînant les libellés de jalons. La frise d'icônes du
 * cahier §3.1 le remplacera une fois le jeu vectoriel livré au lot 7.
 */
internal fun routePreview(direction: Direction): String =
    Routes
        .forDirection(direction)
        .milestones
        .joinToString(separator = ROUTE_SEPARATOR) { it.label }
