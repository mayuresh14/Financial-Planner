package com.financeplanner.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedInvestmentDao {
    @Query("SELECT * FROM saved_investments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedInvestmentEntity>>

    @Query("SELECT * FROM saved_investments WHERE id = :id")
    suspend fun getById(id: Long): SavedInvestmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedInvestmentEntity): Long

    @Update
    suspend fun update(entity: SavedInvestmentEntity)

    @Delete
    suspend fun delete(entity: SavedInvestmentEntity)

    @Query("DELETE FROM saved_investments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
