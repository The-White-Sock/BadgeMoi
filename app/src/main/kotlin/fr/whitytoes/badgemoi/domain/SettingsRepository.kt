package fr.whitytoes.badgemoi.domain

import kotlinx.coroutines.flow.Flow

/** Préférences persistées de l'application (thème). */
interface SettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
