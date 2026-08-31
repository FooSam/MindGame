package com.example.game.whack

import com.example.R
import com.example.data.model.GameDifficulty

enum class AvatarType {
    NORMAL,
    BONUS,
    BOMB
}

enum class AvatarExpression {
    NORMAL,
    FAKE_OUT,
    HIT
}

data class CharacterAvatar(
    val id: String,
    val avatarIndex: Int, // 1..21
    val type: AvatarType = AvatarType.NORMAL,
    val expression: AvatarExpression = AvatarExpression.NORMAL
)

data class WhackHoleState(
    val holeIndex: Int,
    val avatar: CharacterAvatar? = null,
    val isPopping: Boolean = false,
    val isHit: Boolean = false,
    val isFakeOut: Boolean = false,
    val isDisappearing: Boolean = false,
    val spawnTimeMs: Long = 0L,
    val stayDurationMs: Long = 1000L
)

data class WhackDifficultyConfig(
    val difficulty: GameDifficulty,
    val rows: Int,
    val cols: Int,
    val totalHoles: Int,
    val stayDurationMs: Long,
    val fakeOutProb: Float,
    val bombProb: Float,
    val bonusProb: Float,
    val simultaneousTargets: Int,
    val spawnIntervalMs: Long,
    val gameDurationMs: Long = 45_000L
) {
    companion object {
        fun getConfig(difficulty: GameDifficulty): WhackDifficultyConfig {
            return when (difficulty) {
                GameDifficulty.BEGINNER -> WhackDifficultyConfig(
                    difficulty = difficulty,
                    rows = 2,
                    cols = 2,
                    totalHoles = 4,
                    stayDurationMs = 1500L,
                    fakeOutProb = 0.0f,
                    bombProb = 0.0f,
                    bonusProb = 0.05f,
                    simultaneousTargets = 1,
                    spawnIntervalMs = 1400L
                )
                GameDifficulty.INTERMEDIATE -> WhackDifficultyConfig(
                    difficulty = difficulty,
                    rows = 2,
                    cols = 3,
                    totalHoles = 6,
                    stayDurationMs = 1200L,
                    fakeOutProb = 0.10f,
                    bombProb = 0.0f,
                    bonusProb = 0.08f,
                    simultaneousTargets = 1,
                    spawnIntervalMs = 1100L
                )
                GameDifficulty.ADVANCED -> WhackDifficultyConfig(
                    difficulty = difficulty,
                    rows = 3,
                    cols = 3,
                    totalHoles = 9,
                    stayDurationMs = 900L,
                    fakeOutProb = 0.20f,
                    bombProb = 0.15f,
                    bonusProb = 0.10f,
                    simultaneousTargets = 1,
                    spawnIntervalMs = 850L
                )
                GameDifficulty.HARD -> WhackDifficultyConfig(
                    difficulty = difficulty,
                    rows = 3,
                    cols = 3,
                    totalHoles = 9,
                    stayDurationMs = 700L,
                    fakeOutProb = 0.30f,
                    bombProb = 0.20f,
                    bonusProb = 0.12f,
                    simultaneousTargets = 2,
                    spawnIntervalMs = 650L
                )
                GameDifficulty.HELL -> WhackDifficultyConfig(
                    difficulty = difficulty,
                    rows = 4,
                    cols = 3,
                    totalHoles = 12,
                    stayDurationMs = 500L,
                    fakeOutProb = 0.40f,
                    bombProb = 0.25f,
                    bonusProb = 0.15f,
                    simultaneousTargets = 2,
                    spawnIntervalMs = 450L
                )
                GameDifficulty.EPIC -> WhackDifficultyConfig(
                    difficulty = difficulty,
                    rows = 4,
                    cols = 3,
                    totalHoles = 12,
                    stayDurationMs = 350L,
                    fakeOutProb = 0.50f,
                    bombProb = 0.30f,
                    bonusProb = 0.20f,
                    simultaneousTargets = 3,
                    spawnIntervalMs = 320L
                )
            }
        }
    }
}

object AvatarCatalog {
    const val TOTAL_AVATARS = 21

    // Mapping of Avatar 01..21 normal, fake, hit Drawables
    private val normalDrawables = listOf(
        R.drawable.avatar_01_normal, R.drawable.avatar_02_normal, R.drawable.avatar_03_normal,
        R.drawable.avatar_04_normal, R.drawable.avatar_05_normal, R.drawable.avatar_06_normal,
        R.drawable.avatar_07_normal, R.drawable.avatar_08_normal, R.drawable.avatar_09_normal,
        R.drawable.avatar_10_normal, R.drawable.avatar_11_normal, R.drawable.avatar_12_normal,
        R.drawable.avatar_13_normal, R.drawable.avatar_14_normal, R.drawable.avatar_15_normal,
        R.drawable.avatar_16_normal, R.drawable.avatar_17_normal, R.drawable.avatar_18_normal,
        R.drawable.avatar_19_normal, R.drawable.avatar_20_normal, R.drawable.avatar_21_normal
    )

    private val fakeDrawables = listOf(
        R.drawable.avatar_01_fake, R.drawable.avatar_02_fake, R.drawable.avatar_03_fake,
        R.drawable.avatar_04_fake, R.drawable.avatar_05_fake, R.drawable.avatar_06_fake,
        R.drawable.avatar_07_fake, R.drawable.avatar_08_fake, R.drawable.avatar_09_fake,
        R.drawable.avatar_10_fake, R.drawable.avatar_11_fake, R.drawable.avatar_12_fake,
        R.drawable.avatar_13_fake, R.drawable.avatar_14_fake, R.drawable.avatar_15_fake,
        R.drawable.avatar_16_fake, R.drawable.avatar_17_fake, R.drawable.avatar_18_fake,
        R.drawable.avatar_19_fake, R.drawable.avatar_20_fake, R.drawable.avatar_21_fake
    )

    private val hitDrawables = listOf(
        R.drawable.avatar_01_hit, R.drawable.avatar_02_hit, R.drawable.avatar_03_hit,
        R.drawable.avatar_04_hit, R.drawable.avatar_05_hit, R.drawable.avatar_06_hit,
        R.drawable.avatar_07_hit, R.drawable.avatar_08_hit, R.drawable.avatar_09_hit,
        R.drawable.avatar_10_hit, R.drawable.avatar_11_hit, R.drawable.avatar_12_hit,
        R.drawable.avatar_13_hit, R.drawable.avatar_14_hit, R.drawable.avatar_15_hit,
        R.drawable.avatar_16_hit, R.drawable.avatar_17_hit, R.drawable.avatar_18_hit,
        R.drawable.avatar_19_hit, R.drawable.avatar_20_hit, R.drawable.avatar_21_hit
    )

    fun getDrawableRes(avatarIndex: Int, expression: AvatarExpression, type: AvatarType): Int {
        if (type == AvatarType.BOMB) {
            return R.drawable.avatar_bomb
        }
        if (type == AvatarType.BONUS) {
            return R.drawable.avatar_bonus
        }

        val safeIdx = (avatarIndex - 1).coerceIn(0, TOTAL_AVATARS - 1)
        return when (expression) {
            AvatarExpression.FAKE_OUT -> fakeDrawables[safeIdx]
            AvatarExpression.HIT -> hitDrawables[safeIdx]
            AvatarExpression.NORMAL -> normalDrawables[safeIdx]
        }
    }

    fun getRandomAvatarIndex(): Int {
        return (1..TOTAL_AVATARS).random()
    }
}
