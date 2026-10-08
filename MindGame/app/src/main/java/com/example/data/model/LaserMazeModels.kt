package com.example.data.model

import java.util.UUID

/**
 * 雷射方向列舉
 */
enum class LaserDirection(val dx: Int, val dy: Int, val degrees: Float) {
    UP(0, -1, 0f),
    RIGHT(1, 0, 90f),
    DOWN(0, 1, 180f),
    LEFT(-1, 0, 270f);

    fun opposite(): LaserDirection = when (this) {
        UP -> DOWN
        RIGHT -> LEFT
        DOWN -> UP
        LEFT -> RIGHT
    }

    fun rotateClockwise(): LaserDirection = when (this) {
        UP -> RIGHT
        RIGHT -> DOWN
        DOWN -> LEFT
        LEFT -> UP
    }

    fun rotate(steps: Int): LaserDirection {
        val s = (steps % 4 + 4) % 4
        var dir = this
        repeat(s) { dir = dir.rotateClockwise() }
        return dir
    }
}

/**
 * 光學元件類型
 */
enum class OpticalPieceType(val titleKey: String) {
    EMPTY("piece_empty"),
    EMITTER("piece_emitter"),
    RECEIVER("piece_receiver"),
    MIRROR_SINGLE("piece_mirror_single"),
    MIRROR_DOUBLE("piece_mirror_double"),
    BEAM_SPLITTER("piece_beam_splitter"),
    TUNNEL("piece_tunnel"),
    OBSTACLE_ABSORBER("piece_obstacle_absorber"),
    OBSTACLE_FIXED_MIRROR("piece_obstacle_fixed_mirror"),
    OBSTACLE_TINTED_GLASS("piece_obstacle_tinted_glass"),
    // 【多維度光學擴充進階機關】
    FILTER_RED("piece_filter_red"),
    FILTER_GREEN("piece_filter_green"),
    FILTER_BLUE("piece_filter_blue"),
    PRISM_SYNTHESIZER("piece_prism_synthesizer"),
    PORTAL("piece_portal")
}

/**
 * 棋盤上的光學方塊實體
 */
data class LaserPiece(
    val id: String = UUID.randomUUID().toString(),
    val type: OpticalPieceType,
    val rotation: Int = 0, // 0: 0°, 1: 90°, 2: 180°, 3: 270°
    val isFixed: Boolean = false, // 是否不可旋轉 (發射器、接收水晶、固定障礙石等)
    val isDecoy: Boolean = false, // 是否為煙霧彈干擾鏡片 (增加推理深度)
    val colorHex: Long = 0xFF00E5FF, // 預設高科技青光
    val portalTarget: Pair<Int, Int>? = null, // 若為傳送門，記錄躍遷目標座標
    val requiredColorHex: Long? = null // 若為多波長共振水晶或特定元件，記錄指定波長顏色
) {
    fun rotateClockwise(): LaserPiece {
        if (isFixed) return this
        return copy(rotation = (rotation + 1) % 4)
    }
}

/**
 * 雷射光束線段 (供渲染引擎繪製發光光束)
 */
data class LaserRaySegment(
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
    val direction: LaserDirection,
    val colorHex: Long = 0xFF00E5FF
)

/**
 * 棋盤狀態
 */
data class LaserBoardState(
    val gridSize: Int,
    val grid: List<List<LaserPiece?>>,
    val emitterRow: Int,
    val emitterCol: Int,
    val emitterDir: LaserDirection,
    val receivers: List<Pair<Int, Int>>, // row, col
    val segments: List<LaserRaySegment> = emptyList(),
    val litReceiverIndices: Set<Int> = emptySet(),
    val isCleared: Boolean = false,
    val moveCount: Int = 0,
    val receiverColors: Map<Pair<Int, Int>, Long> = emptyMap() // 接收水晶所屬波長需求
)

/**
 * 難度與關卡配置
 */
data class LaserLevelConfig(
    val difficulty: GameDifficulty,
    val levelIndex: Int, // 1..10
    val gridSize: Int,
    val targetReceivers: Int,
    val minBounces: Int,
    val decoyCount: Int,
    val allowSplitter: Boolean,
    val allowTunnel: Boolean,
    val obstacleCount: Int,
    val allowFilters: Boolean = false,
    val allowSynthesizer: Boolean = false,
    val allowPortals: Boolean = false,
    val allowResonance: Boolean = false
) {
    companion object {
        fun getConfig(difficulty: GameDifficulty, levelIndex: Int): LaserLevelConfig {
            val clampedLevel = levelIndex.coerceIn(1, 10)
            return when (difficulty) {
                GameDifficulty.BEGINNER -> LaserLevelConfig(
                    difficulty = difficulty,
                    levelIndex = clampedLevel,
                    gridSize = 5, // 初級 5x5
                    targetReceivers = 1,
                    minBounces = 2 + (clampedLevel / 4), // 2~4 次轉折
                    decoyCount = if (clampedLevel >= 5) 1 else 0,
                    allowSplitter = false,
                    allowTunnel = false,
                    obstacleCount = if (clampedLevel >= 7) 1 else 0,
                    allowFilters = false,
                    allowSynthesizer = false,
                    allowPortals = false,
                    allowResonance = false
                )
                GameDifficulty.INTERMEDIATE -> LaserLevelConfig(
                    difficulty = difficulty,
                    levelIndex = clampedLevel,
                    gridSize = 6, // 中級 6x6
                    targetReceivers = if (clampedLevel >= 6) 2 else 1,
                    minBounces = 3 + (clampedLevel / 3), // 3~6 次轉折
                    decoyCount = 1 + (clampedLevel / 4), // 1~3 個干擾鏡片
                    allowSplitter = false,
                    allowTunnel = false,
                    obstacleCount = 1 + (clampedLevel / 3), // 1~4 個障礙物
                    allowFilters = clampedLevel >= 5, // 5關起引入濾光鏡
                    allowSynthesizer = false,
                    allowPortals = false,
                    allowResonance = clampedLevel >= 5
                )
                GameDifficulty.ADVANCED -> LaserLevelConfig(
                    difficulty = difficulty,
                    levelIndex = clampedLevel,
                    gridSize = 8, // 高級 8x8
                    targetReceivers = 2,
                    minBounces = 4 + (clampedLevel / 3),
                    decoyCount = 2 + (clampedLevel / 3),
                    allowSplitter = true, // 引入十字分光鏡
                    allowTunnel = false,
                    obstacleCount = 2 + (clampedLevel / 3),
                    allowFilters = true,
                    allowSynthesizer = clampedLevel >= 5, // 引入混光合成
                    allowPortals = false,
                    allowResonance = true
                )
                GameDifficulty.HARD -> LaserLevelConfig(
                    difficulty = difficulty,
                    levelIndex = clampedLevel,
                    gridSize = 9, // 困難級 9x9
                    targetReceivers = if (clampedLevel >= 7) 3 else 2,
                    minBounces = 5 + (clampedLevel / 3),
                    decoyCount = 3 + (clampedLevel / 3),
                    allowSplitter = true,
                    allowTunnel = true, // 引入立體隧道格
                    obstacleCount = 3 + (clampedLevel / 3),
                    allowFilters = true,
                    allowSynthesizer = true,
                    allowPortals = clampedLevel >= 4, // 引入時空傳送門
                    allowResonance = true
                )
                GameDifficulty.HELL -> LaserLevelConfig(
                    difficulty = difficulty,
                    levelIndex = clampedLevel,
                    gridSize = 10, // 地獄級 10x10
                    targetReceivers = 3,
                    minBounces = 6 + (clampedLevel / 3),
                    decoyCount = 4 + (clampedLevel / 3),
                    allowSplitter = true,
                    allowTunnel = true,
                    obstacleCount = 4 + (clampedLevel / 3),
                    allowFilters = true,
                    allowSynthesizer = true,
                    allowPortals = true,
                    allowResonance = true
                )
                GameDifficulty.EPIC -> LaserLevelConfig(
                    difficulty = difficulty,
                    levelIndex = clampedLevel,
                    gridSize = 12, // 史詩級 12x12
                    targetReceivers = if (clampedLevel >= 6) 4 else 3,
                    minBounces = 7 + (clampedLevel / 3),
                    decoyCount = 5 + (clampedLevel / 3),
                    allowSplitter = true,
                    allowTunnel = true,
                    obstacleCount = 5 + (clampedLevel / 3),
                    allowFilters = true,
                    allowSynthesizer = true,
                    allowPortals = true,
                    allowResonance = true
                )
            }
        }
    }
}
