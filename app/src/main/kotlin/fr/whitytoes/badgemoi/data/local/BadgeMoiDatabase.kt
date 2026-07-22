package fr.whitytoes.badgemoi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base Room locale. `exportSchema = false` tant qu'aucune migration n'est nécessaire
 * (première version) ; à activer avec un dossier de schémas dès la première migration.
 */
@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BadgeMoiDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
