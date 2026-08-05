package fr.whitytoes.badgemoi.domain

import java.text.Normalizer
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
 * Le format reprend trois décisions du POC, dont aucune n'est cosmétique :
 *
 * - séparateur **`;`** et non la virgule, seul séparateur qu'un tableur en locale
 *   française ouvre sans boîte de dialogue d'import ;
 * - **chaque cellule entre guillemets**, guillemets internes doublés — un libellé
 *   contenant un `;` corromprait sinon toutes les colonnes suivantes, en silence ;
 * - une ligne **par jalon et par trajet**, et non par trajet : c'est la forme longue,
 *   celle qu'un tableur croise et filtre.
 *
 * S'y ajoute une quatrième décision, qui **remplace** le BOM UTF-8 du POC : le fichier est
 * entièrement **ASCII** (voir [fold]). Le POC écrivait cette marque d'ordre des octets pour
 * qu'Excel n'affiche pas « Départ » en mojibake ; à l'usage, Google Sheets l'ignore, décode
 * le fichier en Latin-1, et affiche donc la marque elle-même dans la première cellule
 * (« ï»¿ ») **en plus** de manquer l'accent. Or cet accent était le seul octet non ASCII de
 * tout l'export : le retirer à la source règle les deux symptômes d'un coup. Un fichier
 * ASCII se lit à l'identique en UTF-8, en Latin-1 et en Windows-1252 — il n'y a plus rien
 * à deviner pour le lecteur, donc plus rien à deviner de travers.
 */
object TripCsv {
    private const val SEPARATOR = ";"
    private const val LINE_SEPARATOR = "\n"

    /** Signes diacritiques isolés par la décomposition NFD : accents, cédilles, trémas. */
    private val Diacritics = Regex("\\p{Mn}+")

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

        return rows.joinToString(LINE_SEPARATOR) { row ->
            row.joinToString(SEPARATOR) { cell -> escape(fold(cell)) }
        }
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

    /**
     * Retire les accents d'une cellule : « Départ » devient « Depart ».
     *
     * Le procédé est la décomposition **NFD**, qui sépare une lettre accentuée en lettre
     * de base + signe diacritique, suivie de la suppression de ces signes. On ne
     * transcrit pas caractère par caractère : une table d'équivalences serait à compléter
     * à chaque libellé nouveau, là où la décomposition vaut pour tout l'alphabet latin.
     *
     * C'est une perte assumée, et elle ne porte que sur le fichier : l'écran continue
     * d'afficher « Départ ». L'export est une donnée destinée à un tableur, pas une
     * copie de l'interface — il a d'ailleurs déjà son propre vocabulaire, l'en-tête étant
     * en minuscules techniques.
     *
     * Limite connue : un caractère latin sans forme décomposée (« œ », « ß ») traverserait
     * ce repli intact et rouvrirait la question de l'encodage. Aucun libellé n'en contient
     * aujourd'hui, et ils sont des constantes de [Routes] — pas une saisie utilisateur.
     */
    internal fun fold(cell: String): String = Normalizer.normalize(cell, Normalizer.Form.NFD).replace(Diacritics, "")
}
