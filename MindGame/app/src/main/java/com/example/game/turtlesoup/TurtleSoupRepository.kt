package com.example.game.turtlesoup

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.TurtleSoupPuzzle
import com.example.data.model.TurtleSoupRecord
import com.example.data.model.TurtleSoupSaveData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TurtleSoupRepository {
    private const val TAG = "TurtleSoupRepository"
    private const val PREF_KEY_SAVE_DATA = "TURTLE_SOUP_SAVE_DATA"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val puzzleListType = Types.newParameterizedType(List::class.java, TurtleSoupPuzzle::class.java)
    private val puzzleListAdapter = moshi.adapter<List<TurtleSoupPuzzle>>(puzzleListType)
    private val saveDataSaveAdapter = moshi.adapter(TurtleSoupSaveData::class.java)

    @Volatile
    private var cachedPuzzles: List<TurtleSoupPuzzle>? = null

    /**
     * 從 Assets 載入並快取海龜湯題庫
     */
    fun loadPuzzles(context: Context): List<TurtleSoupPuzzle> {
        cachedPuzzles?.let { return it }

        synchronized(this) {
            cachedPuzzles?.let { return it }
            try {
                val jsonString = context.assets.open("puzzles.json").bufferedReader().use { it.readText() }
                val puzzles = puzzleListAdapter.fromJson(jsonString) ?: emptyList()
                cachedPuzzles = puzzles
                Log.d(TAG, "Successfully loaded ${puzzles.size} puzzles from assets.")
                return puzzles
            } catch (e: Exception) {
                Log.e(TAG, "Error reading puzzles.json from assets", e)
                return emptyList()
            }
        }
    }

    /**
     * 依難度取得題目清單 (Easy / Medium / Hard)
     */
    fun getPuzzlesByDifficulty(context: Context, difficulty: String): List<TurtleSoupPuzzle> {
        val all = loadPuzzles(context)
        return all.filter { it.difficulty.equals(difficulty, ignoreCase = true) }
    }

    /**
     * 讀取本機存檔 (TURTLE_SOUP_SAVE_DATA)
     */
    fun loadSaveData(context: Context): TurtleSoupSaveData {
        val prefs = getPrefs(context)
        val json = prefs.getString(PREF_KEY_SAVE_DATA, null) ?: return TurtleSoupSaveData()
        return try {
            saveDataSaveAdapter.fromJson(json) ?: TurtleSoupSaveData()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse TURTLE_SOUP_SAVE_DATA", e)
            TurtleSoupSaveData()
        }
    }

    /**
     * 儲存通關紀錄
     */
    fun savePuzzleCleared(
        context: Context,
        puzzleId: String,
        difficulty: String,
        stars: Int,
        usedChances: Int,
        score: Int
    ): TurtleSoupSaveData {
        val prefs = getPrefs(context)
        val currentSave = loadSaveData(context)

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val clearedAt = isoFormat.format(Date())

        val existingRecordIndex = currentSave.records.indexOfFirst { it.puzzleId == puzzleId }
        val newRecords = currentSave.records.toMutableList()

        val newRecord = TurtleSoupRecord(
            puzzleId = puzzleId,
            difficulty = difficulty,
            stars = stars,
            usedChances = usedChances,
            score = score,
            clearedAt = clearedAt
        )

        if (existingRecordIndex >= 0) {
            val existing = currentSave.records[existingRecordIndex]
            // 保留最高星星與最高分
            val bestRecord = existing.copy(
                stars = maxOf(existing.stars, stars),
                score = maxOf(existing.score, score),
                usedChances = minOf(existing.usedChances, usedChances),
                clearedAt = clearedAt
            )
            newRecords[existingRecordIndex] = bestRecord
        } else {
            newRecords.add(newRecord)
        }

        val updatedSave = currentSave.copy(records = newRecords)
        try {
            val json = saveDataSaveAdapter.toJson(updatedSave)
            prefs.edit().putString(PREF_KEY_SAVE_DATA, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save TURTLE_SOUP_SAVE_DATA", e)
        }

        return updatedSave
    }

    /**
     * 依難度取得標準初始提問次數 (簡單 4 次 / 普通 3 次 / 困難 3 次)
     */
    fun getInitialChances(difficulty: String): Int {
        return when (difficulty.uppercase()) {
            "EASY", "BEGINNER" -> 4
            "MEDIUM", "INTERMEDIATE", "ADVANCED" -> 3
            "HARD", "HELL", "EPIC" -> 3
            else -> 3
        }
    }

    /**
     * 取得該題目的有效調查維度 (若題庫已有 dimensions 則使用，否則依題目結構動態生成 3~4 個維度)
     */
    fun getEffectiveDimensions(puzzle: TurtleSoupPuzzle, language: AppLanguage): List<com.example.data.model.InvestigationDimension> {
        if (!puzzle.dimensions.isNullOrEmpty()) {
            return puzzle.dimensions
        }

        // 動態從題目 SlotDeduction 與 Questions 中提取維度
        val dimensions = mutableListOf<com.example.data.model.InvestigationDimension>()
        val slots = puzzle.slotDeduction.slots

        // 1. 人物/身分維度
        val charOptionsZh = mutableListOf<String>()
        val charOptionsEn = mutableListOf<String>()
        val charSlot = slots.firstOrNull { it.slotId.contains("1") || it.label.zhTW.contains("身分") || it.label.zhTW.contains("人") }
        if (charSlot != null) {
            charOptionsZh.addAll(charSlot.options.zhTW)
            charOptionsEn.addAll(charSlot.options.en)
        } else {
            charOptionsZh.addAll(listOf("主角/男子", "警方/調查員", "受害者", "目擊證人", "陌生人"))
            charOptionsEn.addAll(listOf("Main Character", "Police/Detective", "Victim", "Eyewitness", "Stranger"))
        }
        dimensions.add(
            com.example.data.model.InvestigationDimension(
                id = "character",
                label = com.example.data.model.LocalizedText(zhTW = "人物對象", en = "Character"),
                options = com.example.data.model.LocalizedOptions(zhTW = charOptionsZh.distinct(), en = charOptionsEn.distinct())
            )
        )

        // 2. 物件/道具維度
        val itemOptionsZh = mutableListOf<String>()
        val itemOptionsEn = mutableListOf<String>()
        puzzle.questions.forEach { q ->
            val kwZh = q.keyword.zhTW
            val kwEn = q.keyword.en
            if (kwZh.isNotEmpty() && !kwZh.contains("動機") && !kwZh.contains("身分")) {
                itemOptionsZh.add(kwZh)
                itemOptionsEn.add(kwEn)
            }
        }
        val itemSlot = slots.firstOrNull { it.slotId.contains("2") || it.label.zhTW.contains("手法") || it.label.zhTW.contains("物") }
        if (itemSlot != null) {
            itemOptionsZh.addAll(itemSlot.options.zhTW)
            itemOptionsEn.addAll(itemSlot.options.en)
        }
        if (itemOptionsZh.isEmpty()) {
            itemOptionsZh.addAll(listOf("案發現場物品", "隨身攜帶物", "通訊工具", "凶器道具"))
            itemOptionsEn.addAll(listOf("Scene Object", "Personal Belonging", "Communication Device", "Tool/Weapon"))
        }
        dimensions.add(
            com.example.data.model.InvestigationDimension(
                id = "item",
                label = com.example.data.model.LocalizedText(zhTW = "物件媒介", en = "Item/Object"),
                options = com.example.data.model.LocalizedOptions(zhTW = itemOptionsZh.distinct(), en = itemOptionsEn.distinct())
            )
        )

        // 3. 事件/動機/行為維度
        val eventOptionsZh = mutableListOf<String>()
        val eventOptionsEn = mutableListOf<String>()
        val eventSlot = slots.firstOrNull { it.slotId.contains("3") || it.label.zhTW.contains("原因") || it.label.zhTW.contains("破綻") || it.label.zhTW.contains("動機") }
        if (eventSlot != null) {
            eventOptionsZh.addAll(eventSlot.options.zhTW)
            eventOptionsEn.addAll(eventSlot.options.en)
        } else {
            eventOptionsZh.addAll(listOf("職業身分關聯", "意外事故", "預謀犯罪", "心裡恐慌", "不在場證明失效"))
            eventOptionsEn.addAll(listOf("Occupation Link", "Accident", "Premeditated Crime", "Panic", "Alibi Ruined"))
        }
        dimensions.add(
            com.example.data.model.InvestigationDimension(
                id = "event",
                label = com.example.data.model.LocalizedText(zhTW = "事件動機", en = "Event/Motive"),
                options = com.example.data.model.LocalizedOptions(zhTW = eventOptionsZh.distinct(), en = eventOptionsEn.distinct())
            )
        )

        return dimensions
    }

    /**
     * 評估玩家選取的多維度組合，進行智能判定與生成調查日誌
     */
    fun evaluateQuery(
        puzzle: TurtleSoupPuzzle,
        selectedDimensionValues: Map<String, String>,
        language: AppLanguage
    ): com.example.data.model.InvestigationQueryLog {
        val selectedWords = selectedDimensionValues.values.toList()
        val queryText = selectedWords.joinToString(" + ")
        val queryId = "q_${System.currentTimeMillis()}"

        // 1. 比對自定義 rules (若有)
        puzzle.rules?.forEach { rule ->
            val matchCount = rule.keywords.count { kw ->
                selectedWords.any { it.contains(kw, ignoreCase = true) }
            }
            if (matchCount >= 2 || (rule.keywords.size == 1 && matchCount == 1)) {
                return com.example.data.model.InvestigationQueryLog(
                    queryId = queryId,
                    selections = selectedDimensionValues,
                    queryText = queryText,
                    answer = rule.answerType,
                    detail = rule.detail.get(language),
                    isCore = rule.isCore
                )
            }
        }

        // 2. 比對題庫內置 questions
        var bestQuestion: com.example.data.model.PuzzleQuestion? = null
        var bestScore = 0

        val wordTokensZh = mutableListOf<String>()
        val wordTokensEn = mutableListOf<String>()
        selectedWords.forEach { w ->
            wordTokensZh.add(w)
            val partsZh = w.split('/', ' ', '、', '，', '（', '）', '(', ')', '／').filter { it.length >= 2 }
            wordTokensZh.addAll(partsZh)
            partsZh.forEach { part ->
                if (part.length >= 3) {
                    for (n in 2..minOf(part.length, 4)) {
                        for (i in 0..(part.length - n)) {
                            wordTokensZh.add(part.substring(i, i + n))
                        }
                    }
                }
            }

            wordTokensEn.add(w)
            val partsEn = w.split('/', ' ', ',', '(', ')', '-').filter { it.length >= 2 }
            wordTokensEn.addAll(partsEn)
        }

        val distinctTokensZh = wordTokensZh.distinct()
        val distinctTokensEn = wordTokensEn.distinct()

        puzzle.questions.forEach { q ->
            var score = 0
            val kwZh = q.keyword.zhTW
            val kwEn = q.keyword.en
            val qTextZh = q.question.zhTW
            val qTextEn = q.question.en
            val detailZh = q.detail.zhTW
            val detailEn = q.detail.en

            distinctTokensZh.forEach { token ->
                if (kwZh.isNotEmpty() && (token.contains(kwZh) || kwZh.contains(token))) score += 4
                if (qTextZh.contains(token)) score += 2
                if (detailZh.contains(token)) score += 2
            }

            distinctTokensEn.forEach { token ->
                if (kwEn.isNotEmpty() && (token.contains(kwEn, ignoreCase = true) || kwEn.contains(token, ignoreCase = true))) score += 4
                if (qTextEn.contains(token, ignoreCase = true)) score += 2
                if (detailEn.contains(token, ignoreCase = true)) score += 2
            }

            // 核心問題加權
            if (q.isCore && score > 0) score += 3

            if (score > bestScore) {
                bestScore = score
                bestQuestion = q
            }
        }


        if (bestQuestion != null && bestScore >= 2) {
            val q = bestQuestion!!
            return com.example.data.model.InvestigationQueryLog(
                queryId = queryId,
                selections = selectedDimensionValues,
                queryText = queryText,
                answer = q.answerType,
                detail = q.detail.get(language),
                isCore = q.isCore
            )
        }

        // 3. 通用煙霧彈與未命中處理
        val isZh = language == AppLanguage.TRADITIONAL_CHINESE
        val defaultDetail = if (isZh) {
            "【調查結果】經向現場與證人查證，此人事物組合與本案真相無直接關聯或假設不成立。"
        } else {
            "【Investigation】After checking evidence and witnesses, this combination has no direct link to the truth."
        }

        return com.example.data.model.InvestigationQueryLog(
            queryId = queryId,
            selections = selectedDimensionValues,
            queryText = queryText,
            answer = com.example.data.model.QuestionAnswer.IRRELEVANT,
            detail = defaultDetail,
            isCore = false
        )
    }

    /**
     * 計算基礎分數
     */
    fun getBaseScore(difficulty: String): Int {
        return when (difficulty.uppercase()) {
            "EASY", "BEGINNER", "INTERMEDIATE" -> 1000
            "MEDIUM", "ADVANCED" -> 2000
            "HARD", "HELL", "EPIC" -> 3000
            else -> 1000
        }
    }

    /**
     * 計算通關得分與星級
     * 公式: Score = BaseScore + (RemainingChances * 100) - (ElapsedSeconds * 2)
     * 若使用廣告回補次數: 分數固定為 BaseScore 的 50%，星級為 0
     */
    fun calculateScoreAndStars(
        difficulty: String,
        usedChances: Int,
        remainingChances: Int,
        elapsedSeconds: Long,
        usedAdReward: Boolean
    ): Pair<Int, Int> {
        val baseScore = getBaseScore(difficulty)

        if (usedAdReward) {
            val adScore = (baseScore * 0.5f).toInt()
            return Pair(adScore, 0)
        }

        val bonus = remainingChances * 100
        val penalty = (elapsedSeconds * 2).toInt()
        val calculatedScore = (baseScore + bonus - penalty).coerceAtLeast(100)

        val stars = when {
            usedChances <= 2 -> 3
            usedChances <= 4 -> 2
            else -> 1
        }

        return Pair(calculatedScore, stars)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("mindgame_prefs", Context.MODE_PRIVATE)
    }
}
