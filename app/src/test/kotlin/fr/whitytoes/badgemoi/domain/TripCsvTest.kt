@file:Suppress("MagicNumber") // Données de test : indices de jalons en clair.

package fr.whitytoes.badgemoi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

private val PARIS: ZoneId = ZoneId.of("Europe/Paris")

class TripCsvTest {
    private val departure: Instant = Instant.parse("2026-07-26T05:12:00Z") // 07h12 à Paris

    private fun trip(
        id: String = "t1",
        direction: Direction = Direction.ALLER,
    ) = Trip.start(id = id, direction = direction, departureAt = departure)

    private fun lines(csv: String) = csv.removePrefix("\uFEFF").split("\n")

    /** Un fichier vide serait indistinguable d'un export raté : l'en-tête doit rester. */
    @Test
    fun `une archive vide produit le seul en-tête`() {
        val csv = TripCsv.serialize(emptyList(), PARIS)

        assertEquals(listOf("\"direction\";\"date\";\"jalon\";\"heure\""), lines(csv))
    }

    /**
     * Excel lit le fichier en ANSI sans marque d'ordre des octets, et rend « Gare » ou
     * « Départ » en mojibake.
     */
    @Test
    fun `le fichier commence par une marque d'ordre des octets`() {
        assertTrue(TripCsv.serialize(emptyList(), PARIS).startsWith("\uFEFF"))
    }

    /** Forme longue du POC : une ligne par jalon, pas une par trajet. */
    @Test
    fun `un trajet donne une ligne par jalon`() {
        val csv = TripCsv.serialize(listOf(trip()), PARIS)

        assertEquals(1 + Routes.MILESTONE_COUNT, lines(csv).size)
    }

    @Test
    fun `chaque ligne porte le sens, la date, le jalon et son heure`() {
        val complet =
            (1 until Routes.MILESTONE_COUNT).fold(trip()) { trip, index ->
                trip.poseMilestone(index, departure.plusSeconds(index * 600L))
            }

        val csv = TripCsv.serialize(listOf(complet), PARIS)

        assertEquals("\"Aller\";\"2026-07-26\";\"Domicile\";\"07:12\"", lines(csv)[1])
        assertEquals("\"Aller\";\"2026-07-26\";\"Gare\";\"07:22\"", lines(csv)[2])
    }

    /**
     * Un jalon ignoré n'a pas d'horodatage. La cellule reste **vide** : le CSV est une
     * donnée, et un tiret s'y lirait comme une valeur.
     */
    @Test
    fun `un jalon ignoré donne une cellule vide`() {
        val csv = TripCsv.serialize(listOf(trip().skipMilestone(1)), PARIS)

        assertEquals("\"Aller\";\"2026-07-26\";\"Gare\";\"\"", lines(csv)[2])
    }

    @Test
    fun `un jalon jamais atteint donne aussi une cellule vide`() {
        val csv = TripCsv.serialize(listOf(trip()), PARIS)

        assertEquals("\"Aller\";\"2026-07-26\";\"Bureau\";\"\"", lines(csv).last())
    }

    /**
     * Le cas qui corrompt un CSV **en silence** : un point-virgule dans une cellule
     * décale toutes les colonnes suivantes, un guillemet non doublé casse la ligne.
     *
     * Éprouvé sur [TripCsv.escape] directement : aucune donnée du domaine ne contient
     * aujourd'hui ces caractères, donc aucun trajet ne pourrait déclencher le cas par
     * [TripCsv.serialize]. Le vérifier au travers d'un trajet reviendrait à réécrire la
     * règle dans le test et à la comparer à elle-même.
     */
    @Test
    fun `un point-virgule ou un guillemet est échappé, pas propagé`() {
        assertEquals("un guillemet est doublé", "\"a\"\"b\"", TripCsv.escape("a\"b"))
        assertEquals("un point-virgule reste dans sa cellule", "\"a;b\"", TripCsv.escape("a;b"))
        assertEquals("une cellule vide reste une cellule", "\"\"", TripCsv.escape(""))
        assertEquals("un saut de ligne est encadré", "\"a\nb\"", TripCsv.escape("a\nb"))
    }

    /** Toutes les cellules sont encadrées, y compris celles de l'en-tête. */
    @Test
    fun `l'en-tête est échappé comme les données`() {
        val header = lines(TripCsv.serialize(emptyList(), PARIS)).first()

        assertEquals(4, header.split(";").size)
        header.split(";").forEach { assertTrue(it, it.startsWith("\"") && it.endsWith("\"")) }
    }

    /** L'export porte sur toute l'archive, les deux sens confondus. */
    @Test
    fun `les deux sens sont exportés ensemble`() {
        val csv =
            TripCsv.serialize(
                listOf(trip(id = "a"), trip(id = "b", direction = Direction.RETOUR)),
                PARIS,
            )

        val sens = lines(csv).drop(1).map { it.split(";").first() }.distinct()
        assertEquals(listOf("\"Aller\"", "\"Retour\""), sens)
    }

    /**
     * Les dates du fichier sont en `AAAA-MM-JJ` : triables telles quelles dans un tableur,
     * et non ambiguës d'une locale à l'autre. L'écran, lui, garde `JJ/MM/AA`.
     */
    @Test
    fun `les dates du fichier sont au format ISO`() {
        val csv = TripCsv.serialize(listOf(trip()), PARIS)

        assertEquals("\"2026-07-26\"", lines(csv)[1].split(";")[1])
    }

    /** Le fuseau est celui de lecture : un départ à 05h12 UTC est un 26 juillet à Paris. */
    @Test
    fun `les heures sont lues dans le fuseau fourni`() {
        val csv = TripCsv.serialize(listOf(trip()), ZoneId.of("UTC"))

        assertEquals("\"05:12\"", lines(csv)[1].split(";")[3])
    }

    @Test
    fun `le nom de fichier porte la date du jour`() {
        val nom = TripCsv.fileName(Instant.parse("2026-07-26T22:30:00Z"), PARIS)

        // 22h30 UTC, soit le 27 à Paris : le fuseau compte aussi pour le nom.
        assertEquals("trajet-historique-2026-07-27.csv", nom)
    }
}
