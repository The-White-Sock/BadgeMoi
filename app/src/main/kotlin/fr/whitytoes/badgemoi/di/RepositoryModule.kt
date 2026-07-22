package fr.whitytoes.badgemoi.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.whitytoes.badgemoi.data.local.DataStoreActiveTripRepository
import fr.whitytoes.badgemoi.data.local.DataStoreSettingsRepository
import fr.whitytoes.badgemoi.data.local.RoomTripArchiveRepository
import fr.whitytoes.badgemoi.domain.ActiveTripRepository
import fr.whitytoes.badgemoi.domain.SettingsRepository
import fr.whitytoes.badgemoi.domain.TripArchiveRepository
import javax.inject.Singleton

/** Lie les interfaces de repository du domaine à leurs implémentations de la couche data. */
@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindTripArchiveRepository(impl: RoomTripArchiveRepository): TripArchiveRepository

    @Binds
    @Singleton
    fun bindActiveTripRepository(impl: DataStoreActiveTripRepository): ActiveTripRepository

    @Binds
    @Singleton
    fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
