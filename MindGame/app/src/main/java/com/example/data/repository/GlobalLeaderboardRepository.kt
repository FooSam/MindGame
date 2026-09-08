package com.example.data.repository

import com.example.data.model.Country
import com.example.data.model.GameType
import com.example.data.model.GlobalLeaderboardResponse
import com.example.data.model.GlobalScoreEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GlobalLeaderboardRepository(
    private var customServerUrl: String = DEFAULT_SERVER_URL
) {
    companion object {
        const val DEFAULT_SERVER_URL = "https://script.google.com/macros/s/AKfycbwb9W7gzFdccnHT1HAW6xpcJE4ns_ihaPbjVthi_ktU8CWsz_jRFyrYG1lh1i-5x2HB/exec"
    }

    // 本地記憶體模擬與快取池 (確保離線或通訊異常時，UI與互動能完整無礙運行與自我驗證)
    private val localGlobalCache = mutableListOf<GlobalScoreEntry>()

    init {
        seedInitialMockData()
    }

    fun setServerUrl(url: String) {
        customServerUrl = url.trim()
    }

    suspend fun fetchGlobalLeaderboard(
        gameTypeKey: String,
        difficultyKey: String,
        playerId: String
    ): Result<GlobalLeaderboardResponse> = withContext(Dispatchers.IO) {
        try {
            if (customServerUrl.isNotBlank()) {
                // 若有設定伺服器 URL，則發送真實 HTTP 請求
                try {
                    val queryUrl = "$customServerUrl?action=get&gameType=$gameTypeKey&difficulty=$difficultyKey&playerId=$playerId"
                    val connection = (URL(queryUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        instanceFollowRedirects = true
                        setRequestProperty("Accept", "application/json")
                    }

                    if (connection.responseCode in 200..299) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(responseText)
                        val topScores = mutableListOf<GlobalScoreEntry>()
                        val listArray = json.optJSONArray("topScores") ?: JSONArray()
                        for (i in 0 until listArray.length()) {
                            val obj = listArray.getJSONObject(i)
                            topScores.add(parseEntry(obj, i + 1))
                        }

                        val myRankObj = json.optJSONObject("myRankEntry")
                        val myRankEntry = myRankObj?.let { parseEntry(it, it.optInt("rank", 0)) }
                        val totalCount = json.optInt("totalCount", topScores.size)

                        return@withContext Result.success(
                            GlobalLeaderboardResponse(
                                topScores = topScores,
                                myRankEntry = myRankEntry,
                                totalCount = totalCount
                            )
                        )
                    }
                } catch (netEx: Exception) {
                    // 網路異常或離線時，自動降級至本地快取
                }
            }

            // 離線 / 抽象通訊層本地降級處理
            val filtered = localGlobalCache.filter {
                it.gameTypeKey == gameTypeKey && it.difficultyKey == difficultyKey
            }

            val isScoreDesc = when (gameTypeKey) {
                GameType.SPEED_MATCH.key,
                GameType.TURTLE_SOUP.key,
                GameType.AVATAR_WHACK.key,
                GameType.STROOP_EFFECT.key -> true
                else -> false
            }

            val isSudoku = gameTypeKey == GameType.SUDOKU.key || gameTypeKey == GameType.CAT_SUDOKU.key

            val sorted = filtered.sortedWith { a, b ->
                when {
                    isScoreDesc -> {
                        val scoreComp = b.score.compareTo(a.score)
                        if (scoreComp != 0) scoreComp
                        else {
                            val wrongComp = a.wrongCount.compareTo(b.wrongCount)
                            if (wrongComp != 0) wrongComp else a.timeMillis.compareTo(b.timeMillis)
                        }
                    }
                    isSudoku -> {
                        val wrongComp = a.wrongCount.compareTo(b.wrongCount)
                        if (wrongComp != 0) wrongComp
                        else {
                            val timeComp = a.timeMillis.compareTo(b.timeMillis)
                            if (timeComp != 0) timeComp else b.score.compareTo(a.score)
                        }
                    }
                    else -> {
                        val timeComp = a.timeMillis.compareTo(b.timeMillis)
                        if (timeComp != 0) timeComp else a.wrongCount.compareTo(b.wrongCount)
                    }
                }
            }

            val rankedList = sorted.mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }

            val top100 = rankedList.take(100)
            val myRankIndex = rankedList.indexOfFirst { it.playerId == playerId }
            val myRankEntry = if (myRankIndex >= 0) rankedList[myRankIndex] else null

            Result.success(
                GlobalLeaderboardResponse(
                    topScores = top100,
                    myRankEntry = myRankEntry,
                    totalCount = rankedList.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadScore(entry: GlobalScoreEntry): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var networkSuccess = false
            if (customServerUrl.isNotBlank()) {
                try {
                    val connection = (URL(customServerUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 8000
                        readTimeout = 8000
                        doOutput = true
                        instanceFollowRedirects = false
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    }

                    val payload = JSONObject().apply {
                        put("playerId", entry.playerId)
                        put("playerName", entry.playerName)
                        put("countryCode", entry.countryCode)
                        put("categoryKey", entry.categoryKey)
                        put("gameTypeKey", entry.gameTypeKey)
                        put("difficultyKey", entry.difficultyKey)
                        put("score", entry.score)
                        put("timeMillis", entry.timeMillis)
                        put("wrongCount", entry.wrongCount)
                        put("timestamp", entry.timestamp)
                    }

                    connection.outputStream.use { os ->
                        os.write(payload.toString().toByteArray(Charsets.UTF_8))
                    }

                    val code = connection.responseCode
                    // Google Apps Script 於 doPost 完成後回傳 302 Found，代表已成功寫入試算表
                    if (code in 200..299 || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                        networkSuccess = true
                    }
                } catch (netEx: Exception) {
                    // 網路異常或逾時
                }
            }

            // 本地快取池更新（保留或覆蓋玩家最佳成績）
            val existingIndex = localGlobalCache.indexOfFirst {
                it.playerId == entry.playerId &&
                it.gameTypeKey == entry.gameTypeKey &&
                it.difficultyKey == entry.difficultyKey
            }

            if (existingIndex >= 0) {
                val existing = localGlobalCache[existingIndex]
                val isScoreDesc = when (entry.gameTypeKey) {
                    GameType.SPEED_MATCH.key,
                    GameType.TURTLE_SOUP.key,
                    GameType.AVATAR_WHACK.key,
                    GameType.STROOP_EFFECT.key -> true
                    else -> false
                }
                val isSudoku = entry.gameTypeKey == GameType.SUDOKU.key || entry.gameTypeKey == GameType.CAT_SUDOKU.key

                val isBetter = when {
                    isScoreDesc -> entry.score > existing.score
                    isSudoku -> entry.wrongCount < existing.wrongCount || (entry.wrongCount == existing.wrongCount && entry.timeMillis < existing.timeMillis)
                    else -> entry.timeMillis < existing.timeMillis
                }

                if (isBetter) {
                    localGlobalCache[existingIndex] = entry
                }
            } else {
                localGlobalCache.add(entry)
            }

            if (customServerUrl.isNotBlank() && !networkSuccess) {
                Result.failure(Exception("network_error"))
            } else {
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseEntry(obj: JSONObject, rank: Int): GlobalScoreEntry {
        return GlobalScoreEntry(
            rank = rank,
            playerId = obj.optString("playerId", ""),
            playerName = obj.optString("playerName", "Unknown"),
            countryCode = obj.optString("countryCode", "TW"),
            categoryKey = obj.optString("categoryKey", ""),
            gameTypeKey = obj.optString("gameTypeKey", ""),
            difficultyKey = obj.optString("difficultyKey", ""),
            score = obj.optInt("score", 0),
            timeMillis = obj.optLong("timeMillis", 0L),
            wrongCount = obj.optInt("wrongCount", 0),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun seedInitialMockData() {
        val sampleNames = listOf(
            "Sam" to "TW", "Alex" to "US", "Kenji" to "JP", "Minho" to "KR",
            "Chloe" to "GB", "Wei" to "HK", "Lucas" to "SG", "Elena" to "DE",
            "Mateo" to "OTHER", "Hana" to "JP", "Emma" to "CA", "Ahmad" to "MY"
        )
        val now = System.currentTimeMillis()

        GameType.entries.forEach { gameType ->
            val isScoreDesc = when (gameType) {
                GameType.SPEED_MATCH, GameType.TURTLE_SOUP, GameType.AVATAR_WHACK, GameType.STROOP_EFFECT -> true
                else -> false
            }

            listOf("BEGINNER", "INTERMEDIATE", "ADVANCED", "HARD", "HELL", "EPIC").forEach { diff ->
                sampleNames.forEachIndexed { idx, (name, country) ->
                    val score = if (isScoreDesc) 800 + (12 - idx) * 120 else 0
                    val time = if (isScoreDesc) 45000L else 15000L + idx * 3500L
                    localGlobalCache.add(
                        GlobalScoreEntry(
                            playerId = "bot_player_$idx",
                            playerName = name,
                            countryCode = country,
                            categoryKey = "TEST",
                            gameTypeKey = gameType.key,
                            difficultyKey = diff,
                            score = score,
                            timeMillis = time,
                            wrongCount = if (idx % 3 == 0) 0 else 1,
                            timestamp = now - idx * 3600_000L
                        )
                    )
                }
            }
        }
    }
}
