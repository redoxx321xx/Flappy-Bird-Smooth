package com.example.data.db

import kotlinx.coroutines.flow.Flow

class ScoreRepository(private val scoreDao: ScoreDao) {
    val topScores: Flow<List<ScoreEntity>> = scoreDao.getTopScores(10)
    val recentScores: Flow<List<ScoreEntity>> = scoreDao.getRecentScores()
    val highScore: Flow<Int?> = scoreDao.getHighScore()
    val totalGames: Flow<Int> = scoreDao.getTotalGames()
    val totalCoins: Flow<Int?> = scoreDao.getTotalCoins()

    suspend fun saveScore(score: Int, coins: Int, birdSkin: String, theme: String): Long {
        return scoreDao.insertScore(
            ScoreEntity(
                score = score,
                coins = coins,
                birdSkin = birdSkin,
                theme = theme
            )
        )
    }

    suspend fun resetScores() {
        scoreDao.clearAll()
    }
}
