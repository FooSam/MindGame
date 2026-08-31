package com.example.game.stroop

import androidx.compose.ui.graphics.Color
import com.example.data.model.GameDifficulty

enum class StroopColor(
    val key: String,
    val zhName: String,
    val enName: String,
    val color: Color
) {
    RED("color_red", "紅", "Red", Color(0xFFEF4444)),
    BLUE("color_blue", "藍", "Blue", Color(0xFF3B82F6)),
    GREEN("color_green", "綠", "Green", Color(0xFF10B981)),
    YELLOW("color_yellow", "黃", "Yellow", Color(0xFFEAB308)),
    PURPLE("color_purple", "紫", "Purple", Color(0xFF8B5CF6)),
    ORANGE("color_orange", "橙", "Orange", Color(0xFFF97316))
}

enum class StroopShape(
    val key: String,
    val zhName: String,
    val enName: String
) {
    CIRCLE("shape_circle", "圓形", "Circle"),
    SQUARE("shape_square", "方形", "Square"),
    TRIANGLE("shape_triangle", "三角形", "Triangle"),
    STAR("shape_star", "星形", "Star")
}

enum class StroopInstruction(val key: String) {
    CHOOSE_COLOR("stroop_inst_choose_color"),          // 請選文字顯示的【顏色】
    CHOOSE_TEXT("stroop_inst_choose_text"),            // 請選文字本身的【字義】
    COMPARE_EQUAL("stroop_inst_compare_equal"),        // 左邊字義 是否等於 右邊顏色？
    DISTRACTED_CHOOSE("stroop_inst_distracted"),       // 請選上方文字的【顏色】(注意選項干擾)
    DYNAMIC_FAST("stroop_inst_dynamic_fast"),          // 極速辨識文字【顏色】(動態旋轉干擾)
    COMPOSITE_NOT("stroop_inst_composite_not")         // 複合指令：排除指定條件
}

data class StroopOption(
    val id: String,
    val labelZh: String,
    val labelEn: String,
    val displayColor: Color = Color.Unspecified,
    val bgColor: Color = Color.Unspecified,
    val isCorrect: Boolean = false,
    val associatedColor: StroopColor? = null
)

data class StroopQuestion(
    val difficulty: GameDifficulty,
    val instruction: StroopInstruction,
    // For single target
    val mainWord: StroopColor,
    val displayColor: StroopColor,
    // For shape
    val shape: StroopShape? = null,
    val notConditionColor: StroopColor? = null,
    // For compare
    val leftWordMeaning: StroopColor? = null,
    val leftDisplayColor: StroopColor? = null,
    val rightWordMeaning: StroopColor? = null,
    val rightDisplayColor: StroopColor? = null,
    // Options
    val isYesNoMode: Boolean = false,
    val isYesCorrect: Boolean = false,
    val options: List<StroopOption> = emptyList(),
    val timeLimitMs: Long = 45_000L
)

object StroopGenerator {

    private val basic4Colors = listOf(StroopColor.RED, StroopColor.BLUE, StroopColor.GREEN, StroopColor.YELLOW)
    private val all6Colors = StroopColor.entries

    fun generateQuestion(difficulty: GameDifficulty): StroopQuestion {
        return when (difficulty) {
            GameDifficulty.BEGINNER -> generateBeginnerQuestion()
            GameDifficulty.INTERMEDIATE -> generateIntermediateQuestion()
            GameDifficulty.ADVANCED -> generateAdvancedQuestion()
            GameDifficulty.HARD -> generateHardQuestion()
            GameDifficulty.HELL -> generateHellQuestion()
            GameDifficulty.EPIC -> generateEpicQuestion()
        }
    }

    // 1. 初級：經典字色辨識 (4基本色，選字色)
    private fun generateBeginnerQuestion(): StroopQuestion {
        val wordMeaning = basic4Colors.random()
        val displayColor = (basic4Colors.filter { it != wordMeaning }).random()

        val options = basic4Colors.shuffled().map { color ->
            StroopOption(
                id = color.key,
                labelZh = color.zhName,
                labelEn = color.enName,
                displayColor = Color.White,
                bgColor = color.color,
                isCorrect = (color == displayColor),
                associatedColor = color
            )
        }

        return StroopQuestion(
            difficulty = GameDifficulty.BEGINNER,
            instruction = StroopInstruction.CHOOSE_COLOR,
            mainWord = wordMeaning,
            displayColor = displayColor,
            options = options,
            timeLimitMs = 3000L
        )
    }

    // 2. 中級：雙向指令切換 (隨機選字義或選顏色)
    private fun generateIntermediateQuestion(): StroopQuestion {
        val isChooseMeaning = listOf(true, false).random()
        val wordMeaning = all6Colors.random()
        val displayColor = (all6Colors.filter { it != wordMeaning }).random()

        val targetColor = if (isChooseMeaning) wordMeaning else displayColor
        val otherCandidates = all6Colors.filter { it != targetColor }.shuffled().take(3)
        val selectedColors = (listOf(targetColor) + otherCandidates).shuffled()

        val options = selectedColors.map { color ->
            StroopOption(
                id = color.key,
                labelZh = color.zhName,
                labelEn = color.enName,
                displayColor = Color.White,
                bgColor = color.color,
                isCorrect = (color == targetColor),
                associatedColor = color
            )
        }

        return StroopQuestion(
            difficulty = GameDifficulty.INTERMEDIATE,
            instruction = if (isChooseMeaning) StroopInstruction.CHOOSE_TEXT else StroopInstruction.CHOOSE_COLOR,
            mainWord = wordMeaning,
            displayColor = displayColor,
            options = options,
            timeLimitMs = 2500L
        )
    }

    // 3. 高級：左右雙字比對 (左字義 == 右顏色？ 是/否 快速二選一)
    private fun generateAdvancedQuestion(): StroopQuestion {
        val leftMeaning = all6Colors.random()
        val leftDisplay = (all6Colors.filter { it != leftMeaning }).random()

        val shouldBeEqual = listOf(true, false).random()
        val rightDisplay = if (shouldBeEqual) {
            leftMeaning
        } else {
            (all6Colors.filter { it != leftMeaning }).random()
        }
        val rightMeaning = (all6Colors.filter { it != rightDisplay }).random()

        val isActuallyEqual = (leftMeaning == rightDisplay)

        val options = listOf(
            StroopOption(id = "YES", labelZh = "是 (YES)", labelEn = "YES", isCorrect = isActuallyEqual),
            StroopOption(id = "NO", labelZh = "否 (NO)", labelEn = "NO", isCorrect = !isActuallyEqual)
        )

        return StroopQuestion(
            difficulty = GameDifficulty.ADVANCED,
            instruction = StroopInstruction.COMPARE_EQUAL,
            mainWord = leftMeaning,
            displayColor = leftDisplay,
            leftWordMeaning = leftMeaning,
            leftDisplayColor = leftDisplay,
            rightWordMeaning = rightMeaning,
            rightDisplayColor = rightDisplay,
            isYesNoMode = true,
            isYesCorrect = isActuallyEqual,
            options = options,
            timeLimitMs = 2000L
        )
    }

    // 4. 困難級：干擾按鈕模式 (按鈕自身的文字與顏色也是衝突的)
    private fun generateHardQuestion(): StroopQuestion {
        val wordMeaning = all6Colors.random()
        val displayColor = (all6Colors.filter { it != wordMeaning }).random()
        val targetColor = displayColor // 目標是選上方字顯示的顏色

        // 產生 4 個干擾按鈕：每個按鈕的文字和背景色都不同
        val candidateColors = (listOf(targetColor) + (all6Colors.filter { it != targetColor }).shuffled().take(3)).shuffled()

        val options = candidateColors.map { color ->
            val isTarget = (color == targetColor)
            // 按鈕文字是 color 的名稱，但按鈕背景色隨機挑選另一種顏色來干擾
            val conflictBg = (all6Colors.filter { it != color }).random()
            StroopOption(
                id = color.key,
                labelZh = color.zhName,
                labelEn = color.enName,
                displayColor = Color.White,
                bgColor = conflictBg.color,
                isCorrect = isTarget,
                associatedColor = color
            )
        }

        return StroopQuestion(
            difficulty = GameDifficulty.HARD,
            instruction = StroopInstruction.DISTRACTED_CHOOSE,
            mainWord = wordMeaning,
            displayColor = displayColor,
            options = options,
            timeLimitMs = 1800L
        )
    }

    // 5. 地獄級：動態計時閃爍 (限時 1.5s，旋轉閃爍干擾)
    private fun generateHellQuestion(): StroopQuestion {
        val isChooseMeaning = listOf(true, false).random()
        val wordMeaning = all6Colors.random()
        val displayColor = (all6Colors.filter { it != wordMeaning }).random()
        val targetColor = if (isChooseMeaning) wordMeaning else displayColor

        val candidateColors = (listOf(targetColor) + (all6Colors.filter { it != targetColor }).shuffled().take(3)).shuffled()

        val options = candidateColors.map { color ->
            val conflictBg = (all6Colors.filter { it != color }).random()
            StroopOption(
                id = color.key,
                labelZh = color.zhName,
                labelEn = color.enName,
                displayColor = Color.White,
                bgColor = conflictBg.color,
                isCorrect = (color == targetColor),
                associatedColor = color
            )
        }

        return StroopQuestion(
            difficulty = GameDifficulty.HELL,
            instruction = if (isChooseMeaning) StroopInstruction.CHOOSE_TEXT else StroopInstruction.DYNAMIC_FAST,
            mainWord = wordMeaning,
            displayColor = displayColor,
            options = options,
            timeLimitMs = 1500L
        )
    }

    // 6. 史詩級：複合多重認知 (形狀 + 否定句，限時 1.2s)
    private fun generateEpicQuestion(): StroopQuestion {
        val shape = StroopShape.entries.random()
        val wordMeaning = all6Colors.random()
        val displayColor = (all6Colors.filter { it != wordMeaning }).random()
        
        // 否定句指令：例如「不是 [notColor] 且非文字字義」
        val notColor = (all6Colors.filter { it != displayColor && it != wordMeaning }).random()
        // 正確答案是 displayColor
        val targetColor = displayColor

        val candidateColors = (listOf(targetColor) + (all6Colors.filter { it != targetColor && it != notColor }).shuffled().take(3)).shuffled()

        val options = candidateColors.map { color ->
            val conflictBg = (all6Colors.filter { it != color }).random()
            StroopOption(
                id = color.key,
                labelZh = color.zhName,
                labelEn = color.enName,
                displayColor = Color.White,
                bgColor = conflictBg.color,
                isCorrect = (color == targetColor),
                associatedColor = color
            )
        }

        return StroopQuestion(
            difficulty = GameDifficulty.EPIC,
            instruction = StroopInstruction.COMPOSITE_NOT,
            mainWord = wordMeaning,
            displayColor = displayColor,
            shape = shape,
            notConditionColor = notColor,
            options = options,
            timeLimitMs = 1200L
        )
    }
}
