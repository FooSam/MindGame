package com.example

import com.example.data.db.ScoreRecord
import com.example.data.model.GameType
import com.example.data.model.GlobalScoreEntry
import com.example.data.model.RankingType
import com.example.data.model.getGlobalScoreComparator
import com.example.data.model.getScoreRecordComparator
import com.example.data.model.isBetterGlobalScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 全域排行榜排序規則與一致性自動化測試
 * 鐵律驗證：
 * 1. 分數型：分數最高排第 1，次高排第 2；同分時，以時間早 (timestamp 早 / 數值較小) 的排在上面。
 * 2. 完成時間型：時間最短最少排第 1，第 2 少排第 2；同時間時，以時間早 (timestamp 早 / 數值較小) 的排在上面。
 * 3. 數獨型：錯誤數少排前，同錯誤數比時間短，同時間以時間早者優先。
 * 4. 所有 GameType 枚舉皆必須具備明確且正確的 rankingType 屬性。
 */
class RankingConsistencyTest {

    @Test
    fun testAllGameTypesHaveExplicitRankingType() {
        for (game in GameType.entries) {
            assertNotNull("${game.key} 必須擁有 rankingType", game.rankingType)
            when (game) {
                GameType.FOCUS_TEST,
                GameType.FOCUS_TRAIN,
                GameType.SUDOKU,
                GameType.CAT_SUDOKU,
                GameType.GLASS_PUZZLE_CUBE,
                GameType.LASER_MAZE -> {
                    assertEquals("${game.key} 應為 TIME_ASC", RankingType.TIME_ASC, game.rankingType)
                }
                GameType.SPEED_MATCH,
                GameType.TURTLE_SOUP,
                GameType.AVATAR_WHACK,
                GameType.STROOP_EFFECT,
                GameType.BLOCK_PUZZLE,
                GameType.FRUIT_MASTER,
                GameType.PINBALL_FLIPPER,
                GameType.NIGHT_MARKET_PINBALL,
                GameType.BRAIN_IN_TROUBLE -> {
                    assertEquals("${game.key} 應為 SCORE_DESC", RankingType.SCORE_DESC, game.rankingType)
                }
            }
        }
    }

    @Test
    fun testScoreBasedRankingOrderWithTieBreaker() {
        val game = GameType.BRAIN_IN_TROUBLE
        assertEquals(RankingType.SCORE_DESC, game.rankingType)

        val recordA = ScoreRecord(
            id = 1L,
            playerName = "PlayerA",
            categoryKey = "TEST",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            score = 1500,
            timestamp = 1000L
        )
        val recordB = ScoreRecord(
            id = 2L,
            playerName = "PlayerB",
            categoryKey = "TEST",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            score = 2500,
            timestamp = 2000L
        )
        // 與 PlayerB 同分，但時間更早
        val recordC = ScoreRecord(
            id = 3L,
            playerName = "PlayerC",
            categoryKey = "TEST",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            score = 2500,
            timestamp = 1500L
        )

        val list = listOf(recordA, recordB, recordC)
        val sorted = list.sortedWith(game.getScoreRecordComparator())

        // 預期排名：
        // 第 1 名：PlayerC (2500 分，時間 1500L 較早)
        // 第 2 名：PlayerB (2500 分，時間 2000L 較晚)
        // 第 3 名：PlayerA (1500 分)
        assertEquals("第 1 名應為同分但時間更早的 PlayerC", "PlayerC", sorted[0].playerName)
        assertEquals(2500, sorted[0].score)

        assertEquals("第 2 名應為同分但時間較晚的 PlayerB", "PlayerB", sorted[1].playerName)
        assertEquals(2500, sorted[1].score)

        assertEquals("第 3 名應為分數最低的 PlayerA", "PlayerA", sorted[2].playerName)
        assertEquals(1500, sorted[2].score)
    }

    @Test
    fun testTimeBasedRankingOrderWithTieBreaker() {
        val game = GameType.FOCUS_TEST
        assertEquals(RankingType.TIME_ASC, game.rankingType)

        val recordA = ScoreRecord(
            id = 1L,
            playerName = "Slow",
            categoryKey = "TEST",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            timeMillis = 50000L,
            timestamp = 1000L
        )
        val recordB = ScoreRecord(
            id = 2L,
            playerName = "FastLater",
            categoryKey = "TEST",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            timeMillis = 28000L,
            timestamp = 2000L
        )
        // 與 FastLater 花費相同時間，但時間更早完成
        val recordC = ScoreRecord(
            id = 3L,
            playerName = "FastEarlier",
            categoryKey = "TEST",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            timeMillis = 28000L,
            timestamp = 1500L
        )

        val list = listOf(recordA, recordB, recordC)
        val sorted = list.sortedWith(game.getScoreRecordComparator())

        // 預期排名：
        // 第 1 名：FastEarlier (28000ms，時間 1500L 較早)
        // 第 2 名：FastLater (28000ms，時間 2000L 較晚)
        // 第 3 名：Slow (50000ms)
        assertEquals("第 1 名應為耗時相同但紀錄時間更早的 FastEarlier", "FastEarlier", sorted[0].playerName)
        assertEquals(28000L, sorted[0].timeMillis)

        assertEquals("第 2 名應為耗時相同但紀錄時間較晚的 FastLater", "FastLater", sorted[1].playerName)
        assertEquals(28000L, sorted[1].timeMillis)

        assertEquals("第 3 名應為耗時最長的 Slow", "Slow", sorted[2].playerName)
        assertEquals(50000L, sorted[2].timeMillis)
    }

    @Test
    fun testSudokuRankingOrder() {
        val game = GameType.SUDOKU
        assertTrue(game.isSudoku)

        val recordA = ScoreRecord(
            id = 1L,
            playerName = "ZeroMistakeLater",
            categoryKey = "DEDUCTION",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            wrongCount = 0,
            timeMillis = 40000L,
            timestamp = 2000L
        )
        val recordB = ScoreRecord(
            id = 2L,
            playerName = "ZeroMistakeEarlier",
            categoryKey = "DEDUCTION",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            wrongCount = 0,
            timeMillis = 40000L,
            timestamp = 1000L
        )
        val recordC = ScoreRecord(
            id = 3L,
            playerName = "OneMistakeFaster",
            categoryKey = "DEDUCTION",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            wrongCount = 1,
            timeMillis = 20000L,
            timestamp = 500L
        )

        val list = listOf(recordA, recordB, recordC)
        val sorted = list.sortedWith(game.getScoreRecordComparator())

        // 預期排名：
        // 第 1 名：ZeroMistakeEarlier (0錯, 40s, 1000L)
        // 第 2 名：ZeroMistakeLater (0錯, 40s, 2000L)
        // 第 3 名：OneMistakeFaster (1錯)
        assertEquals("第 1 名應為 0 錯且時間早的紀錄", "ZeroMistakeEarlier", sorted[0].playerName)
        assertEquals("第 2 名應為 0 錯但時間較晚的紀錄", "ZeroMistakeLater", sorted[1].playerName)
        assertEquals("第 3 名應為有錯誤的紀錄", "OneMistakeFaster", sorted[2].playerName)
    }

    @Test
    fun testGlobalScoreEntryComparatorAndBetterScoreLogic() {
        val game = GameType.BRAIN_IN_TROUBLE

        val entryOld = GlobalScoreEntry(
            playerId = "p1",
            playerName = "Alice",
            countryCode = "TW",
            categoryKey = "TEST",
            gameTypeKey = game.key,
            difficultyKey = "BEGINNER",
            score = 1000,
            timestamp = 1000L
        )
        val entryNew = entryOld.copy(score = 1200, timestamp = 2000L)
        val entryLower = entryOld.copy(score = 800, timestamp = 3000L)

        assertTrue("更高的分數應視為更佳成績", game.isBetterGlobalScore(entryNew, entryOld))
        assertFalse("較低的分數不應視為更佳成績", game.isBetterGlobalScore(entryLower, entryOld))

        val entrySameScoreLater = entryOld.copy(score = 1000, timestamp = 1500L, playerName = "AliceLater")
        val sorted = listOf(entrySameScoreLater, entryOld).sortedWith(game.getGlobalScoreComparator())
        assertEquals("全球榜同分時，時間早者排在上方", "Alice", sorted[0].playerName)
    }
}
