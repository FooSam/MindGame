package com.example

import com.example.data.model.GameType
import com.example.data.model.HotGameEntry
import com.example.data.repository.GlobalLeaderboardRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HotGamesAndFavoriteVoteTest {

    private fun getCurrentMonth(): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    @Test
    fun testHotGameEntryDataModel() {
        val entry = HotGameEntry(rank = 1, gameTypeKey = GameType.GLASS_PUZZLE_CUBE.key, votes = 99)
        assertEquals(1, entry.rank)
        assertEquals("GLASS_PUZZLE_CUBE", entry.gameTypeKey)
        assertEquals(99, entry.votes)
        assertEquals(GameType.GLASS_PUZZLE_CUBE, entry.gameType)
    }

    @Test
    fun testFetchHotGamesInitialMockData() = runBlocking {
        val repo = GlobalLeaderboardRepository(customServerUrl = "")
        val currentMonth = getCurrentMonth()
        val result = repo.fetchHotGames(currentMonth)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(currentMonth, response.month)
        assertTrue("Total voters should be greater than 0", response.totalVoters > 0)
        assertTrue("Hot games list should not be empty", response.hotGames.isNotEmpty())

        // 驗證票數降序排列
        for (i in 0 until response.hotGames.size - 1) {
            val curr = response.hotGames[i]
            val next = response.hotGames[i + 1]
            assertTrue(
                "Rank ${curr.rank} (${curr.votes}) should have >= votes than Rank ${next.rank} (${next.votes})",
                curr.votes >= next.votes
            )
            assertEquals("Rank should be 1-indexed sequential", i + 1, curr.rank)
        }
    }

    @Test
    fun testVoteFavoritesAndAggregation() = runBlocking {
        val repo = GlobalLeaderboardRepository(customServerUrl = "")
        val currentMonth = getCurrentMonth()

        // 獲取投票前成績
        val beforeResult = repo.fetchHotGames(currentMonth).getOrThrow()
        val beforeCubeVotes = beforeResult.hotGames.find { it.gameTypeKey == GameType.GLASS_PUZZLE_CUBE.key }?.votes ?: 0

        // 玩家進行投票
        val voteResult = repo.voteFavorites(
            playerId = "unit_test_player_abc",
            playerName = "Tester",
            countryCode = "TW",
            favorites = listOf(GameType.GLASS_PUZZLE_CUBE, GameType.FRUIT_MASTER, GameType.CAT_SUDOKU),
            month = currentMonth
        )
        assertTrue(voteResult.isSuccess)

        // 重新獲取並驗證票數增加
        val afterResult = repo.fetchHotGames(currentMonth).getOrThrow()
        val afterCubeVotes = afterResult.hotGames.find { it.gameTypeKey == GameType.GLASS_PUZZLE_CUBE.key }?.votes ?: 0
        assertEquals("Glass puzzle cube votes should increase by 1", beforeCubeVotes + 1, afterCubeVotes)
    }

    @Test
    fun testVoteOverwriteSingleVoter() = runBlocking {
        val repo = GlobalLeaderboardRepository(customServerUrl = "")
        val currentMonth = getCurrentMonth()
        val testPlayerId = "unique_voter_123"

        // 第一次投票
        repo.voteFavorites(
            playerId = testPlayerId,
            playerName = "Sam",
            countryCode = "TW",
            favorites = listOf(GameType.FOCUS_TEST, GameType.FOCUS_TRAIN, GameType.SPEED_MATCH),
            month = currentMonth
        )
        val firstResp = repo.fetchHotGames(currentMonth).getOrThrow()
        val firstVoters = firstResp.totalVoters
        val firstFocusTestVotes = firstResp.hotGames.find { it.gameTypeKey == GameType.FOCUS_TEST.key }?.votes ?: 0

        // 第二次修改投票：換成另外 3 款遊戲
        repo.voteFavorites(
            playerId = testPlayerId,
            playerName = "Sam",
            countryCode = "TW",
            favorites = listOf(GameType.SUDOKU, GameType.CAT_SUDOKU, GameType.GLASS_PUZZLE_CUBE),
            month = currentMonth
        )
        val secondResp = repo.fetchHotGames(currentMonth).getOrThrow()
        assertEquals("Total voters should NOT increase when same player modifies vote", firstVoters, secondResp.totalVoters)

        val secondFocusTestVotes = secondResp.hotGames.find { it.gameTypeKey == GameType.FOCUS_TEST.key }?.votes ?: 0
        assertEquals("Original vote should be replaced, so FOCUS_TEST votes decremented by 1", firstFocusTestVotes - 1, secondFocusTestVotes)

        val secondSudokuVotes = secondResp.hotGames.find { it.gameTypeKey == GameType.SUDOKU.key }?.votes ?: 0
        assertTrue("Newly selected SUDOKU should have votes >= 1", secondSudokuVotes >= 1)
    }

    @Test
    fun testHotGamesTitleLocalization() {
        val titleZh = com.example.data.model.Localization.getString("hot_games_title", com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
        assertEquals("熱門遊戲排行榜繁中字串應為「全球玩家熱門遊戲排行榜」", "全球玩家熱門遊戲排行榜", titleZh)

        val currentGameZh = com.example.data.model.Localization.getString("current_game_label", com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
        assertEquals("當前遊戲", currentGameZh)

        val switchGameZh = com.example.data.model.Localization.getString("switch_game", com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
        assertEquals("切換", switchGameZh)
    }
}
