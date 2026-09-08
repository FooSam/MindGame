package com.example

import com.example.data.model.AppLanguage
import com.example.data.model.Country
import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.data.model.GlobalScoreEntry
import com.example.data.repository.GlobalLeaderboardRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalLeaderboardTest {

    @Test
    fun testCountryCatalogAndLocalization() {
        assertTrue(Country.ALL_COUNTRIES.isNotEmpty())
        assertTrue(Country.ALL_COUNTRIES.any { it.code == "TW" })
        assertTrue(Country.ALL_COUNTRIES.any { it.code == "JP" })
        assertTrue(Country.ALL_COUNTRIES.any { it.code == "US" })

        val tw = Country.fromCode("tw")
        assertEquals("TW", tw.code)
        assertEquals("台灣", tw.getDisplayName(AppLanguage.TRADITIONAL_CHINESE))
        assertEquals("Taiwan", tw.getDisplayName(AppLanguage.ENGLISH))
        assertEquals("🇹🇼", tw.flagEmoji)

        val fallback = Country.fromCode("XYZ_UNKNOWN")
        assertEquals("OTHER", fallback.code)
        assertEquals("🌐", fallback.flagEmoji)
    }

    @Test
    fun testGlobalLeaderboardFetchAndRanking() = runBlocking {
        val repo = GlobalLeaderboardRepository(customServerUrl = "")
        val result = repo.fetchGlobalLeaderboard(
            gameTypeKey = GameType.AVATAR_WHACK.key,
            difficultyKey = GameDifficulty.BEGINNER.key,
            playerId = "bot_player_0"
        )

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue("Top scores should not be empty", response.topScores.isNotEmpty())
        assertTrue("Top scores should not exceed 100", response.topScores.size <= 100)

        // 打那個誰為分數型，驗證第一名分數大於等於第二名
        if (response.topScores.size >= 2) {
            assertTrue(response.topScores[0].score >= response.topScores[1].score)
        }

        // 驗證自己排名檢索
        assertNotNull(response.myRankEntry)
        assertEquals("bot_player_0", response.myRankEntry?.playerId)
    }

    @Test
    fun testGlobalScoreUploadAndBestScoreRetention() = runBlocking {
        val repo = GlobalLeaderboardRepository(customServerUrl = "")
        val myId = "test_player_unique_id"
        val gameKey = GameType.SPEED_MATCH.key
        val diffKey = GameDifficulty.HARD.key

        // 首次上傳 500 分
        val entry1 = GlobalScoreEntry(
            playerId = myId,
            playerName = "Hero",
            countryCode = "TW",
            categoryKey = "TEST",
            gameTypeKey = gameKey,
            difficultyKey = diffKey,
            score = 500,
            timeMillis = 45000L,
            wrongCount = 1
        )
        val upload1 = repo.uploadScore(entry1)
        assertTrue(upload1.isSuccess)

        val res1 = repo.fetchGlobalLeaderboard(gameKey, diffKey, myId).getOrThrow()
        assertEquals(500, res1.myRankEntry?.score)

        // 上傳較差的 300 分，應保留 500 分
        val entry2 = entry1.copy(score = 300)
        repo.uploadScore(entry2)
        val res2 = repo.fetchGlobalLeaderboard(gameKey, diffKey, myId).getOrThrow()
        assertEquals("較低的分數不應覆蓋最佳紀錄", 500, res2.myRankEntry?.score)

        // 上傳更佳的 800 分，應成功更新
        val entry3 = entry1.copy(score = 800)
        repo.uploadScore(entry3)
        val res3 = repo.fetchGlobalLeaderboard(gameKey, diffKey, myId).getOrThrow()
        assertEquals("更高的分數應成功更新最佳紀錄", 800, res3.myRankEntry?.score)
    }

    @Test
    fun testTimeBasedRankingOrder() = runBlocking {
        val repo = GlobalLeaderboardRepository(customServerUrl = "")
        val res = repo.fetchGlobalLeaderboard(
            gameTypeKey = GameType.FOCUS_TEST.key,
            difficultyKey = GameDifficulty.BEGINNER.key,
            playerId = "nobody"
        ).getOrThrow()

        // 專注力為時間型，時間越短排名越前
        if (res.topScores.size >= 2) {
            assertTrue(res.topScores[0].timeMillis <= res.topScores[1].timeMillis)
        }
    }
}
