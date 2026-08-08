package fr.whitytoes.badgemoi.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fr.whitytoes.badgemoi.R
import fr.whitytoes.badgemoi.domain.Direction
import fr.whitytoes.badgemoi.domain.StartTrip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Action portée par les `PendingIntent` des deux blocs.
 *
 * Elle n'est **pas** déclarée au manifeste : un intent explicite (visant le receiver par
 * [ComponentName]) atteint sa cible sans le moindre `intent-filter`, donc la déclarer
 * n'apporterait rien et rendrait l'action résoluble **implicitement**, c'est-à-dire
 * atteignable par simple diffusion.
 *
 * Ce que cette absence ne fait **pas** : protéger l'accès. Le receiver est
 * `exported="true"` — contrainte du système, un receiver de widget non exporté ne reçoit
 * rien — si bien qu'une application tierce connaissant le nom de composant et cette
 * chaîne peut de toute façon déclencher [BadgeMoiWidgetProvider.onReceive]. Ne pas lire
 * ici une garantie de sécurité qui n'existe pas.
 *
 * Ce qui borne réellement le risque est la nature de l'action : démarrer un trajet, et
 * rien d'autre. La garde de [fr.whitytoes.badgemoi.domain.ActiveTripRepository.saveIfNoneInProgress]
 * empêche d'écraser un trajet en cours, aucune donnée n'est lue ni sortie de
 * l'appareil, et le pire cas reste un trajet parasite que l'utilisateur abandonne.
 * C'est le seul composant exporté de l'application, et la revue d'inclusion F-Droid
 * (#120) le regardera.
 */
private const val ACTION_START_TRIP = "fr.whitytoes.badgemoi.action.START_TRIP"

/** Sens du trajet à démarrer, transporté par son nom d'énumération. */
private const val EXTRA_DIRECTION = "fr.whitytoes.badgemoi.extra.DIRECTION"

/**
 * Widget d'écran d'accueil (cahier §3.6, lot 6).
 *
 * Les deux blocs de direction démarrent un trajet **sans ouvrir l'application** (#114) :
 * c'est tout l'intérêt du widget, on badge en enfourchant la roue. #115 lui donnera son
 * état « trajet en cours », #116 son rafraîchissement.
 *
 * **Pas de Glance, et c'est une décision** (écart 15 du §9, contre le §4.1 qui le
 * nommait) : `androidx.glance:glance` tire `androidx.work:work-runtime`, dont le
 * manifeste ajoute `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED` et
 * `FOREGROUND_SERVICE`. Quatre permissions pour un widget hors-ligne de deux boutons,
 * sur une application dont le §4.8 ne déclare que `VIBRATE`.
 *
 * Le prix payé en échange : la mise en page est du XML, pas du Compose, et la palette
 * est relue en ressources couleur (`values/colors.xml` et son versant `values-night/`).
 * Ce qui se partage avec l'application reste le **domaine**, et c'est là qu'est la valeur.
 */
class BadgeMoiWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context))
        }
    }

    /**
     * Démarre le trajet demandé, puis rend la main.
     *
     * `onReceive` dispose de dix secondes et n'est pas `suspend` : le travail part donc
     * derrière [goAsync], dont le `PendingResult` doit être terminé sur **tous** les
     * chemins — d'où le `finally`. Sans [goAsync], le processus peut être tué avant que
     * l'écriture aboutisse ; sans `finish()`, il est retenu vivant pour rien.
     *
     * Les dépendances se récupèrent par [EntryPointAccessors] sur l'`applicationContext` :
     * un receiver n'a ni cycle de vie ni `viewModelScope`, et c'est précisément la raison
     * technique qui a imposé d'extraire la règle de démarrage en [StartTrip].
     *
     * Aucun chemin ne lance `MainActivity` : si le widget ouvre l'application, il n'a
     * servi à rien.
     */
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_START_TRIP) {
            super.onReceive(context, intent)
            return
        }
        val direction = intent.getStringExtra(EXTRA_DIRECTION)?.let(::directionOrNull) ?: return
        val startTrip =
            EntryPointAccessors
                .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
                .startTrip()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                startTrip(direction)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Point d'accès Hilt du receiver — voir [onReceive]. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface WidgetEntryPoint {
        fun startTrip(): StartTrip
    }
}

/**
 * Construit la vue du widget et branche ses deux blocs.
 *
 * Le jour/nuit est résolu par le qualificatif de ressources `-night` **dans la
 * configuration du lanceur**, pas dans celle de l'application. C'est voulu — le widget
 * vit sur l'écran d'accueil et doit s'accorder à lui, non à la bascule de thème interne
 * à l'application (`ThemeMode`, §4.6), qu'un `RemoteViews` ne peut de toute façon pas
 * consulter.
 */
internal fun buildRemoteViews(context: Context): RemoteViews =
    RemoteViews(context.packageName, R.layout.badgemoi_widget).apply {
        setOnClickPendingIntent(R.id.widget_start_aller, startTripIntent(context, Direction.ALLER))
        setOnClickPendingIntent(R.id.widget_start_retour, startTripIntent(context, Direction.RETOUR))
    }

/**
 * `PendingIntent` explicite visant le receiver par son [ComponentName].
 *
 * `requestCode` vaut l'ordinal du sens, et ce n'est pas décoratif : deux `PendingIntent`
 * sont considérés identiques quand leurs intents le sont, et `Intent.filterEquals`
 * **ignore les extras**. Sans code distinct, le second appel recyclerait le premier et
 * les deux blocs démarreraient le même sens.
 *
 * `FLAG_IMMUTABLE` interdit à qui reçoit le `PendingIntent` d'en réécrire les extras ;
 * `FLAG_UPDATE_CURRENT` garde l'instance à jour au lieu d'en empiler une par
 * reconstruction de la vue.
 */
private fun startTripIntent(
    context: Context,
    direction: Direction,
): PendingIntent {
    val intent =
        Intent(ACTION_START_TRIP)
            .setComponent(ComponentName(context, BadgeMoiWidgetProvider::class.java))
            .putExtra(EXTRA_DIRECTION, direction.name)
    return PendingIntent.getBroadcast(
        context,
        direction.ordinal,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Sens correspondant à [name], ou `null` si l'extra est absent ou méconnaissable. */
private fun directionOrNull(name: String): Direction? = Direction.entries.firstOrNull { it.name == name }
