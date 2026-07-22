package fr.whitytoes.badgemoi.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.whitytoes.badgemoi.data.local.BadgeMoiDatabase
import fr.whitytoes.badgemoi.data.local.TripDao
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "badgemoi")

/** Fournit les briques de persistance (Room, DataStore). */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): BadgeMoiDatabase =
        Room
            .databaseBuilder(context, BadgeMoiDatabase::class.java, "badgemoi.db")
            .build()

    @Provides
    fun provideTripDao(database: BadgeMoiDatabase): TripDao = database.tripDao()

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.dataStore
}
