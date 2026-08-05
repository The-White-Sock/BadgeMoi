package fr.whitytoes.badgemoi.ui.home

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.Routes

/** Séparateur de l'aperçu de parcours (ponctuation, pas du texte traduisible). */
private const val ROUTE_SEPARATOR = " › "

/**
 * Aperçu textuel du parcours, enchaînant les libellés de jalons. La frise d'icônes du
 * cahier §3.1 le remplacera une fois le jeu vectoriel livré au lot 7.
 */
internal fun routePreview(direction: Direction): String =
    Routes
        .forDirection(direction)
        .milestones
        .joinToString(separator = ROUTE_SEPARATOR) { it.label }
