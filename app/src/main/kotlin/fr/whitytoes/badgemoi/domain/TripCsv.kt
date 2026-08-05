package fr.whitytoes.badgemoi.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Sérialisation CSV de l'archive (cahier des charges §3.4), portée depuis l'`exportCsv`
 * du POC.
 *
 * **Fonction pure** : des trajets en entrée, une chaîne en sortie. Aucun `Intent`, aucun
 * flux, aucune dépendance Android — c'est ici que se logent les erreurs de format, et
 * c'est donc ici qu'il faut pouvoir tester.
 *
 * Le format reprend quatre décisions du POC, dont aucune n'est cosmétique :
 *
 * - séparateur **`;`** et non la virgule, seul séparateur qu'un tableur en locale
 *   française ouvre sans boîte de dialogue d'import ;
 * - **chaque cellule entre guillemets**, guillemets internes doublés — un libellé
 *   contenant un `;` corromprait sinon toutes les colonnes suivantes, en silence ;
 * - **BOM UTF-8** en tête, sans lequel Excel rend les accents en mojibake ;
 * - une ligne **par jalon et par trajet**, et non par trajet : c'est la forme longue,
 *   celle qu'un tableur croise et filtre.
 */
object TripCsv {
    private const val SEPARATOR = ";"
    private const val LINE_SEPARATOR = "\n"

    /**
     * Marque d'ordre des octets. Excel lit le fichier en ANSI sans elle, et les accents
     * ressortent en mojibake. Écrite en séquence d'échappement : le caractère lui-même
     * est invisible dans le source, donc impossible à relire ou à corriger sans outil.
     */
    private const val BOM = "\uFEFF"

    private val HEADER = listOf("direction", "date", "jalon", "heure")

    /**
     * Dates en `AAAA-MM-JJ` dans le fichier, là où l'écran affiche `JJ/MM/AA`.
     *
     * Ce sont deux publics : un humain qui lit, un tableur qui trie. La forme ISO se trie
     * telle quelle comme du texte et ne se lit pas différemment d'une locale à l'autre,
     * ce dont `JJ/MM/AA` est incapable. Le formateur est donc **volontairement distinct**
     * de celui de l'interface.
     */
    private val DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)

    private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    /**
     * Sérialise **toute** l'archive, les deux sens confondus — contrairement aux
     * statistiques, qui sont par sens.
     *
     * Une archive vide produit le seul en-tête : un fichier vide serait indistinguable
     * d'un export raté.
     */
    fun serialize(
        trips: List<Trip>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val date = DateFormatter.withZone(zone)
        val time = TimeFormatter.withZone(zone)

        val rows =
            buildList {
                add(HEADER)
                trips.forEach { trip ->
                    val tripDate = date.format(trip.departureAt ?: trip.createdAt)
                    trip.route.milestones.forEach { milestone ->
                        add(
                            listOf(
                                trip.direction.label,
                                tripDate,
                                milestone.label,
                                // Un jalon non posé donne une cellule **vide** : le CSV
                                // est une donnée, pas un affichage. Un tiret s'y lirait
                                // comme une valeur.
                                trip.times[milestone.index]?.let(time::format).orEmpty(),
                            ),
                        )
                    }
                }
            }

        return BOM + rows.joinToString(LINE_SEPARATOR) { row -> row.joinToString(SEPARATOR, transform = ::escape) }
    }

    /** Nom proposé au sélecteur : `trajet-historique-AAAA-MM-JJ.csv`, comme le POC. */
    fun fileName(
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = "trajet-historique-${DateFormatter.withZone(zone).format(now)}.csv"

    /**
     * Encadre une cellule et double ses guillemets internes.
     *
     * `internal` et non `private` : c'est **la** fonction où un CSV se corrompt en
     * silence, et aucune donnée du domaine ne contient aujourd'hui de guillemet ni de
     * point-virgule — un test passant par [serialize] ne pourrait donc pas l'éprouver. La
     * rendre visible au test vaut mieux qu'un test qui se contenterait de reproduire la
     * règle qu'il prétend vérifier.
     */
    internal fun escape(cell: String): String = "\"" + cell.replace("\"", "\"\"") + "\""
}
