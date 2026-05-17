package com.example.flora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BowelMovementDao {
    @Query("SELECT * FROM bowel_movements ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BowelMovement>>

    @Query("SELECT * FROM bowel_movements WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getForDay(startOfDay: Long, endOfDay: Long): Flow<List<BowelMovement>>

    @Insert
    suspend fun insert(bowelMovement: BowelMovement)

    @Delete
    suspend fun delete(bowelMovement: BowelMovement)
}
