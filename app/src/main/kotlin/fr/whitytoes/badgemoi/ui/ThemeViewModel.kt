package fr.whitytoes.badgemoi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.whitytoes.badgemoi.domain.SettingsRepository
import fr.whitytoes.badgemoi.domain.ThemeMode
import fr.whitytoes.badgemoi.ui.theme.toggled
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Préférence de thème de l'application, lue et écrite via le [SettingsRepository] du
 * lot 1.
 *
 * [themeMode] vaut `null` tant que la première valeur n'a pas été lue du DataStore.
 * L'appelant s'en sert pour ne rien afficher avant, ce qui évite un flash de thème
 * incorrect au lancement.
 */
@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val themeMode: StateFlow<ThemeMode?> =
            settingsRepository
                .observeThemeMode()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null,
                )

        /**
         * Bascule vers le contraire du thème actuellement affiché. Sans effet tant que
         * la préférence n'est pas lue, pour ne pas écrire par-dessus une valeur inconnue.
         */
        fun toggleTheme(systemInDarkTheme: Boolean) {
            val current = themeMode.value ?: return
            viewModelScope.launch {
                settingsRepository.setThemeMode(current.toggled(systemInDarkTheme))
            }
        }
    }
