package fr.whitytoes.badgemoi.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import fr.whitytoes.badgemoi.R

/**
 * Widget d'écran d'accueil (cahier §3.6, lot 6).
 *
 * Ce socle **n'expose aucune fonctionnalité** : il pose le composant, sa mise en page et
 * sa palette. Les deux blocs de direction sont inertes — #114 leur donne leur action,
 * #115 l'état « trajet en cours », #116 le rafraîchissement.
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
}

/**
 * Construit la vue du widget.
 *
 * Rien n'est encore branché : le layout se suffit à lui-même, et le jour/nuit est résolu
 * par le qualificatif de ressources `-night` **dans la configuration du lanceur**, pas
 * dans celle de l'application. C'est voulu — le widget vit sur l'écran d'accueil et doit
 * s'accorder à lui, non à la bascule de thème interne à l'application (`ThemeMode`,
 * §4.6), qu'un `RemoteViews` ne peut de toute façon pas consulter.
 */
internal fun buildRemoteViews(context: Context): RemoteViews =
    RemoteViews(context.packageName, R.layout.badgemoi_widget)
