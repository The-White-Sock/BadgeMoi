package fr.whitytoes.badgemoi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.ui.formatDuration
import fr.whitytoes.badgemoi.ui.formatTime
import fr.whitytoes.badgemoi.ui.theme.numeric
import fr.whitytoes.badgemoi.ui.trip.MilestoneRow
import fr.whitytoes.badgemoi.ui.trip.MilestoneStatus
import fr.whitytoes.badgemoi.ui.trip.isCorrectable
import java.time.Instant
import kotlin.time.Duration

private val RowShape = RoundedCornerShape(12.dp)
private val CurrentAccentWidth = 5.dp

/** Hauteur d'une ligne de jalon tranché ou à venir : le minimum d'une cible tactile. */
private val RowMinHeight = 48.dp

/** Hauteur de la ligne courante. Elle porte l'emphase, et un chronomètre qui court. */
private val CurrentRowMinHeight = 76.dp

/** Colonne de l'heure : `HH:mm` en monospace, plus une marge. */
private val TimeWidth = 56.dp

/** Colonne de la durée : jusqu'à `h:mm:ss`, plus « Ignoré ». */
private val DurationWidth = 80.dp

private val BadgeSize = 10.dp
private val CurrentBadgeSize = 16.dp

/** Fondu d'**extinction** du fond. L'allumage, lui, est instantané — voir [rowBackgroundColor]. */
private const val PRESS_FADE_MILLIS = 120

/**
 * Liste des jalons d'un trajet (cahier des charges §3.2).
 *
 * Elle n'appartient qu'à l'**écran actif**, parce que c'en est une liste d'actions : on
 * valide un jalon à la fois. Le récapitulatif, qui est une lecture, ne montre que des
 * tronçons (§9, écart 9).
 *
 * ## Ce qu'elle montre, et ce qu'elle tait
 *
 * Elle n'affiche **pas les cinq jalons** mais s'arrête au premier à venir (§9, écart 11).
 * Trois traitements nettement séparés :
 *
 * | Jalons | Traitement |
 * |---|---|
 * | tranchés — posés ou ignorés | petits et discrets, au-dessus |
 * | le jalon **courant** | emphase franche, ligne plus haute |
 * | à venir | **seul le prochain**, grisé et inerte |
 *
 * Les jalons plus lointains ne disent rien qu'on ait besoin de lire en roulant, et la
 * frise de progression du haut porte déjà la vue d'ensemble. Les taire ramène la ligne
 * courante — la seule sur laquelle on agit — au contact de la barre d'action.
 *
 * ## La grille
 *
 * Chaque ligne est une **rangée de tableau** : libellé extensible, puis deux cellules de
 * largeur fixe pour l'heure et la durée. Sans largeurs fixes, les valeurs se calaient sur
 * la longueur du libellé et sautaient d'une ligne à l'autre.
 *
 * @param onMilestoneClick ouvre la correction d'un jalon. Seuls les jalons **déjà
 *   tranchés** sont cliquables : corriger un jalon qu'on n'a pas encore atteint
 *   reviendrait à inventer un passage. Leur traitement discret est typographique — la
 *   cible tactile, elle, garde ses 48 dp et ses 8 dp d'écart.
 * @param runningSince temps écoulé depuis le dernier jalon posé, affiché sur la ligne du
 *   jalon **courant**. Volontairement une lambda : la lecture de l'état est différée
 *   jusqu'à cette seule ligne, qui est donc la seule à se recomposer chaque seconde.
 * @param selectedIndex jalon dont la feuille de correction est ouverte. Sa ligne reste
 *   allumée tant que la feuille est là.
 *
 * Le **défilement appartient à l'appelant**, d'où une simple [Column].
 */
@Composable
fun MilestoneList(
    rows: List<MilestoneRow>,
    modifier: Modifier = Modifier,
    onMilestoneClick: ((Int) -> Unit)? = null,
    runningSince: (() -> Duration?)? = null,
    selectedIndex: Int? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        rows.upToNextMilestone().forEach { row ->
            key(row.index) {
                MilestoneListRow(
                    row = row,
                    onClick =
                        if (row.status.isCorrectable) {
                            onMilestoneClick?.let { { it(row.index) } }
                        } else {
                            null
                        },
                    runningSince = runningSince,
                    selected = row.index == selectedIndex,
                )
            }
        }
    }
}

/**
 * Les jalons tranchés, le courant, et le prochain — rien au-delà.
 *
 * Les lignes précédant le jalon courant sont toutes tranchées par construction : couper
 * un cran après lui suffit. Un trajet **terminé** n'a plus de jalon courant ; on montre
 * alors tout, le temps que l'écran parte au récapitulatif.
 */
private fun List<MilestoneRow>.upToNextMilestone(): List<MilestoneRow> {
    val current = indexOfFirst { it.status == MilestoneStatus.CURRENT }
    return if (current < 0) this else take(current + 2)
}

@Composable
private fun MilestoneListRow(
    row: MilestoneRow,
    onClick: (() -> Unit)?,
    runningSince: (() -> Duration?)?,
    selected: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val current = row.status == MilestoneStatus.CURRENT
    val accent = colors.primary

    val background = rowBackgroundColor(current = current, selected = selected)

    val clickable =
        if (onClick != null) {
            // Ondulation Material telle quelle. Elle scintille en thème nuit — le style
            // *patterned* d'Android 12 y superpose du blanc à 55 % — et Compose ne donne
            // pas accès à `effectColor` pour l'éteindre. Décision tranchée au §9 du cahier
            // des charges : on l'accepte plutôt que de réimplémenter l'ondulation.
            Modifier.clickable(
                interactionSource = null,
                indication = ripple(color = colors.secondary),
                onClick = onClick,
            )
        } else {
            Modifier
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                // 4 dp de part et d'autre, soit 8 dp entre deux lignes : l'écart minimal
                // entre cibles tactiles distinctes (`docs/ergonomie.md` §4). Deux lignes
                // voisines ouvrent la correction de deux jalons différents, et le doigt
                // vise mal sur un board qui vibre.
                .padding(horizontal = 8.dp, vertical = 4.dp)
                // Découpe avant le fond : le coin arrondi vaut aussi pour l'aplat d'appui.
                .clip(RowShape)
                .background(background)
                // Barre d'accent du POC (`border-left:5px solid var(--amber)`). C'est elle
                // qui porte le « vous êtes ici » : un aplat seul se confond avec l'aplat
                // d'appui et donne au jalon courant — le seul non modifiable — l'air d'être
                // la ligne cliquable.
                .drawBehind {
                    if (current) {
                        drawRect(color = accent, size = Size(CurrentAccentWidth.toPx(), size.height))
                    }
                }.then(clickable)
                .heightIn(min = if (current) CurrentRowMinHeight else RowMinHeight)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(status = row.status)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = row.label,
            style = labelStyle(row.status),
            color = labelColor(row.status),
            modifier = Modifier.weight(1f),
        )
        MilestoneTime(at = row.at, status = row.status)
        TrailingValue(row = row, runningSince = runningSince)
    }
}

/** Typographie du libellé : le courant domine, le prochain s'efface. */
@Composable
private fun labelStyle(status: MilestoneStatus): TextStyle =
    when (status) {
        MilestoneStatus.CURRENT -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.bodyMedium
    }

/**
 * Fond d'une ligne de jalon.
 *
 * Il ne porte que l'état **persistant** : l'ambre du jalon courant, et le teal de la ligne
 * dont la feuille de correction est ouverte. Le retour d'appui, lui, est transitoire et
 * revient à l'ondulation Material posée sur le `clickable`.
 *
 * L'allumage est instantané, l'extinction en fondu : un fondu d'entrée retarderait
 * l'emphase de tout son temps de montée, ce qui se ressent comme une latence, alors qu'à
 * l'extinction il évite une coupure sèche.
 */
@Composable
private fun rowBackgroundColor(
    current: Boolean,
    selected: Boolean,
): Color {
    val colors = MaterialTheme.colorScheme

    val background by
        animateColorAsState(
            targetValue =
                when {
                    selected -> colors.secondaryContainer
                    // Ambre du POC (`.mrow.current{background:var(--amber-soft)}`), et non
                    // le teal : celui-ci est la couleur des jalons **posés**.
                    current -> colors.primaryContainer
                    else -> Color.Transparent
                },
            animationSpec = if (selected) snap() else tween(durationMillis = PRESS_FADE_MILLIS),
            label = "fond de la ligne de jalon",
        )

    return background
}

/**
 * Marqueur d'état. Le jeu de 24 icônes vectorielles du cahier §1.5 arrive au lot 7 ;
 * d'ici là, l'état est porté par la forme et la couleur, et le sens du jalon par son
 * libellé. [MilestoneRow.icon] est déjà transporté pour que ce remplacement soit local.
 */
@Composable
private fun StatusBadge(status: MilestoneStatus) {
    val colors = MaterialTheme.colorScheme
    // Répartition du POC : teal pour ce qui est fait (`.mrow.done`), ambre pour le jalon
    // courant (`.mrow.current`).
    val color =
        when (status) {
            MilestoneStatus.POSED -> colors.secondary
            MilestoneStatus.SKIPPED -> colors.outline
            MilestoneStatus.CURRENT -> colors.primary
            MilestoneStatus.PENDING -> colors.outlineVariant
        }

    Box(
        modifier =
            Modifier
                .size(if (status == MilestoneStatus.CURRENT) CurrentBadgeSize else BadgeSize)
                .clip(CircleShape)
                .background(color),
    )
}

/**
 * Heure de passage, première des deux colonnes chiffrées.
 *
 * Elle comble la ligne du départ, qui n'a pas de tronçon avant elle donc pas de durée, et
 * distingue les deux natures que la ligne porte : un jalon est un instant, la durée qu'il
 * affiche appartient au tronçon qui le précède.
 *
 * Largeur fixe, et la cellule reste posée même sans heure : c'est ce qui aligne la colonne
 * des durées d'une ligne à l'autre.
 */
@Composable
private fun MilestoneTime(
    at: Instant?,
    status: MilestoneStatus,
) {
    val colors = MaterialTheme.colorScheme

    Text(
        text = at?.let { formatTime(it) }.orEmpty(),
        style = MaterialTheme.typography.bodyMedium.numeric(FontWeight.Medium),
        color = if (status == MilestoneStatus.PENDING) colors.outline else colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(TimeWidth),
    )
}

@Composable
private fun TrailingValue(
    row: MilestoneRow,
    runningSince: (() -> Duration?)?,
) {
    val colors = MaterialTheme.colorScheme
    val current = row.status == MilestoneStatus.CURRENT
    // Le chronomètre en cours vit sur la ligne du jalon à valider, là où le regard se
    // pose, plutôt que dans le bandeau où il doublait la valeur « Écoulé ».
    val running = if (current) runningSince?.invoke() else null

    val cell = Modifier.width(DurationWidth)

    when {
        running != null -> {
            val value = formatDuration(running)
            // Hors contexte, un chronomètre nu ne dit pas ce qu'il compte : la
            // description sonore reprend la phrase que portait le bandeau.
            val description = stringResource(R.string.trip_since_last_milestone, value)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.numeric(),
                color = colors.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = cell.semantics { contentDescription = description },
            )
        }

        // « Ignoré » plutôt qu'une durée vide : le jalon a été traité, pas oublié.
        row.status == MilestoneStatus.SKIPPED ->
            Text(
                text = stringResource(R.string.milestone_skipped),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = cell,
            )

        row.sincePrevious != null ->
            Text(
                text = formatDuration(row.sincePrevious),
                style = MaterialTheme.typography.bodyMedium.numeric(),
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = cell,
            )

        else ->
            Text(
                text = stringResource(R.string.milestone_no_value),
                style = MaterialTheme.typography.bodyMedium.numeric(),
                color =
                    when (row.status) {
                        MilestoneStatus.CURRENT -> colors.onPrimaryContainer
                        MilestoneStatus.PENDING -> colors.outline
                        else -> colors.onSurfaceVariant
                    },
                textAlign = TextAlign.Center,
                modifier = cell,
            )
    }
}

/** Encre du libellé : le prochain jalon est **inerte**, sa couleur doit le dire. */
@Composable
private fun labelColor(status: MilestoneStatus) =
    when (status) {
        MilestoneStatus.CURRENT -> MaterialTheme.colorScheme.onPrimaryContainer
        MilestoneStatus.PENDING -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
