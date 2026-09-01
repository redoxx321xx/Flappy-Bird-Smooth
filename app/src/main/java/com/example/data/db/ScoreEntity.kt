package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val score: Int,
    val coins: Int,
    val birdSkin: String,
    val theme: String,
    val timestamp: Long = System.currentTimeMillis()
)
