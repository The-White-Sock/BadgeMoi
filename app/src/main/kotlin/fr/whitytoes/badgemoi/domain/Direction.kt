package fr.whitytoes.badgemoi.domain

/**
 * Sens d'un trajet domicile-travail, porté à l'identique depuis le POC
 * (docs/poc/trajet.html, objet DIRECTIONS).
 */
enum class Direction(
    val label: String,
) {
    ALLER("Aller"),
    RETOUR("Retour"),
}
