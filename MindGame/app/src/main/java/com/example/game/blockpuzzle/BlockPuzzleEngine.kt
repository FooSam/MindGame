package com.example.game.blockpuzzle

import kotlin.random.Random

/**
 * 代表一個拼圖方塊形狀
 * coords: 方塊內的相對座標 (row, col)，左上角對齊 (0, 0)
 * colorIndex: 陶土顏色索引 (0..5)
 */
data class BlockShape(
    val id: String,
    val coords: List<Pair<Int, Int>>,
    val colorIndex: Int = 0
) {
    val width: Int get() = (coords.maxOfOrNull { it.second } ?: 0) + 1
    val height: Int get() = (coords.maxOfOrNull { it.first } ?: 0) + 1
    val cellCount: Int get() = coords.size

    /**
     * 順時針旋轉 90 度演算法
     * 原始 (r, c) 旋轉後為 (c, -r)，最後正規化使 minRow = 0, minCol = 0
     */
    fun rotateClockwise(): BlockShape {
        val rotated = coords.map { (r, c) -> Pair(c, -r) }
        val minR = rotated.minOf { it.first }
        val minC = rotated.minOf { it.second }
        val normalized = rotated.map { (r, c) -> Pair(r - minR, c - minC) }.sortedWith(compareBy({ it.first }, { it.second }))
        return copy(coords = normalized)
    }
}

/**
 * 消除判定結果
 */
data class EliminationResult(
    val fullRows: List<Int>,
    val fullCols: List<Int>,
    val crossPoints: List<Pair<Int, Int>>,
    val clearedCellCount: Int,
    val scoreEarned: Int
)

object BlockPuzzleEngine {
    const val BOARD_SIZE = 8

    // 預定義豐富的經典幾何形狀庫
    private val SHAPE_TEMPLATES = listOf(
        // 1. 單格
        listOf(Pair(0, 0)),
        // 2. 2格直線 (橫/直)
        listOf(Pair(0, 0), Pair(0, 1)),
        listOf(Pair(0, 0), Pair(1, 0)),
        // 3. 3格直線
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0)),
        // 4. 4格直線
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0)),
        // 5. 5格直線
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(4, 0)),
        // 6. 2x2 正方形
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)),
        // 7. 3x3 大正方形 (高難度但大面積)
        listOf(
            Pair(0, 0), Pair(0, 1), Pair(0, 2),
            Pair(1, 0), Pair(1, 1), Pair(1, 2),
            Pair(2, 0), Pair(2, 1), Pair(2, 2)
        ),
        // 8. 2x2 拐角 (L 3格)
        listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(1, 0), Pair(1, 1), Pair(0, 1)),
        // 9. L 型 4格
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(2, 1)),
        listOf(Pair(0, 1), Pair(1, 1), Pair(2, 1), Pair(2, 0)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 0)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 2)),
        // 10. T 型
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 1)),
        listOf(Pair(1, 0), Pair(1, 1), Pair(1, 2), Pair(0, 1)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(1, 1)),
        // 11. Z / S 型
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1), Pair(1, 2)),
        listOf(Pair(1, 0), Pair(1, 1), Pair(0, 1), Pair(0, 2))
    )

    /**
     * 生成一輪 3 個隨機方塊
     */
    fun generateThreePieces(): List<BlockShape> {
        return List(3) { index ->
            val template = SHAPE_TEMPLATES.random()
            val colorIndex = Random.nextInt(0, 6)
            BlockShape(
                id = "piece_${System.currentTimeMillis()}_$index",
                coords = template,
                colorIndex = colorIndex
            )
        }
    }

    /**
     * 驗證 shape 是否可放置在盤面的 (startRow, startCol)
     */
    fun canPlace(shape: BlockShape, startRow: Int, startCol: Int, board: Array<IntArray>): Boolean {
        for ((r, c) in shape.coords) {
            val targetR = startRow + r
            val targetC = startCol + c
            if (targetR !in 0 until BOARD_SIZE || targetC !in 0 until BOARD_SIZE) {
                return false
            }
            if (board[targetR][targetC] != 0) {
                return false
            }
        }
        return true
    }

    /**
     * 放置方塊到盤面上
     */
    fun placeShape(shape: BlockShape, startRow: Int, startCol: Int, board: Array<IntArray>) {
        for ((r, c) in shape.coords) {
            board[startRow + r][startCol + c] = shape.colorIndex + 1
        }
    }

    /**
     * 檢查全盤滿行滿列消除
     */
    fun checkEliminations(board: Array<IntArray>, currentCombo: Int): EliminationResult {
        val fullRows = mutableListOf<Int>()
        val fullCols = mutableListOf<Int>()

        // 檢查橫行
        for (r in 0 until BOARD_SIZE) {
            if ((0 until BOARD_SIZE).all { c -> board[r][c] != 0 }) {
                fullRows.add(r)
            }
        }

        // 檢查直行
        for (c in 0 until BOARD_SIZE) {
            if ((0 until BOARD_SIZE).all { r -> board[r][c] != 0 }) {
                fullCols.add(c)
            }
        }

        // 找出十字交叉交會點
        val crossPoints = mutableListOf<Pair<Int, Int>>()
        for (r in fullRows) {
            for (c in fullCols) {
                crossPoints.add(Pair(r, c))
            }
        }

        val totalLines = fullRows.size + fullCols.size
        if (totalLines == 0) {
            return EliminationResult(emptyList(), emptyList(), emptyList(), 0, 0)
        }

        // 計算被消除的獨立格子數
        val clearedCoords = mutableSetOf<Pair<Int, Int>>()
        for (r in fullRows) {
            for (c in 0 until BOARD_SIZE) {
                clearedCoords.add(Pair(r, c))
            }
        }
        for (c in fullCols) {
            for (r in 0 until BOARD_SIZE) {
                clearedCoords.add(Pair(r, c))
            }
        }

        // 計分規則：
        // 基礎消線分：每條線 100 分
        // 多線加成：2條 x 1.5倍, 3條 x 2倍...
        // 連擊加成：combo * 50
        // 十字爆破獎勵：每個交會點額外 +200 分！
        val baseLineScore = totalLines * 100
        val multiLineBonus = if (totalLines > 1) (totalLines - 1) * 80 else 0
        val comboBonus = (currentCombo + 1) * 50
        val crossBonus = crossPoints.size * 200

        val totalScore = baseLineScore + multiLineBonus + comboBonus + crossBonus

        return EliminationResult(
            fullRows = fullRows,
            fullCols = fullCols,
            crossPoints = crossPoints,
            clearedCellCount = clearedCoords.size,
            scoreEarned = totalScore
        )
    }

    /**
     * 清除盤面上的指定行與列
     */
    fun clearLines(board: Array<IntArray>, rows: List<Int>, cols: List<Int>) {
        for (r in rows) {
            for (c in 0 until BOARD_SIZE) {
                board[r][c] = 0
            }
        }
        for (c in cols) {
            for (r in 0 until BOARD_SIZE) {
                board[r][c] = 0
            }
        }
    }

    /**
     * 檢查剩餘的方塊中，是否「至少有一個方塊可在盤面上的某處放置」
     * 若任何剩餘方塊都無法擺入盤面，則判定為 Game Over
     */
    fun isGameOver(pieces: List<BlockShape?>, board: Array<IntArray>): Boolean {
        val activePieces = pieces.filterNotNull()
        if (activePieces.isEmpty()) return false

        for (piece in activePieces) {
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    if (canPlace(piece, r, c, board)) {
                        return false // 只要有一個地方放得下，就還沒結束！
                    }
                }
            }
        }
        return true // 所有的備選方塊在所有 64 個位置都放不下 -> 遊戲結束
    }
}
