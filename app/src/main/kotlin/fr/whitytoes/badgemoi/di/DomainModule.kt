package fr.whitytoes.badgemoi.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.StartTrip
import java.time.Clock
import java.util.UUID
import javax.inject.Singleton

/**
 * Construit les cas d'usage du domaine.
 *
 * Ils ne s'annotent pas eux-mêmes : `domain/` est du Kotlin/JVM pur, sans Dagger ni
 * `javax.inject`, ce qui rendra l'extraction du module `:domain` (#123) mécanique.
 * C'est donc ici que se règlent leurs dépendances — y compris la fabrique
 * d'identifiants, `java.util.UUID` n'étant pas une dépendance tolérée du domaine.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideStartTrip(
        activeTripRepository: ActiveTripRepository,
        clock: Clock,
    ): StartTrip =
        StartTrip(
            activeTripRepository = activeTripRepository,
            clock = clock,
            newTripId = { UUID.randomUUID().toString() },
        )
}
