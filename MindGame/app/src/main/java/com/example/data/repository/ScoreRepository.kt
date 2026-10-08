package com.example.data.db

import com.example.data.model.GameType
import kotlinx.coroutines.flow.Flow

class ScoreRepository(private val scoreDao: ScoreDao) {
    fun getTopScores(categoryKey: String, gameTypeKey: String, difficultyKey: String, limit: Int = 10): Flow<List<ScoreRecord>> {
        val gameType = GameType.fromKey(gameTypeKey)
        return when (gameType.rankingType) {
            com.example.data.model.RankingType.SCORE_DESC -> scoreDao.getTopScoresByScore(categoryKey, gameTypeKey, difficultyKey, limit)
            com.example.data.model.RankingType.TIME_ASC -> {
                if (gameType.isSudoku) {
                    scoreDao.getTopScoresBySudoku(categoryKey, gameTypeKey, difficultyKey, limit)
                } else {
                    scoreDao.getTopScoresByTime(categoryKey, gameTypeKey, difficultyKey, limit)
                }
            }
        }
    }

    suspend fun insertScore(score: ScoreRecord) {
        scoreDao.insertScore(score)
    }

    suspend fun clearCategoryScores(categoryKey: String, gameTypeKey: String, difficultyKey: String) {
        scoreDao.clearGameScores(categoryKey, gameTypeKey, difficultyKey)
    }
}
