package com.example.game.cattrap

/**
 * 尖頂六角蜂巢軸向座標系統 (Pointy-Topped Axial Coordinates)
 * q: 水平列 (Column), r: 垂直行 (Row)
 */
data class HexCoord(val q: Int, val r: Int) {
    /**
     * 獲取六角相鄰 6 方向座標
     * 尖頂六角格 (pointy-topped) 奇偶行交錯相鄰位移：
     * 若 r 是偶數 vs 奇數，相鄰偏移量有所不同（以 staggered offset 轉換為軸向相鄰）
     */
    fun getNeighbors(gridSize: Int = 11): List<HexCoord> {
        val isEvenRow = (r % 2 == 0)
        val deltas = if (isEvenRow) {
            listOf(
                Pair(-1, 0),  // 左
                Pair(1, 0),   // 右
                Pair(-1, -1), // 左上
                Pair(0, -1),  // 右上
                Pair(-1, 1),  // 左下
                Pair(0, 1)    // 右下
            )
        } else {
            listOf(
                Pair(-1, 0),  // 左
                Pair(1, 0),   // 右
                Pair(0, -1),  // 左上
                Pair(1, -1),  // 右上
                Pair(0, 1),   // 左下
                Pair(1, 1)    // 右下
            )
        }

        return deltas.map { (dq, dr) -> HexCoord(q + dq, r + dr) }
            .filter { it.q in 0 until gridSize && it.r in 0 until gridSize }
    }

    /**
     * 是否為地圖最外圈邊緣
     */
    fun isEdge(gridSize: Int = 11): Boolean {
        return q == 0 || q == gridSize - 1 || r == 0 || r == gridSize - 1
    }
}

/**
 * 地塊類型
 */
enum class HexCellType {
    EMPTY,       // 正常可走、可落子之浮板
    WATER_POND,  // 初始障礙（水塘/深淵/融冰），不可通行亦不可落子
    BLOCK        // 玩家放置的路障/毛線球/封鎖石
}

/**
 * 貓咪表情與姿態
 */
enum class CatEmotion {
    IDLE,     // 正常待機，搖擺尾巴
    LEAP,     // 躍遷移動中
    ALERT,    // 警覺炸毛 (剩餘逃脫路徑 <= 2)
    TRAPPED,  // 被完全包圍認輸 (舉白旗/貓球)
    ESCAPED   // 逃脫邊界成功
}

/**
 * 貓咪品系外觀
 */
enum class CatBreed(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val nameJa: String,
    val nameKo: String,
    val primaryColorHex: Long,
    val stripeColorHex: Long
) {
    ORANGE_TABBY("orange_tabby", "貪吃橘貓", "Orange Tabby", "茶トラ", "치즈태비", 0xFFFFA726, 0xFFE65100),
    CALICO("calico", "調皮三花", "Calico", "三毛猫", "삼색이", 0xFFEEEEEE, 0xFFE65100),
    BLACK_CAT("black_cat", "神秘黑貓", "Midnight Black", "黒猫", "올블랙", 0xFF37474F, 0xFF212121),
    TUXEDO("tuxedo", "紳士賓士", "Tuxedo", "ハチワレ", "턱시도", 0xFF455A64, 0xFFECEFF1),
    WHITE_SNOW("white_snow", "優雅白貓", "Snowy White", "白猫", "터키시앙고라", 0xFFFFFFFF, 0xFFB0BEC5);

    fun getName(lang: String): String {
        return when (lang) {
            "en" -> nameEn
            "ja" -> nameJa
            "ko" -> nameKo
            else -> nameZh
        }
    }
}

/**
 * 益智輔助道具
 */
enum class CatTrapTool(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val iconEmoji: String
) {
    FREEZE("freeze", "cattrap_tool_freeze", "cattrap_tool_freeze_desc", "🧊"),
    DOUBLE_BLOCK("double_block", "cattrap_tool_double", "cattrap_tool_double_desc", "🧱"),
    RADAR("radar", "cattrap_tool_radar", "cattrap_tool_radar_desc", "🧭")
}

/**
 * 3 星通關評級標準
 */
data class StarGoal(
    val targetSteps: Int,   // 2 星標準（目標步數內獲勝）
    val extremeSteps: Int   // 3 星標準（極限步數內獲勝）
)

/**
 * 關卡資料模型
 */
data class CatTrapLevel(
    val levelId: Int,
    val nameKey: String,
    val initialPonds: Set<HexCoord>,
    val startCatPos: HexCoord = HexCoord(5, 5),
    val starGoal: StarGoal
)

/**
 * 遊戲模式
 */
sealed class CatTrapMode {
    data class LevelMode(val levelId: Int) : CatTrapMode()
    data class RandomMode(val pondCount: Int = 10) : CatTrapMode()
}

/**
 * 世界觀與企劃助手規劃之 25 大漸進關卡靜態資料庫
 */
object CatTrapLevels {
    const val TOTAL_LEVELS = 60
    const val GRID_SIZE = 11

    val levels: List<CatTrapLevel> = listOf(
        // 1~20 關：初級・庭園初遇（天然水塘豐富 12~15 個，外圍天然屏障多、隘口狹小）
        CatTrapLevel(1, "cattrap_lvl_1", setOf(HexCoord(1, 1), HexCoord(1, 6), HexCoord(2, 5), HexCoord(3, 1), HexCoord(3, 3), HexCoord(4, 6), HexCoord(6, 4), HexCoord(6, 5), HexCoord(7, 5), HexCoord(8, 5), HexCoord(8, 7), HexCoord(8, 9), HexCoord(9, 4)), HexCoord(5, 5), StarGoal(21, 16)),
        CatTrapLevel(2, "cattrap_lvl_2", setOf(HexCoord(1, 4), HexCoord(3, 3), HexCoord(3, 7), HexCoord(4, 2), HexCoord(4, 7), HexCoord(6, 8), HexCoord(6, 9), HexCoord(7, 6), HexCoord(8, 1), HexCoord(8, 4), HexCoord(8, 7), HexCoord(9, 2), HexCoord(9, 5)), HexCoord(5, 5), StarGoal(18, 12)),
        CatTrapLevel(3, "cattrap_lvl_3", setOf(HexCoord(1, 2), HexCoord(1, 3), HexCoord(2, 3), HexCoord(3, 7), HexCoord(4, 3), HexCoord(4, 7), HexCoord(5, 1), HexCoord(6, 2), HexCoord(7, 6), HexCoord(7, 8), HexCoord(8, 5), HexCoord(9, 6), HexCoord(9, 8)), HexCoord(5, 5), StarGoal(20, 13)),
        CatTrapLevel(4, "cattrap_lvl_4", setOf(HexCoord(1, 2), HexCoord(2, 7), HexCoord(2, 9), HexCoord(3, 1), HexCoord(3, 4), HexCoord(4, 3), HexCoord(4, 5), HexCoord(4, 8), HexCoord(5, 8), HexCoord(6, 2), HexCoord(6, 4), HexCoord(8, 3), HexCoord(9, 6)), HexCoord(5, 5), StarGoal(24, 17)),
        CatTrapLevel(5, "cattrap_lvl_5", setOf(HexCoord(1, 4), HexCoord(2, 3), HexCoord(2, 9), HexCoord(3, 1), HexCoord(3, 2), HexCoord(3, 4), HexCoord(3, 5), HexCoord(4, 1), HexCoord(4, 8), HexCoord(6, 2), HexCoord(6, 9), HexCoord(7, 5), HexCoord(8, 5), HexCoord(9, 2), HexCoord(9, 4)), HexCoord(5, 5), StarGoal(22, 17)),
        CatTrapLevel(6, "cattrap_lvl_6", setOf(HexCoord(1, 2), HexCoord(1, 6), HexCoord(2, 4), HexCoord(2, 5), HexCoord(3, 3), HexCoord(3, 4), HexCoord(5, 9), HexCoord(7, 7), HexCoord(8, 4), HexCoord(8, 6), HexCoord(8, 7), HexCoord(8, 9)), HexCoord(5, 5), StarGoal(19, 12)),
        CatTrapLevel(7, "cattrap_lvl_7", setOf(HexCoord(1, 2), HexCoord(1, 4), HexCoord(2, 4), HexCoord(2, 7), HexCoord(3, 2), HexCoord(3, 5), HexCoord(3, 8), HexCoord(5, 3), HexCoord(7, 1), HexCoord(7, 3), HexCoord(7, 4), HexCoord(8, 6), HexCoord(8, 8)), HexCoord(5, 5), StarGoal(23, 16)),
        CatTrapLevel(8, "cattrap_lvl_8", setOf(HexCoord(1, 8), HexCoord(2, 6), HexCoord(3, 1), HexCoord(3, 5), HexCoord(4, 4), HexCoord(4, 8), HexCoord(5, 6), HexCoord(5, 9), HexCoord(6, 2), HexCoord(6, 5), HexCoord(6, 7), HexCoord(7, 7), HexCoord(7, 8), HexCoord(9, 8)), HexCoord(5, 5), StarGoal(18, 13)),
        CatTrapLevel(9, "cattrap_lvl_9", setOf(HexCoord(1, 6), HexCoord(1, 7), HexCoord(1, 9), HexCoord(2, 3), HexCoord(3, 9), HexCoord(4, 9), HexCoord(5, 4), HexCoord(6, 8), HexCoord(7, 6), HexCoord(8, 3), HexCoord(8, 5), HexCoord(8, 8), HexCoord(9, 5)), HexCoord(5, 5), StarGoal(21, 16)),
        CatTrapLevel(10, "cattrap_lvl_10", setOf(HexCoord(1, 1), HexCoord(1, 4), HexCoord(1, 9), HexCoord(2, 8), HexCoord(3, 2), HexCoord(3, 5), HexCoord(3, 8), HexCoord(4, 1), HexCoord(4, 2), HexCoord(7, 8), HexCoord(8, 5), HexCoord(9, 2)), HexCoord(5, 5), StarGoal(22, 15)),
        CatTrapLevel(11, "cattrap_lvl_11", setOf(HexCoord(1, 1), HexCoord(3, 2), HexCoord(3, 4), HexCoord(5, 1), HexCoord(5, 4), HexCoord(6, 5), HexCoord(6, 7), HexCoord(6, 8), HexCoord(7, 5), HexCoord(8, 1), HexCoord(8, 3), HexCoord(8, 6), HexCoord(8, 8)), HexCoord(5, 5), StarGoal(23, 18)),
        CatTrapLevel(12, "cattrap_lvl_12", setOf(HexCoord(1, 1), HexCoord(1, 2), HexCoord(1, 4), HexCoord(2, 5), HexCoord(2, 7), HexCoord(2, 9), HexCoord(4, 5), HexCoord(5, 2), HexCoord(5, 9), HexCoord(7, 3), HexCoord(7, 4), HexCoord(9, 3)), HexCoord(5, 5), StarGoal(22, 16)),
        CatTrapLevel(13, "cattrap_lvl_13", setOf(HexCoord(1, 7), HexCoord(3, 1), HexCoord(3, 6), HexCoord(4, 6), HexCoord(4, 7), HexCoord(4, 9), HexCoord(5, 8), HexCoord(7, 9), HexCoord(8, 2), HexCoord(8, 4), HexCoord(8, 5), HexCoord(9, 1)), HexCoord(5, 5), StarGoal(20, 13)),
        CatTrapLevel(14, "cattrap_lvl_14", setOf(HexCoord(1, 7), HexCoord(3, 3), HexCoord(4, 6), HexCoord(4, 7), HexCoord(5, 1), HexCoord(5, 3), HexCoord(5, 8), HexCoord(7, 2), HexCoord(7, 8), HexCoord(8, 3), HexCoord(8, 5), HexCoord(9, 1)), HexCoord(5, 5), StarGoal(20, 14)),
        CatTrapLevel(15, "cattrap_lvl_15", setOf(HexCoord(1, 2), HexCoord(3, 5), HexCoord(3, 7), HexCoord(4, 1), HexCoord(4, 5), HexCoord(4, 6), HexCoord(5, 3), HexCoord(5, 8), HexCoord(7, 4), HexCoord(8, 8), HexCoord(9, 5), HexCoord(9, 8)), HexCoord(5, 5), StarGoal(21, 16)),
        CatTrapLevel(16, "cattrap_lvl_16", setOf(HexCoord(1, 6), HexCoord(3, 2), HexCoord(3, 9), HexCoord(4, 5), HexCoord(4, 6), HexCoord(5, 2), HexCoord(5, 9), HexCoord(6, 3), HexCoord(6, 7), HexCoord(7, 1), HexCoord(7, 2), HexCoord(7, 3), HexCoord(7, 8), HexCoord(9, 3), HexCoord(9, 6)), HexCoord(5, 5), StarGoal(19, 14)),
        CatTrapLevel(17, "cattrap_lvl_17", setOf(HexCoord(1, 3), HexCoord(1, 4), HexCoord(1, 6), HexCoord(1, 7), HexCoord(2, 3), HexCoord(2, 8), HexCoord(3, 7), HexCoord(4, 8), HexCoord(5, 6), HexCoord(5, 9), HexCoord(7, 4), HexCoord(7, 7), HexCoord(8, 1), HexCoord(9, 3)), HexCoord(5, 5), StarGoal(20, 13)),
        CatTrapLevel(18, "cattrap_lvl_18", setOf(HexCoord(2, 2), HexCoord(2, 4), HexCoord(4, 5), HexCoord(5, 8), HexCoord(6, 5), HexCoord(7, 5), HexCoord(7, 6), HexCoord(8, 1), HexCoord(8, 3), HexCoord(9, 5), HexCoord(9, 6), HexCoord(9, 8)), HexCoord(5, 5), StarGoal(22, 16)),
        CatTrapLevel(19, "cattrap_lvl_19", setOf(HexCoord(1, 5), HexCoord(3, 6), HexCoord(4, 1), HexCoord(4, 5), HexCoord(4, 6), HexCoord(5, 1), HexCoord(5, 7), HexCoord(5, 9), HexCoord(6, 2), HexCoord(6, 3), HexCoord(6, 6), HexCoord(6, 7), HexCoord(7, 6), HexCoord(8, 9), HexCoord(9, 3)), HexCoord(5, 5), StarGoal(19, 13)),
        CatTrapLevel(20, "cattrap_lvl_20", setOf(HexCoord(1, 2), HexCoord(1, 5), HexCoord(2, 2), HexCoord(2, 8), HexCoord(3, 1), HexCoord(3, 6), HexCoord(4, 2), HexCoord(6, 4), HexCoord(6, 5), HexCoord(8, 2), HexCoord(9, 2), HexCoord(9, 5)), HexCoord(5, 5), StarGoal(24, 19)),
        // 21~40 關：中級・九曲迷冰（天然水塘適中 8~11 個，障礙分佈於峽灣外緣，考驗多向攔截）
        CatTrapLevel(21, "cattrap_lvl_21", setOf(HexCoord(0, 6), HexCoord(1, 0), HexCoord(1, 1), HexCoord(1, 9), HexCoord(3, 4), HexCoord(9, 0), HexCoord(9, 5), HexCoord(9, 9)), HexCoord(5, 5), StarGoal(32, 23)),
        CatTrapLevel(22, "cattrap_lvl_22", setOf(HexCoord(1, 7), HexCoord(2, 1), HexCoord(2, 5), HexCoord(2, 10), HexCoord(3, 7), HexCoord(5, 10), HexCoord(6, 10), HexCoord(7, 8), HexCoord(8, 4)), HexCoord(5, 5), StarGoal(34, 25)),
        CatTrapLevel(23, "cattrap_lvl_23", setOf(HexCoord(0, 9), HexCoord(1, 5), HexCoord(2, 3), HexCoord(2, 7), HexCoord(2, 8), HexCoord(4, 1), HexCoord(7, 1), HexCoord(10, 3), HexCoord(10, 7)), HexCoord(5, 5), StarGoal(33, 26)),
        CatTrapLevel(24, "cattrap_lvl_24", setOf(HexCoord(0, 1), HexCoord(0, 9), HexCoord(2, 8), HexCoord(2, 9), HexCoord(3, 1), HexCoord(7, 3), HexCoord(8, 3), HexCoord(10, 6)), HexCoord(5, 5), StarGoal(34, 27)),
        CatTrapLevel(25, "cattrap_lvl_25", setOf(HexCoord(0, 7), HexCoord(1, 5), HexCoord(2, 9), HexCoord(3, 9), HexCoord(4, 2), HexCoord(5, 6), HexCoord(6, 1), HexCoord(6, 10), HexCoord(9, 1)), HexCoord(5, 5), StarGoal(26, 19)),
        CatTrapLevel(26, "cattrap_lvl_26", setOf(HexCoord(0, 8), HexCoord(3, 2), HexCoord(5, 9), HexCoord(6, 9), HexCoord(7, 10), HexCoord(8, 6), HexCoord(9, 4), HexCoord(9, 9), HexCoord(10, 3)), HexCoord(5, 5), StarGoal(29, 20)),
        CatTrapLevel(27, "cattrap_lvl_27", setOf(HexCoord(0, 7), HexCoord(1, 1), HexCoord(1, 8), HexCoord(2, 4), HexCoord(2, 10), HexCoord(4, 1), HexCoord(5, 4), HexCoord(7, 6), HexCoord(10, 2)), HexCoord(5, 5), StarGoal(33, 25)),
        CatTrapLevel(28, "cattrap_lvl_28", setOf(HexCoord(0, 8), HexCoord(1, 7), HexCoord(3, 3), HexCoord(3, 7), HexCoord(3, 8), HexCoord(4, 10), HexCoord(8, 2), HexCoord(9, 2), HexCoord(10, 2)), HexCoord(5, 5), StarGoal(31, 21)),
        CatTrapLevel(29, "cattrap_lvl_29", setOf(HexCoord(1, 2), HexCoord(2, 4), HexCoord(4, 1), HexCoord(4, 5), HexCoord(7, 4), HexCoord(8, 1), HexCoord(8, 8), HexCoord(10, 3)), HexCoord(5, 5), StarGoal(28, 20)),
        CatTrapLevel(30, "cattrap_lvl_30", setOf(HexCoord(2, 8), HexCoord(5, 8), HexCoord(7, 0), HexCoord(7, 4), HexCoord(9, 4), HexCoord(9, 10), HexCoord(10, 3), HexCoord(10, 8)), HexCoord(5, 5), StarGoal(30, 22)),
        CatTrapLevel(31, "cattrap_lvl_31", setOf(HexCoord(1, 7), HexCoord(3, 9), HexCoord(6, 0), HexCoord(6, 8), HexCoord(7, 8), HexCoord(7, 9), HexCoord(9, 3), HexCoord(9, 8), HexCoord(10, 4)), HexCoord(5, 5), StarGoal(26, 18)),
        CatTrapLevel(32, "cattrap_lvl_32", setOf(HexCoord(3, 5), HexCoord(6, 0), HexCoord(6, 3), HexCoord(6, 6), HexCoord(6, 7), HexCoord(7, 4), HexCoord(7, 6), HexCoord(9, 6), HexCoord(10, 8)), HexCoord(5, 5), StarGoal(27, 19)),
        CatTrapLevel(33, "cattrap_lvl_33", setOf(HexCoord(0, 7), HexCoord(4, 1), HexCoord(4, 10), HexCoord(5, 10), HexCoord(6, 5), HexCoord(6, 7), HexCoord(7, 4), HexCoord(9, 6), HexCoord(10, 9)), HexCoord(5, 5), StarGoal(28, 20)),
        CatTrapLevel(34, "cattrap_lvl_34", setOf(HexCoord(0, 9), HexCoord(1, 10), HexCoord(3, 10), HexCoord(5, 2), HexCoord(5, 8), HexCoord(6, 8), HexCoord(6, 9), HexCoord(9, 10), HexCoord(10, 5)), HexCoord(5, 5), StarGoal(27, 17)),
        CatTrapLevel(35, "cattrap_lvl_35", setOf(HexCoord(0, 2), HexCoord(2, 5), HexCoord(2, 10), HexCoord(4, 0), HexCoord(4, 8), HexCoord(5, 10), HexCoord(6, 3), HexCoord(7, 0), HexCoord(7, 4)), HexCoord(5, 5), StarGoal(31, 21)),
        CatTrapLevel(36, "cattrap_lvl_36", setOf(HexCoord(2, 8), HexCoord(3, 0), HexCoord(4, 8), HexCoord(6, 2), HexCoord(8, 3), HexCoord(9, 3), HexCoord(10, 4), HexCoord(10, 8)), HexCoord(5, 5), StarGoal(29, 19)),
        CatTrapLevel(37, "cattrap_lvl_37", setOf(HexCoord(0, 3), HexCoord(1, 4), HexCoord(2, 3), HexCoord(3, 1), HexCoord(5, 6), HexCoord(5, 10), HexCoord(8, 0), HexCoord(8, 6), HexCoord(8, 9), HexCoord(10, 3)), HexCoord(5, 5), StarGoal(32, 24)),
        CatTrapLevel(38, "cattrap_lvl_38", setOf(HexCoord(1, 5), HexCoord(2, 6), HexCoord(2, 8), HexCoord(3, 0), HexCoord(3, 3), HexCoord(3, 8), HexCoord(5, 1), HexCoord(7, 3), HexCoord(7, 10)), HexCoord(5, 5), StarGoal(26, 18)),
        CatTrapLevel(39, "cattrap_lvl_39", setOf(HexCoord(0, 2), HexCoord(2, 9), HexCoord(3, 0), HexCoord(5, 0), HexCoord(5, 2), HexCoord(5, 3), HexCoord(6, 3), HexCoord(8, 9), HexCoord(9, 5)), HexCoord(5, 5), StarGoal(28, 21)),
        CatTrapLevel(40, "cattrap_lvl_40", setOf(HexCoord(1, 10), HexCoord(3, 8), HexCoord(4, 9), HexCoord(7, 0), HexCoord(8, 2), HexCoord(8, 5), HexCoord(10, 3), HexCoord(10, 9)), HexCoord(5, 5), StarGoal(33, 24)),
        // 41~60 關：高級・絕境圍城（天然水塘稀疏 4~7 個，貓咪起手四通八達，極度考驗步數算力與道具）
        CatTrapLevel(41, "cattrap_lvl_41", setOf(HexCoord(1, 4), HexCoord(2, 6), HexCoord(3, 5), HexCoord(3, 6), HexCoord(4, 3), HexCoord(4, 7), HexCoord(9, 5)), HexCoord(5, 5), StarGoal(36, 26)),
        CatTrapLevel(42, "cattrap_lvl_42", setOf(HexCoord(1, 1), HexCoord(1, 2), HexCoord(1, 8), HexCoord(3, 2), HexCoord(5, 9), HexCoord(6, 2), HexCoord(9, 1)), HexCoord(5, 5), StarGoal(44, 33)),
        CatTrapLevel(43, "cattrap_lvl_43", setOf(HexCoord(1, 6), HexCoord(2, 5), HexCoord(6, 1), HexCoord(6, 2), HexCoord(9, 9)), HexCoord(5, 5), StarGoal(43, 35)),
        CatTrapLevel(44, "cattrap_lvl_44", setOf(HexCoord(1, 4), HexCoord(1, 6), HexCoord(2, 9), HexCoord(6, 8), HexCoord(8, 4), HexCoord(8, 5), HexCoord(8, 9)), HexCoord(5, 5), StarGoal(43, 31)),
        CatTrapLevel(45, "cattrap_lvl_45", setOf(HexCoord(4, 8), HexCoord(7, 2), HexCoord(7, 5), HexCoord(8, 3), HexCoord(8, 5), HexCoord(8, 7), HexCoord(9, 7)), HexCoord(5, 5), StarGoal(41, 29)),
        CatTrapLevel(46, "cattrap_lvl_46", setOf(HexCoord(3, 3), HexCoord(4, 8), HexCoord(7, 9), HexCoord(9, 3)), HexCoord(5, 5), StarGoal(41, 32)),
        CatTrapLevel(47, "cattrap_lvl_47", setOf(HexCoord(1, 2), HexCoord(1, 6), HexCoord(2, 4), HexCoord(2, 8), HexCoord(4, 2), HexCoord(9, 4)), HexCoord(5, 5), StarGoal(44, 33)),
        CatTrapLevel(48, "cattrap_lvl_48", setOf(HexCoord(1, 1), HexCoord(6, 9), HexCoord(7, 2), HexCoord(8, 6), HexCoord(8, 8)), HexCoord(5, 5), StarGoal(37, 25)),
        CatTrapLevel(49, "cattrap_lvl_49", setOf(HexCoord(3, 3), HexCoord(6, 8), HexCoord(7, 3), HexCoord(7, 4), HexCoord(8, 6), HexCoord(9, 1)), HexCoord(5, 5), StarGoal(40, 30)),
        CatTrapLevel(50, "cattrap_lvl_50", setOf(HexCoord(5, 3), HexCoord(5, 8), HexCoord(6, 3), HexCoord(7, 4), HexCoord(7, 7)), HexCoord(5, 5), StarGoal(40, 31)),
        CatTrapLevel(51, "cattrap_lvl_51", setOf(HexCoord(3, 4), HexCoord(3, 6), HexCoord(5, 8), HexCoord(7, 5), HexCoord(9, 6), HexCoord(9, 9)), HexCoord(5, 5), StarGoal(39, 29)),
        CatTrapLevel(52, "cattrap_lvl_52", setOf(HexCoord(1, 3), HexCoord(2, 1), HexCoord(2, 9), HexCoord(7, 2), HexCoord(7, 3)), HexCoord(5, 5), StarGoal(36, 25)),
        CatTrapLevel(53, "cattrap_lvl_53", setOf(HexCoord(6, 2), HexCoord(7, 3), HexCoord(7, 6), HexCoord(9, 3)), HexCoord(5, 5), StarGoal(38, 30)),
        CatTrapLevel(54, "cattrap_lvl_54", setOf(HexCoord(1, 9), HexCoord(3, 5), HexCoord(9, 1), HexCoord(9, 9)), HexCoord(5, 5), StarGoal(39, 31)),
        CatTrapLevel(55, "cattrap_lvl_55", setOf(HexCoord(1, 6), HexCoord(5, 2), HexCoord(8, 1), HexCoord(9, 2)), HexCoord(5, 5), StarGoal(43, 34)),
        CatTrapLevel(56, "cattrap_lvl_56", setOf(HexCoord(4, 7), HexCoord(6, 3), HexCoord(8, 4), HexCoord(8, 5)), HexCoord(5, 5), StarGoal(40, 31)),
        CatTrapLevel(57, "cattrap_lvl_57", setOf(HexCoord(1, 8), HexCoord(3, 2), HexCoord(5, 8), HexCoord(6, 1), HexCoord(6, 9), HexCoord(8, 5)), HexCoord(5, 5), StarGoal(41, 32)),
        CatTrapLevel(58, "cattrap_lvl_58", setOf(HexCoord(1, 7), HexCoord(5, 9), HexCoord(7, 3), HexCoord(8, 6), HexCoord(9, 3)), HexCoord(5, 5), StarGoal(40, 30)),
        CatTrapLevel(59, "cattrap_lvl_59", setOf(HexCoord(1, 2), HexCoord(1, 6), HexCoord(2, 3), HexCoord(5, 2)), HexCoord(5, 5), StarGoal(36, 25)),
        CatTrapLevel(60, "cattrap_lvl_60", setOf(HexCoord(3, 4), HexCoord(5, 9), HexCoord(8, 2), HexCoord(8, 8), HexCoord(9, 8)), HexCoord(5, 5), StarGoal(42, 32))
    )

    fun getLevel(levelId: Int): CatTrapLevel {
        return levels.find { it.levelId == levelId } ?: levels.first()
    }
}
