package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocalizedText(
    @Json(name = "zh-TW") val zhTW: String = "",
    @Json(name = "en") val en: String = ""
) {
    fun get(language: AppLanguage): String {
        return when (language) {
            AppLanguage.TRADITIONAL_CHINESE -> if (zhTW.isNotEmpty()) zhTW else en
            AppLanguage.ENGLISH -> if (en.isNotEmpty()) en else zhTW
        }
    }
}

@JsonClass(generateAdapter = true)
data class LocalizedOptions(
    @Json(name = "zh-TW") val zhTW: List<String> = emptyList(),
    @Json(name = "en") val en: List<String> = emptyList()
) {
    fun get(language: AppLanguage): List<String> {
        return when (language) {
            AppLanguage.TRADITIONAL_CHINESE -> if (zhTW.isNotEmpty()) zhTW else en
            AppLanguage.ENGLISH -> if (en.isNotEmpty()) en else zhTW
        }
    }
}

enum class QuestionAnswer(val key: String) {
    YES("YES"),
    NO("NO"),
    IRRELEVANT("IRRELEVANT");

    companion object {
        fun fromString(str: String): QuestionAnswer {
            return when (str.trim().uppercase()) {
                "YES" -> YES
                "NO" -> NO
                else -> IRRELEVANT
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class PuzzleQuestion(
    @Json(name = "id") val id: String,
    @Json(name = "keyword") val keyword: LocalizedText,
    @Json(name = "question") val question: LocalizedText,
    @Json(name = "answer") val answer: String,
    @Json(name = "detail") val detail: LocalizedText,
    @Json(name = "is_core") val isCore: Boolean = false
) {
    val answerType: QuestionAnswer
        get() = QuestionAnswer.fromString(answer)
}

@JsonClass(generateAdapter = true)
data class DeductionSlot(
    @Json(name = "slot_id") val slotId: String,
    @Json(name = "label") val label: LocalizedText,
    @Json(name = "options") val options: LocalizedOptions,
    @Json(name = "correct_index") val correctIndex: Int
)

@JsonClass(generateAdapter = true)
data class SlotDeduction(
    @Json(name = "template") val template: LocalizedText,
    @Json(name = "slots") val slots: List<DeductionSlot> = emptyList()
)

@JsonClass(generateAdapter = true)
data class InvestigationDimension(
    @Json(name = "id") val id: String,
    @Json(name = "label") val label: LocalizedText,
    @Json(name = "options") val options: LocalizedOptions
)

@JsonClass(generateAdapter = true)
data class CombinationRule(
    @Json(name = "keywords") val keywords: List<String> = emptyList(),
    @Json(name = "answer") val answer: String = "IRRELEVANT",
    @Json(name = "detail") val detail: LocalizedText = LocalizedText(),
    @Json(name = "is_core") val isCore: Boolean = false
) {
    val answerType: QuestionAnswer
        get() = QuestionAnswer.fromString(answer)
}

@JsonClass(generateAdapter = true)
data class InvestigationQueryLog(
    @Json(name = "query_id") val queryId: String,
    @Json(name = "selections") val selections: Map<String, String> = emptyMap(),
    @Json(name = "query_text") val queryText: String = "",
    @Json(name = "answer") val answer: QuestionAnswer = QuestionAnswer.IRRELEVANT,
    @Json(name = "detail") val detail: String = "",
    @Json(name = "is_core") val isCore: Boolean = false,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class TurtleSoupPuzzle(
    @Json(name = "id") val id: String,
    @Json(name = "difficulty") val difficulty: String,
    @Json(name = "category") val category: LocalizedText,
    @Json(name = "title") val title: LocalizedText,
    @Json(name = "surface") val surface: LocalizedText,
    @Json(name = "initial_chances") val initialChances: Int = 4,
    @Json(name = "ad_reward_chances") val adRewardChances: Int = 3,
    @Json(name = "required_core_clues_count") val requiredCoreCluesCount: Int = 2,
    @Json(name = "questions") val questions: List<PuzzleQuestion> = emptyList(),
    @Json(name = "dimensions") val dimensions: List<InvestigationDimension>? = null,
    @Json(name = "rules") val rules: List<CombinationRule>? = null,
    @Json(name = "slot_deduction") val slotDeduction: SlotDeduction,
    @Json(name = "bottom") val bottom: LocalizedText
)

// 關卡進度與存檔模型 (LocalStorage Schema: TURTLE_SOUP_SAVE_DATA)
@JsonClass(generateAdapter = true)
data class TurtleSoupRecord(
    @Json(name = "puzzle_id") val puzzleId: String,
    @Json(name = "difficulty") val difficulty: String,
    @Json(name = "stars") val stars: Int,
    @Json(name = "used_chances") val usedChances: Int,
    @Json(name = "score") val score: Int,
    @Json(name = "cleared_at") val clearedAt: String
)

@JsonClass(generateAdapter = true)
data class TurtleSoupLeaderboardItem(
    @Json(name = "rank") val rank: Int,
    @Json(name = "puzzle_id") val puzzleId: String,
    @Json(name = "score") val score: Int,
    @Json(name = "stars") val stars: Int,
    @Json(name = "date") val date: String
)

@JsonClass(generateAdapter = true)
data class TurtleSoupSaveData(
    @Json(name = "records") val records: List<TurtleSoupRecord> = emptyList(),
    @Json(name = "leaderboard") val leaderboard: List<TurtleSoupLeaderboardItem> = emptyList()
)
