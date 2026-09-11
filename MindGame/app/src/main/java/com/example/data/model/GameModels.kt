package com.example.data.model

enum class GameCategory(val key: String) {
    TEST("TEST"),
    BRAIN("BRAIN"),
    DEDUCTION("DEDUCTION"),
    CASUAL("CASUAL")
}

enum class GameType(val key: String, val titleKey: String, val descKey: String) {
    FOCUS_TEST("FOCUS_TEST", "game_focus_test", "game_focus_test_desc"),
    FOCUS_TRAIN("FOCUS_TRAIN", "game_focus_train", "game_focus_train_desc"),
    SPEED_MATCH("SPEED_MATCH", "game_speed_match", "game_speed_match_desc"),
    SUDOKU("SUDOKU", "game_sudoku", "game_sudoku_desc"),
    CAT_SUDOKU("CAT_SUDOKU", "game_cat_sudoku", "game_cat_sudoku_desc"),
    TURTLE_SOUP("TURTLE_SOUP", "game_turtle_soup", "game_turtle_soup_desc"),
    AVATAR_WHACK("AVATAR_WHACK", "game_avatar_whack", "game_avatar_whack_desc"),
    STROOP_EFFECT("STROOP_EFFECT", "game_stroop_effect", "game_stroop_effect_desc"),
    BLOCK_PUZZLE("BLOCK_PUZZLE", "game_block_puzzle", "game_block_puzzle_desc");

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
