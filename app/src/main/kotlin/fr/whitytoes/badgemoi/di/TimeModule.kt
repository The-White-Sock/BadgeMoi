package fr.whitytoes.badgemoi.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Source de temps de l'application. Injectée plutôt qu'appelée en dur via
 * `Instant.now()` : l'horodatage du départ d'un trajet devient ainsi vérifiable dans
 * les tests, avec une horloge figée.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
