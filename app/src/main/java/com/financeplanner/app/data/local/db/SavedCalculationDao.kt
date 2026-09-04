package com.financeplanner.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCalculationDao {
    @Query("SELECT * FROM saved_calculations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedCalculationEntity>>

    @Query("SELECT * FROM saved_calculations WHERE id = :id")
    suspend fun getById(id: Long): SavedCalculationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedCalculationEntity): Long

    @Delete
    suspend fun delete(entity: SavedCalculationEntity)
}
