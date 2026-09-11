package com.example.data.db

import com.example.data.model.GameType
import kotlinx.coroutines.flow.Flow

class ScoreRepository(private val scoreDao: ScoreDao) {
    fun getTopScores(categoryKey: String, gameTypeKey: String, difficultyKey: String, limit: Int = 10): Flow<List<ScoreRecord>> {
        return when (gameTypeKey) {
            GameType.SPEED_MATCH.key, GameType.TURTLE_SOUP.key, GameType.AVATAR_WHACK.key, GameType.STROOP_EFFECT.key, GameType.BLOCK_PUZZLE.key -> scoreDao.getTopScoresByScore(categoryKey, gameTypeKey, difficultyKey, limit)
            GameType.SUDOKU.key -> scoreDao.getTopScoresBySudoku(categoryKey, gameTypeKey, difficultyKey, limit)
            else -> scoreDao.getTopScoresByTime(categoryKey, gameTypeKey, difficultyKey, limit)
        }
    }

    suspend fun insertScore(score: ScoreRecord) {
        scoreDao.insertScore(score)
    }

    suspend fun clearCategoryScores(categoryKey: String, gameTypeKey: String, difficultyKey: String) {
        scoreDao.clearGameScores(categoryKey, gameTypeKey, difficultyKey)
    }
}
