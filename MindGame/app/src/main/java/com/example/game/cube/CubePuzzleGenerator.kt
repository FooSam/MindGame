package com.example.game.cube

import androidx.compose.ui.graphics.Color
import com.example.data.model.GameDifficulty
import java.util.Random

/**
 * 54 格保證有解拼塊分割生成器
 */
object CubePuzzleGenerator {

    // 晶瑩水晶配色調色盤
    private val CRYSTAL_PALETTE = listOf(
        Color(0xFF38BDF8), // 晴空藍 (Sky)
        Color(0xFFF43F5E), // 寶石紅 (Ruby)
        Color(0xFF10B981), // 祖母綠 (Emerald)
        Color(0xFFF59E0B), // 琥珀黃 (Amber)
        Color(0xFF8B5CF6), // 紫水晶 (Amethyst)
        Color(0xFFEC4899), // 碧璽粉 (Pink)
        Color(0xFF06B6D4), // 綠松石 (Turquoise)
        Color(0xFF84CC16), // 橄欖綠 (Peridot)
        Color(0xFF6366F1), // 靛青紫 (Indigo)
        Color(0xFFFB923C), // 琉璃橘 (Tangerine)
        Color(0xFF14B8A6), // 薄荷青 (Mint)
        Color(0xFFA855F7), // 羅蘭紫 (Violet)
        Color(0xFFE11D48), // 薔薇紅 (Rose)
        Color(0xFF0EA5E9), // 深海藍 (Ocean)
        Color(0xFFEAB308), // 金晶黃 (Gold)
        Color(0xFF22C55E), // 翡翠綠 (Jade)
        Color(0xFFD946EF), // 幻彩紫 (Fuchsia)
        Color(0xFFF97316)  // 烈陽橘 (Sun)
    )

    /**
     * 依據難度生成一組總格數精準為 54 格的待拼拼塊集合
     */
    fun generatePuzzle(difficulty: GameDifficulty, random: Random = Random()): List<PuzzlePiece> {
        val allPieces = mutableListOf<PuzzlePiece>()
        var pieceIdCounter = 1

        val shuffledPalette = CRYSTAL_PALETTE.shuffled(random)
        var colorIndex = 0

        // 6 個面，每個面獨立切分為 9 格的連通塊
        CubeFace.entries.forEach { _ ->
            val facePieces = partitionFace(difficulty, random)
            facePieces.forEach { cells ->
                val color = shuffledPalette[colorIndex % shuffledPalette.size]
                colorIndex++

                var piece = PuzzlePiece(
                    id = pieceIdCounter++,
                    cells = cells,
                    color = color
                )

                // 隨機旋轉 0 ~ 3 次 90 度
                val rotationSteps = random.nextInt(4)
                for (r in 0 until rotationSteps) {
                    piece = piece.rotatedClockwise()
                }

                allPieces.add(piece)
            }
        }

        // 打亂順序放入底欄待拼盤
        allPieces.shuffle(random)
        return allPieces
    }

    /**
     * 將單一面（3x3，9 格）分割為符合難度限制的連通拼塊
     * 回傳各拼塊之正規化座標列表
     */
    fun partitionFace(difficulty: GameDifficulty, random: Random): List<List<CellPos>> {
        // 根據難度決定該面 9 格的目標塊大小分組（加總必為 9）
        val targetSizes = getPartitionSizes(difficulty, random)

        // 嘗試劃分 3x3 網格
        for (attempt in 0 until 100) {
            val result = tryPartition(targetSizes, random)
            if (result != null) {
                return result
            }
        }

        // 備用安全預設分割：[3, 3, 3] 橫條或豎條，確保 100% 成功
        return listOf(
            listOf(CellPos(0, 0), CellPos(0, 1), CellPos(0, 2)),
            listOf(CellPos(1, 0), CellPos(1, 1), CellPos(1, 2)),
            listOf(CellPos(2, 0), CellPos(2, 1), CellPos(2, 2))
        )
    }

    /**
     * 依照難度取得加總為 9 的區塊大小配方
     */
    private fun getPartitionSizes(difficulty: GameDifficulty, random: Random): List<Int> {
        return when (difficulty) {
            GameDifficulty.BEGINNER -> {
                // 初級：最多 4 格，以 2 格、3 格為主
                val options = listOf(
                    listOf(3, 3, 3),
                    listOf(4, 3, 2),
                    listOf(3, 2, 2, 2),
                    listOf(3, 3, 2, 1),
                    listOf(4, 2, 2, 1)
                )
                options[random.nextInt(options.size)]
            }
            GameDifficulty.INTERMEDIATE -> {
                // 中級：最多 5 格，以 3 格、4 格為主
                val options = listOf(
                    listOf(5, 4),
                    listOf(4, 5),
                    listOf(4, 3, 2),
                    listOf(3, 3, 3),
                    listOf(4, 4, 1),
                    listOf(5, 2, 2)
                )
                options[random.nextInt(options.size)]
            }
            else -> {
                // 高級 / 最高難度：最多 5 格，包含更多 5 格、4 格異形拼塊
                val options = listOf(
                    listOf(5, 4),
                    listOf(4, 5),
                    listOf(5, 3, 1),
                    listOf(4, 3, 2),
                    listOf(5, 2, 2)
                )
                options[random.nextInt(options.size)]
            }
        }
    }

    /**
     * 嘗試將 3x3 網格分割為指定大小列表的連通塊
     */
    private fun tryPartition(targetSizes: List<Int>, random: Random): List<List<CellPos>>? {
        val grid = Array(3) { BooleanArray(3) } // false = 未佔用
        val pieces = mutableListOf<List<CellPos>>()

        // 將大小由大到小排序，提升生長成功率
        val sortedSizes = targetSizes.sortedDescending()

        for (targetSize in sortedSizes) {
            val emptyCells = mutableListOf<CellPos>()
            for (r in 0..2) {
                for (c in 0..2) {
                    if (!grid[r][c]) emptyCells.add(CellPos(r, c))
                }
            }
            if (emptyCells.size < targetSize) return null

            // 隨機選一個未佔用格子作為種子
            val startCell = emptyCells[random.nextInt(emptyCells.size)]
            val pieceCells = mutableListOf(startCell)
            grid[startCell.row][startCell.col] = true

            // 隨機 BFS / 鄰居生長到 targetSize
            while (pieceCells.size < targetSize) {
                val candidates = mutableListOf<CellPos>()
                for (cell in pieceCells) {
                    for (dir in listOf(CellPos(-1, 0), CellPos(1, 0), CellPos(0, -1), CellPos(0, 1))) {
                        val nr = cell.row + dir.row
                        val nc = cell.col + dir.col
                        if (nr in 0..2 && nc in 0..2 && !grid[nr][nc]) {
                            val candidate = CellPos(nr, nc)
                            if (!candidates.contains(candidate)) {
                                candidates.add(candidate)
                            }
                        }
                    }
                }

                if (candidates.isEmpty()) {
                    // 無法繼續生長，本次嘗試失敗
                    return null
                }

                val nextCell = candidates[random.nextInt(candidates.size)]
                grid[nextCell.row][nextCell.col] = true
                pieceCells.add(nextCell)
            }

            // 正規化至 (0,0)
            val minR = pieceCells.minOf { it.row }
            val minC = pieceCells.minOf { it.col }
            val normalized = pieceCells.map { CellPos(it.row - minR, it.col - minC) }
                .sortedWith(compareBy({ it.row }, { it.col }))
            pieces.add(normalized)
        }

        return pieces
    }
}
