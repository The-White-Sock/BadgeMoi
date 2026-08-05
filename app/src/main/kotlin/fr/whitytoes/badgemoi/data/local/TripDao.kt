package fr.whitytoes.badgemoi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Purge d'un seul sens : l'archive est par sens de bout en bout côté écran.
     *
     * Une seule requête, plutôt que relire les trajets pour les supprimer un à un. La
     * colonne n'est pas indexée — inutile sur une table de quelques dizaines de lignes.
     */
    @Query("DELETE FROM trips WHERE direction = :direction")
    suspend fun clear(direction: String)
}
