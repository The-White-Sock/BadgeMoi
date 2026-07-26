package fr.whitytoes.badgemoi.ui.components

import androidx.annotation.StringRes
import fr.whitytoes.badgemoi.R

/**
 * Onglets de premier niveau du bandeau haut (cahier des charges §3.1). [AppTopBar] les
 * affiche en parcourant cette énumération : en ajouter un ne change aucune signature.
 */
enum class TopLevelDestination(
    @param:StringRes val labelRes: Int,
) {
    TRIP(R.string.destination_trip),
    HISTORY(R.string.destination_history),
}
