package com.example.data.model

import java.util.Locale

data class Country(
    val code: String,
    val nameZh: String,
    val nameEn: String,
    val flagEmoji: String,
    val nameJa: String = nameEn,
    val nameKo: String = nameEn
) {
    fun getDisplayName(language: AppLanguage): String {
        return when (language) {
            AppLanguage.TRADITIONAL_CHINESE -> nameZh
            AppLanguage.ENGLISH -> nameEn
            AppLanguage.JAPANESE -> nameJa
            AppLanguage.KOREAN -> nameKo
        }
    }

    companion object {
        val ALL_COUNTRIES = listOf(
            Country("TW", "台灣", "Taiwan", "🇹🇼", "台湾", "대만"),
            Country("HK", "香港", "Hong Kong", "🇭🇰", "香港", "홍콩"),
            Country("MO", "澳門", "Macau", "🇲🇴", "マカオ", "마카오"),
            Country("JP", "日本", "Japan", "🇯🇵", "日本", "일본"),
            Country("KR", "韓國", "South Korea", "🇰🇷", "韓国", "한국"),
            Country("US", "美國", "United States", "🇺🇸", "アメリカ", "미국"),
            Country("GB", "英國", "United Kingdom", "🇬🇧", "イギリス", "영국"),
            Country("CA", "加拿大", "Canada", "🇨🇦", "カナダ", "캐나다"),
            Country("SG", "新加坡", "Singapore", "🇸🇬", "シンガポール", "싱가포르"),
            Country("MY", "馬來西亞", "Malaysia", "🇲🇾", "マレーシア", "말레이시아"),
            Country("AU", "澳洲", "Australia", "🇦🇺", "オーストラリア", "호주"),
            Country("DE", "德國", "Germany", "🇩🇪", "ドイツ", "독일"),
            Country("OTHER", "全球其他", "Other", "🌐", "その他", "기타")
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

data class HotGameEntry(
    val rank: Int = 0,
    val gameTypeKey: String,
    val votes: Int = 0
) {
    val gameType: GameType
        get() = GameType.fromKey(gameTypeKey)
}

data class HotGamesLeaderboardResponse(
    val month: String,
    val totalVoters: Int,
    val hotGames: List<HotGameEntry>
)

