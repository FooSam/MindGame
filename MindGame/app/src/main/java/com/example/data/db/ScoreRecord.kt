package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_records")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val categoryKey: String,
    val gameTypeKey: String = "FOCUS_TEST",
    val difficultyKey: String,
    val timeMillis: Long = 0L,
    val score: Int = 0,
    val wrongCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
