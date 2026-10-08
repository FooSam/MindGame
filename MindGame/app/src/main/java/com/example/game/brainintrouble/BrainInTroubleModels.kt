package com.example.game.brainintrouble

import androidx.compose.ui.graphics.Color
import com.example.data.model.GameDifficulty

/**
 * 幾何形狀列舉（左腦處理：空間幾何形狀認知）
 */
enum class BrainShape(val key: String, val displayNameZh: String, val displayNameEn: String) {
    CIRCLE("CIRCLE", "圓形", "Circle"),
    SQUARE("SQUARE", "正方形", "Square"),
    TRIANGLE("TRIANGLE", "三角形", "Triangle"),
    DIAMOND("DIAMOND", "菱形", "Diamond"),
    STAR("STAR", "星形", "Star"),
    HEXAGON("HEXAGON", "六角形", "Hexagon")
}

/**
 * 顏色列舉（右腦處理：色彩直覺與抗干擾）
 */
enum class BrainColor(
    val key: String,
    val color: Color,
    val displayNameZh: String,
    val displayNameEn: String
) {
    RED("RED", Color(0xFFFF3B30), "紅", "Red"),
    BLUE("BLUE", Color(0xFF007AFF), "藍", "Blue"),
    GREEN("GREEN", Color(0xFF34C759), "綠", "Green"),
    YELLOW("YELLOW", Color(0xFFFFCC00), "黃", "Yellow"),
    PURPLE("PURPLE", Color(0xFFAF52DE), "紫", "Purple"),
    ORANGE("ORANGE", Color(0xFFFF9500), "橙", "Orange")
}

/**
 * 難度參數配置
 */
data class BrainInTroubleConfig(
    val difficulty: GameDifficulty,
    val leftShapes: List<BrainShape>,
    val rightColors: List<BrainColor>,
    val useTextOnColor: Boolean,       // 困難級：開始出現文字
    val isStroopConflict: Boolean,     // 地獄/史詩級：文字與顏色衝突 (Stroop 效應)
    val fallDurationMs: Long,          // 掉落全程耗時 (毫秒)
    val spawnIntervalMs: Long          // 生成間隔 (毫秒)
) {
    companion object {
        fun of(difficulty: GameDifficulty): BrainInTroubleConfig {
            return when (difficulty) {
                GameDifficulty.BEGINNER -> BrainInTroubleConfig(
                    difficulty = difficulty,
                    leftShapes = listOf(BrainShape.CIRCLE, BrainShape.SQUARE),
                    rightColors = listOf(BrainColor.RED, BrainColor.BLUE),
                    useTextOnColor = false,
                    isStroopConflict = false,
                    fallDurationMs = 4500L,
                    spawnIntervalMs = 1400L
                )
                GameDifficulty.INTERMEDIATE -> BrainInTroubleConfig(
                    difficulty = difficulty,
                    leftShapes = listOf(BrainShape.CIRCLE, BrainShape.SQUARE, BrainShape.TRIANGLE),
                    rightColors = listOf(BrainColor.RED, BrainColor.BLUE, BrainColor.GREEN),
                    useTextOnColor = false,
                    isStroopConflict = false,
                    fallDurationMs = 3800L,
                    spawnIntervalMs = 1100L
                )
                GameDifficulty.ADVANCED -> BrainInTroubleConfig(
                    difficulty = difficulty,
                    leftShapes = listOf(BrainShape.CIRCLE, BrainShape.SQUARE, BrainShape.TRIANGLE, BrainShape.DIAMOND),
                    rightColors = listOf(BrainColor.RED, BrainColor.BLUE, BrainColor.GREEN, BrainColor.YELLOW),
                    useTextOnColor = false,
                    isStroopConflict = false,
                    fallDurationMs = 3200L,
                    spawnIntervalMs = 850L
                )
                GameDifficulty.HARD -> BrainInTroubleConfig(
                    difficulty = difficulty,
                    leftShapes = listOf(
                        BrainShape.CIRCLE, BrainShape.SQUARE, BrainShape.TRIANGLE,
                        BrainShape.DIAMOND, BrainShape.STAR
                    ),
                    rightColors = listOf(
                        BrainColor.RED, BrainColor.BLUE, BrainColor.GREEN,
                        BrainColor.YELLOW, BrainColor.PURPLE
                    ),
                    useTextOnColor = true,      // 色字一致
                    isStroopConflict = false,
                    fallDurationMs = 2600L,
                    spawnIntervalMs = 700L
                )
                GameDifficulty.HELL -> BrainInTroubleConfig(
                    difficulty = difficulty,
                    leftShapes = listOf(
                        BrainShape.CIRCLE, BrainShape.SQUARE, BrainShape.TRIANGLE,
                        BrainShape.DIAMOND, BrainShape.STAR
                    ),
                    rightColors = listOf(
                        BrainColor.RED, BrainColor.BLUE, BrainColor.GREEN,
                        BrainColor.YELLOW, BrainColor.PURPLE
                    ),
                    useTextOnColor = true,
                    isStroopConflict = true,    // 色字衝突！
                    fallDurationMs = 2200L,
                    spawnIntervalMs = 580L
                )
                GameDifficulty.EPIC -> BrainInTroubleConfig(
                    difficulty = difficulty,
                    leftShapes = listOf(
                        BrainShape.CIRCLE, BrainShape.SQUARE, BrainShape.TRIANGLE,
                        BrainShape.DIAMOND, BrainShape.STAR, BrainShape.HEXAGON
                    ),
                    rightColors = listOf(
                        BrainColor.RED, BrainColor.BLUE, BrainColor.GREEN,
                        BrainColor.YELLOW, BrainColor.PURPLE, BrainColor.ORANGE
                    ),
                    useTextOnColor = true,
                    isStroopConflict = true,    // 色字衝突！
                    fallDurationMs = 1800L,
                    spawnIntervalMs = 480L
                )
            }
        }
    }
}

/**
 * 掉落物模型
 */
data class DropItem(
    val id: Long,
    val isLeftSide: Boolean,           // true: 左軌道, false: 右軌道
    val laneIndex: Int = 1,            // 0..2: 該側內部對應之跑道 (左/中/右)
    val shape: BrainShape? = null,     // 左側形狀
    val targetColor: BrainColor? = null, // 右側目標判定顏色 (玩家必須點擊這個顏色按鈕才能消除)
    val textLabel: String? = null,     // 右側文字內容 (例如 "紅", "藍")
    val visualColor: BrainColor? = null, // 右側文字/圖案的視覺顏色 (玩家肉眼看到的顏色，即 targetColor)
    var progress: Float = 0f,          // 0.0f (頂端) -> 1.0f (觸碰底線)
    var isEliminated: Boolean = false,
    var isMissed: Boolean = false
)

/**
 * 粒子動態模型
 */
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    var alpha: Float = 1f,
    var life: Float = 1f
)

/**
 * 掉落物隨機生成器
 */
object BrainInTroubleGenerator {
    private var nextId = 1L
    private var lastLeftLane = -1
    private var lastRightLane = -1

    fun resetId() {
        nextId = 1L
        lastLeftLane = -1
        lastRightLane = -1
    }

    /**
     * 生成一個掉落物（交替或隨機決定左軌或右軌，並自動分流至 3 大獨立跑道）
     */
    fun createDropItem(config: BrainInTroubleConfig, forceSide: Boolean? = null): DropItem {
        val isLeft = forceSide ?: (Math.random() < 0.5)
        val id = nextId++

        return if (isLeft) {
            val randomShape = config.leftShapes.random()
            val availableLanes = listOf(0, 1, 2).filter { it != lastLeftLane }
            val laneIndex = availableLanes.random()
            lastLeftLane = laneIndex

            DropItem(
                id = id,
                isLeftSide = true,
                laneIndex = laneIndex,
                shape = randomShape,
                progress = 0f
            )
        } else {
            val targetColor = config.rightColors.random()
            val availableLanes = listOf(0, 1, 2).filter { it != lastRightLane }
            val laneIndex = availableLanes.random()
            lastRightLane = laneIndex

            val textLabel = if (config.useTextOnColor) {
                if (config.isStroopConflict && Math.random() < 0.8) {
                    // 80% 機率製造 Stroop 衝突文字：字義 ≠ 字色
                    val otherColors = config.rightColors.filter { it != targetColor }
                    if (otherColors.isNotEmpty()) {
                        otherColors.random().displayNameZh
                    } else {
                        targetColor.displayNameZh
                    }
                } else {
                    targetColor.displayNameZh
                }
            } else {
                null
            }

            DropItem(
                id = id,
                isLeftSide = false,
                laneIndex = laneIndex,
                targetColor = targetColor,
                textLabel = textLabel,
                visualColor = targetColor,
                progress = 0f
            )
        }
    }
}
