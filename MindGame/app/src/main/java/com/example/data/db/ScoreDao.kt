package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM score_records WHERE (:categoryKey = '' OR categoryKey = :categoryKey) AND gameTypeKey = :gameTypeKey AND difficultyKey = :difficultyKey ORDER BY timeMillis ASC, id DESC LIMIT :limit")
    fun getTopScoresByTime(categoryKey: String, gameTypeKey: String, difficultyKey: String, limit: Int = 10): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM score_records WHERE (:categoryKey = '' OR categoryKey = :categoryKey) AND gameTypeKey = :gameTypeKey AND difficultyKey = :difficultyKey ORDER BY wrongCount ASC, timeMillis ASC, id DESC LIMIT :limit")
    fun getTopScoresBySudoku(categoryKey: String, gameTypeKey: String, difficultyKey: String, limit: Int = 10): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM score_records WHERE (:categoryKey = '' OR categoryKey = :categoryKey) AND gameTypeKey = :gameTypeKey AND difficultyKey = :difficultyKey ORDER BY score DESC, wrongCount ASC, id DESC LIMIT :limit")
    fun getTopScoresByScore(categoryKey: String, gameTypeKey: String, difficultyKey: String, limit: Int = 10): Flow<List<ScoreRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ScoreRecord)

    @Query("DELETE FROM score_records WHERE (:categoryKey = '' OR categoryKey = :categoryKey) AND gameTypeKey = :gameTypeKey AND difficultyKey = :difficultyKey")
    suspend fun clearGameScores(categoryKey: String, gameTypeKey: String, difficultyKey: String)
}
