package com.example.data.repository

import com.example.BuildConfig
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
        /**
         * 全球排行榜雲端後端 URL。
         *
         * 【安全規範與隱私保護原則】：
         * 本專案支援透過 Google Apps Script (GAS) 搭配 Google 試算表作為輕量級全球排行榜微後端。
         * 為了避免個人私有的 Google Drive 試算表或部署 Web App URL 公開洩漏於 GitHub 開源儲存庫中，
         * 本專案已將正式私有端點 URL 徹底抽離，統一由本機被 .gitignore 忽略的 `local.properties`
         * （或 CI/CD 環境變數 `GLOBAL_LEADERBOARD_URL`）在編譯期安全注入至 `BuildConfig.GLOBAL_LEADERBOARD_URL`。
         *
         * 若本地未配置 `GLOBAL_LEADERBOARD_URL`，預設為空字串 `""`，此時系統將自動平滑降級至
         * 「本地記憶體快取與模擬排行榜機制」，確保所有離線、本機測試與開源單元測試均可 100% 獨立運行。
         *
         * 若欲啟用您專屬的 Google 試算表全球排行榜微後端，請依照 README.md 中的說明部署 Apps Script，
         * 並在專案的 `MindGame/local.properties` 檔案中加入：
         * GLOBAL_LEADERBOARD_URL=https\://script.google.com/macros/s/YOUR_SCRIPT_ID/exec
         */
        val DEFAULT_SERVER_URL: String = BuildConfig.GLOBAL_LEADERBOARD_URL
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

            // 本地快取池更新（保留最佳成績並支援改名/換國旗資訊即時同步）
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
                } else {
                    // 若成績相同或未破紀錄，以最後更新的資料為準：
                    // 維持原最佳成績數值（score / timeMillis / wrongCount），但立即同步最新玩家名稱、國家與時間戳記！
                    localGlobalCache[existingIndex] = existing.copy(
                        playerName = entry.playerName,
                        countryCode = entry.countryCode,
                        timestamp = entry.timestamp
                    )
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
