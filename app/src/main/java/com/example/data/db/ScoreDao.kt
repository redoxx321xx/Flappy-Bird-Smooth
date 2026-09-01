package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores ORDER BY score DESC, timestamp DESC LIMIT :limit")
    fun getTopScores(limit: Int = 10): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores ORDER BY timestamp DESC LIMIT 20")
    fun getRecentScores(): Flow<List<ScoreEntity>>

    @Query("SELECT MAX(score) FROM scores")
    fun getHighScore(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM scores")
    fun getTotalGames(): Flow<Int>

    @Query("SELECT SUM(coins) FROM scores")
    fun getTotalCoins(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ScoreEntity): Long

    @Query("DELETE FROM scores")
    suspend fun clearAll()
}
