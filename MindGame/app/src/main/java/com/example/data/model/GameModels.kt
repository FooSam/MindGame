package com.example.data.model

enum class GameCategory(val key: String) {
    TEST("TEST"),
    BRAIN("BRAIN"),
    DEDUCTION("DEDUCTION"),
    CASUAL("CASUAL")
}

enum class RankingType {
    /** 依分數由高至低排序，同分以時間戳記早 (timestamp ASC, id ASC) 優先 */
    SCORE_DESC,
    /** 依完成時間由短至長排序，同時間以時間戳記早 (timestamp ASC, id ASC) 優先 */
    TIME_ASC
}

enum class GameType(
    val key: String,
    val titleKey: String,
    val descKey: String,
    val rankingType: RankingType,
    val category: GameCategory,
    val scoreUnitZh: String = "分",
    val scoreUnitEn: String = "pts"
) {
    FOCUS_TEST("FOCUS_TEST", "game_focus_test", "game_focus_test_desc", RankingType.TIME_ASC, GameCategory.TEST),
    FOCUS_TRAIN("FOCUS_TRAIN", "game_focus_train", "game_focus_train_desc", RankingType.TIME_ASC, GameCategory.TEST),
    SPEED_MATCH("SPEED_MATCH", "game_speed_match", "game_speed_match_desc", RankingType.SCORE_DESC, GameCategory.TEST, scoreUnitZh = "次", scoreUnitEn = "times"),
    SUDOKU("SUDOKU", "game_sudoku", "game_sudoku_desc", RankingType.TIME_ASC, GameCategory.BRAIN),
    CAT_SUDOKU("CAT_SUDOKU", "game_cat_sudoku", "game_cat_sudoku_desc", RankingType.TIME_ASC, GameCategory.BRAIN),
    TURTLE_SOUP("TURTLE_SOUP", "game_turtle_soup", "game_turtle_soup_desc", RankingType.SCORE_DESC, GameCategory.DEDUCTION),
    AVATAR_WHACK("AVATAR_WHACK", "game_avatar_whack", "game_avatar_whack_desc", RankingType.SCORE_DESC, GameCategory.TEST),
    STROOP_EFFECT("STROOP_EFFECT", "game_stroop_effect", "game_stroop_effect_desc", RankingType.SCORE_DESC, GameCategory.TEST),
    BLOCK_PUZZLE("BLOCK_PUZZLE", "game_block_puzzle", "game_block_puzzle_desc", RankingType.SCORE_DESC, GameCategory.CASUAL),
    FRUIT_MASTER("FRUIT_MASTER", "game_fruit_master", "game_fruit_master_desc", RankingType.SCORE_DESC, GameCategory.CASUAL),
    GLASS_PUZZLE_CUBE("GLASS_PUZZLE_CUBE", "game_glass_puzzle_cube", "game_glass_puzzle_cube_desc", RankingType.TIME_ASC, GameCategory.BRAIN),
    PINBALL_FLIPPER("PINBALL_FLIPPER", "game_pinball_flipper", "game_pinball_flipper_desc", RankingType.SCORE_DESC, GameCategory.TEST),
    NIGHT_MARKET_PINBALL("NIGHT_MARKET_PINBALL", "game_night_market_pinball", "game_night_market_pinball_desc", RankingType.SCORE_DESC, GameCategory.CASUAL),
    BRAIN_IN_TROUBLE("BRAIN_IN_TROUBLE", "game_brain_in_trouble", "game_brain_in_trouble_desc", RankingType.SCORE_DESC, GameCategory.TEST),
    LASER_MAZE("LASER_MAZE", "game_laser_maze", "game_laser_maze_desc", RankingType.TIME_ASC, GameCategory.DEDUCTION);

    val isSudoku: Boolean
        get() = this == SUDOKU || this == CAT_SUDOKU

    companion object {
        fun fromKey(key: String): GameType {
            return entries.find { it.key == key } ?: FOCUS_TEST
        }
    }
}

data class WheelRingInfo(
    val ringIndex: Int, // 1-based (1, 2, 3)
    val itemCount: Int,
    val isClockwise: Boolean
)

data class WheelDifficultyConfig(
    val difficulty: GameDifficulty,
    val hasCenter: Boolean = true,
    val rings: List<WheelRingInfo>,
    val totalNumbers: Int,
    val rotationDurationMs: Int
) {
    companion object {
        fun getConfig(difficulty: GameDifficulty): WheelDifficultyConfig {
            return when (difficulty) {
                GameDifficulty.BEGINNER -> WheelDifficultyConfig(
                    difficulty = difficulty,
                    rings = listOf(
                        WheelRingInfo(1, 6, isClockwise = true)
                    ),
                    totalNumbers = 7,
                    rotationDurationMs = 20_000
                )
                GameDifficulty.INTERMEDIATE -> WheelDifficultyConfig(
                    difficulty = difficulty,
                    rings = listOf(
                        WheelRingInfo(1, 12, isClockwise = true)
                    ),
                    totalNumbers = 13,
                    rotationDurationMs = 20_000
                )
                GameDifficulty.ADVANCED -> WheelDifficultyConfig(
                    difficulty = difficulty,
                    rings = listOf(
                        WheelRingInfo(1, 6, isClockwise = true),
                        WheelRingInfo(2, 12, isClockwise = false)
                    ),
                    totalNumbers = 19,
                    rotationDurationMs = 20_000
                )
                GameDifficulty.HARD -> WheelDifficultyConfig(
                    difficulty = difficulty,
                    rings = listOf(
                        WheelRingInfo(1, 12, isClockwise = true),
                        WheelRingInfo(2, 24, isClockwise = false)
                    ),
                    totalNumbers = 37,
                    rotationDurationMs = 15_000
                )
                GameDifficulty.HELL -> WheelDifficultyConfig(
                    difficulty = difficulty,
                    rings = listOf(
                        WheelRingInfo(1, 6, isClockwise = true),
                        WheelRingInfo(2, 12, isClockwise = false),
                        WheelRingInfo(3, 24, isClockwise = true)
                    ),
                    totalNumbers = 43,
                    rotationDurationMs = 15_000
                )
                GameDifficulty.EPIC -> WheelDifficultyConfig(
                    difficulty = difficulty,
                    rings = listOf(
                        WheelRingInfo(1, 12, isClockwise = true),
                        WheelRingInfo(2, 24, isClockwise = false),
                        WheelRingInfo(3, 32, isClockwise = true)
                    ),
                    totalNumbers = 69,
                    rotationDurationMs = 15_000
                )
            }
        }
    }
}

enum class GameDifficulty(
    val key: String,
    val gridDim: Int,
    val totalCells: Int,
    val speedMatchCells: Int,
    val speedMatchCols: Int
) {
    BEGINNER("BEGINNER", 3, 9, 6, 3),
    INTERMEDIATE("INTERMEDIATE", 4, 16, 9, 3),
    ADVANCED("ADVANCED", 5, 25, 12, 4),
    HARD("HARD", 6, 36, 16, 4),
    HELL("HELL", 7, 49, 20, 4),
    EPIC("EPIC", 8, 64, 25, 5);

    companion object {
        fun fromKey(key: String): GameDifficulty {
            return entries.find { it.key == key } ?: BEGINNER
        }
    }
}

/**
 * 本地排行榜記錄比較器：
 * - SCORE_DESC: 分數最高排第 1，同分以時間早 (timestamp ASC, id ASC) 優先
 * - TIME_ASC: 耗時最短排第 1 (數獨先比錯誤數少)，同耗時以時間早 (timestamp ASC, id ASC) 優先
 */
fun GameType.getScoreRecordComparator(): Comparator<com.example.data.db.ScoreRecord> {
    return when (this.rankingType) {
        RankingType.SCORE_DESC -> {
            compareByDescending<com.example.data.db.ScoreRecord> { it.score }
                .thenBy { it.timestamp }
                .thenBy { it.id }
        }
        RankingType.TIME_ASC -> {
            if (this.isSudoku) {
                compareBy<com.example.data.db.ScoreRecord> { it.wrongCount }
                    .thenBy { it.timeMillis }
                    .thenBy { it.timestamp }
                    .thenBy { it.id }
            } else {
                compareBy<com.example.data.db.ScoreRecord> { it.timeMillis }
                    .thenBy { it.timestamp }
                    .thenBy { it.id }
            }
        }
    }
}

/**
 * 全球排行榜記錄比較器：
 * 規則與本地排行榜 100% 同步
 */
fun GameType.getGlobalScoreComparator(): Comparator<GlobalScoreEntry> {
    return when (this.rankingType) {
        RankingType.SCORE_DESC -> {
            compareByDescending<GlobalScoreEntry> { it.score }
                .thenBy { it.timestamp }
        }
        RankingType.TIME_ASC -> {
            if (this.isSudoku) {
                compareBy<GlobalScoreEntry> { it.wrongCount }
                    .thenBy { it.timeMillis }
                    .thenBy { it.timestamp }
            } else {
                compareBy<GlobalScoreEntry> { it.timeMillis }
                    .thenBy { it.timestamp }
            }
        }
    }
}

/**
 * 判定新上傳成績是否打破現有最佳紀錄
 */
fun GameType.isBetterGlobalScore(newEntry: GlobalScoreEntry, existing: GlobalScoreEntry): Boolean {
    return when (this.rankingType) {
        RankingType.SCORE_DESC -> newEntry.score > existing.score
        RankingType.TIME_ASC -> {
            if (this.isSudoku) {
                newEntry.wrongCount < existing.wrongCount ||
                        (newEntry.wrongCount == existing.wrongCount && newEntry.timeMillis < existing.timeMillis)
            } else {
                newEntry.timeMillis < existing.timeMillis
            }
        }
    }
}

