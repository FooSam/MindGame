package com.example.data.model

import java.util.Locale

data class Country(
    val code: String,
    val nameZh: String,
    val nameEn: String,
    val flagEmoji: String
) {
    fun getDisplayName(language: AppLanguage): String {
        return if (language == AppLanguage.TRADITIONAL_CHINESE) nameZh else nameEn
    }

    companion object {
        val ALL_COUNTRIES = listOf(
            Country("TW", "台灣", "Taiwan", "🇹🇼"),
            Country("HK", "香港", "Hong Kong", "🇭🇰"),
            Country("MO", "澳門", "Macau", "🇲🇴"),
            Country("JP", "日本", "Japan", "🇯🇵"),
            Country("KR", "韓國", "South Korea", "🇰🇷"),
            Country("US", "美國", "United States", "🇺🇸"),
            Country("GB", "英國", "United Kingdom", "🇬🇧"),
            Country("CA", "加拿大", "Canada", "🇨🇦"),
            Country("SG", "新加坡", "Singapore", "🇸🇬"),
            Country("MY", "馬來西亞", "Malaysia", "🇲🇾"),
            Country("AU", "澳洲", "Australia", "🇦🇺"),
            Country("DE", "德國", "Germany", "🇩🇪"),
            Country("OTHER", "全球其他", "Other", "🌐")
        )

        val DEFAULT = ALL_COUNTRIES[0] // 台灣

        fun fromCode(code: String): Country {
            val upper = code.trim().uppercase()
            return ALL_COUNTRIES.find { it.code.uppercase() == upper } ?: Country("OTHER", "全球其他", "Other", "🌐")
        }

        fun detectDefaultCountry(): Country {
            val systemCountry = try {
                Locale.getDefault().country.uppercase()
            } catch (e: Exception) {
                ""
            }
            return ALL_COUNTRIES.find { it.code == systemCountry } ?: DEFAULT
        }
    }
}

data class GlobalScoreEntry(
    val rank: Int = 0,
    val playerId: String,
    val playerName: String,
    val countryCode: String,
    val categoryKey: String,
    val gameTypeKey: String,
    val difficultyKey: String,
    val score: Int = 0,
    val timeMillis: Long = 0L,
    val wrongCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val country: Country
        get() = Country.fromCode(countryCode)
}

data class GlobalLeaderboardResponse(
    val topScores: List<GlobalScoreEntry>,
    val myRankEntry: GlobalScoreEntry? = null,
    val totalCount: Int = 0
)
