@file:Suppress("MagicNumber") // Indices de jalons/tronçons : les littéraux 0..4 sont des positions.

package fr.whitytoes.badgemoi.domain

/** Icône monochrome associée à un jalon (cahier §1.5, 24 icônes du POC). */
enum class MilestoneIcon { HOME, STATION, TRAIN, DOOR }

/** Libellé d'un tronçon nommé reliant deux jalons. */
enum class SegmentLabel(
    val label: String,
) {
    RIDE("Ride"),
    ATTENTE("Attente"),
    TRAIN("Train"),
}

/** Définition immuable d'un jalon dans un sens donné. */
data class MilestoneDefinition(
    val index: Int,
    val label: String,
    val icon: MilestoneIcon,
)

/** Définition d'un tronçon = intervalle entre deux jalons, repérés par leur index. */
data class SegmentDefinition(
    val label: SegmentLabel,
    val fromIndex: Int,
    val toIndex: Int,
)

/** Parcours complet (jalons + tronçons) pour un sens. */
data class RouteDefinition(
    val direction: Direction,
    val milestones: List<MilestoneDefinition>,
    val segments: List<SegmentDefinition>,
)

/**
 * Catalogue figé des parcours, porté à l'identique depuis le POC
 * (docs/poc/trajet.html, objet DIRECTIONS) : 5 jalons et 4 tronçons par sens.
 */
object Routes {
    /** Nombre de jalons par trajet, identique pour les deux sens. */
    const val MILESTONE_COUNT = 5

    private val segments =
        listOf(
            SegmentDefinition(SegmentLabel.RIDE, fromIndex = 0, toIndex = 1),
            SegmentDefinition(SegmentLabel.ATTENTE, fromIndex = 1, toIndex = 2),
            SegmentDefinition(SegmentLabel.TRAIN, fromIndex = 2, toIndex = 3),
            SegmentDefinition(SegmentLabel.RIDE, fromIndex = 3, toIndex = 4),
        )

    private val allerMilestones =
        listOf(
            MilestoneDefinition(0, "Domicile", MilestoneIcon.HOME),
            MilestoneDefinition(1, "Gare", MilestoneIcon.STATION),
            MilestoneDefinition(2, "Départ", MilestoneIcon.TRAIN),
            MilestoneDefinition(3, "Gare", MilestoneIcon.STATION),
            MilestoneDefinition(4, "Bureau", MilestoneIcon.DOOR),
        )

    private val retourMilestones =
        listOf(
            MilestoneDefinition(0, "Bureau", MilestoneIcon.DOOR),
            MilestoneDefinition(1, "Gare", MilestoneIcon.STATION),
            MilestoneDefinition(2, "Départ", MilestoneIcon.TRAIN),
            MilestoneDefinition(3, "Gare", MilestoneIcon.STATION),
            MilestoneDefinition(4, "Domicile", MilestoneIcon.HOME),
        )

    private val aller = RouteDefinition(Direction.ALLER, allerMilestones, segments)

    private val retour = RouteDefinition(Direction.RETOUR, retourMilestones, segments)

    fun forDirection(direction: Direction): RouteDefinition =
        when (direction) {
            Direction.ALLER -> aller
            Direction.RETOUR -> retour
        }
}
