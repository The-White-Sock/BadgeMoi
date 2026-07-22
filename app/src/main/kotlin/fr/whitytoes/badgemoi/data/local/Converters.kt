package fr.whitytoes.badgemoi.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Convertisseurs Room pour les listes stockées dans une colonne texte (JSON). */
class Converters {
    @TypeConverter
    fun timesToJson(value: List<Long?>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToTimes(value: String): List<Long?> = Json.decodeFromString(value)

    @TypeConverter
    fun skippedToJson(value: List<Boolean>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToSkipped(value: String): List<Boolean> = Json.decodeFromString(value)
}
