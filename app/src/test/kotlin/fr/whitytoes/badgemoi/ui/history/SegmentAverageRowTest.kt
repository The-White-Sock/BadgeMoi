@file:Suppress("MagicNumber") // Données de test : effectifs et durées en clair.

package fr.whitytoes.badgemoi.ui.history

import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.DirectionStatistics
import fr.whitytoes.badgemoi.domain.Routes
import fr.whitytoes.badgemoi.domain.SegmentAverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class SegmentAverageRowTest {
    private fun statistics(
        direction: Direction = Direction.ALLER,
        averages: List<Duration?> = listOf(9.minutes, 4.minutes, 11.minutes, 4.minutes),
        samples: List<Int> = listOf(10, 10, 10, 10),
    ): DirectionStatistics {
        val route = Routes.forDirection(direction)
        return DirectionStatistics(
            direction = direction,
            tripCount = 10,
            totalAverage = 28.minutes,
            segmentAverages =
                route.segments.mapIndexed { index, segment ->
                    SegmentAverage(segment, averages[index], samples[index])
                },
        )
    }

    @Test
    fun `chaque tronçon du parcours donne une ligne`() {
        val rows = statistics().segmentRows(sampleLabels = List(4) { "" })

        assertEquals(4, rows.size)
        assertEquals(listOf("Ride", "Attente", "Train", "Ride"), rows.map { it.label })
    }

    /**
     * Le pari de #49 : [fr.whitytoes.badgemoi.ui.summary.SegmentRow] porte une durée
     * **reçue**, non calculée depuis un trajet. Le récapitulatif y met une durée mesurée,
     * l'historique une moyenne, et le composant les rend l'un comme l'autre.
     */
    @Test
    fun `la durée de la ligne est la moyenne du tronçon`() {
        val rows = statistics().segmentRows(sampleLabels = List(4) { "" })

        assertEquals(9.minutes, rows[0].duration)
        assertEquals(11.minutes, rows[2].duration)
    }

    /** Un tronçon sans aucune mesure n'a pas de moyenne : la ligne le dira « Non mesuré ». */
    @Test
    fun `un tronçon jamais mesuré n'a pas de durée`() {
        val rows =
            statistics(averages = listOf(9.minutes, null, 11.minutes, 4.minutes))
                .segmentRows(sampleLabels = List(4) { "" })

        assertNull(rows[1].duration)
    }

    /**
     * Le point de vigilance de l'issue : une moyenne sur trois trajets parmi dix ne se
     * lit pas comme une moyenne sur dix.
     */
    @Test
    fun `le nombre de mesures accompagne chaque ligne`() {
        val rows =
            statistics(samples = listOf(10, 3, 10, 10))
                .segmentRows(sampleLabels = listOf("10 mesures", "3 mesures", "10 mesures", "10 mesures"))

        assertEquals("3 mesures", rows[1].detail)
        assertEquals("10 mesures", rows[0].detail)
    }

    @Test
    fun `les extrémités suivent le sens des statistiques`() {
        val rows = statistics(direction = Direction.RETOUR).segmentRows(sampleLabels = List(4) { "" })

        assertEquals("Bureau", rows[0].fromLabel)
        assertEquals("Domicile", rows[3].toLabel)
    }
}
