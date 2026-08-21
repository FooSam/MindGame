package com.example.game.catsudoku

import com.example.data.model.GameDifficulty
import kotlin.random.Random

enum class CatCellState {
    EMPTY,
    CROSS,
    CAT
}

data class CatCell(
    val row: Int,
    val col: Int,
    val regionId: Int,
    val state: CatCellState = CatCellState.EMPTY,
    val isConflict: Boolean = false
)

data class CatSudokuPuzzle(
    val size: Int,
    val grid: List<CatCell>,
    val solutionQueens: List<Pair<Int, Int>>
)

object CatSudokuGenerator {

    /**
     * 生成指定難度的貓咪數獨（Queens 謎題）
     * 難易度對應：
     * BEGINNER: 4x4, 4隻貓
     * INTERMEDIATE: 5x5, 5隻貓
     * ADVANCED: 6x6, 6隻貓
     * HARD: 7x7, 7隻貓
     * HELL: 8x8, 8隻貓
     * EPIC: 9x9, 9隻貓
     */
    fun getSizeForDifficulty(difficulty: GameDifficulty): Int {
        return when (difficulty) {
            GameDifficulty.BEGINNER -> 4
            GameDifficulty.INTERMEDIATE -> 5
            GameDifficulty.ADVANCED -> 6
            GameDifficulty.HARD -> 7
            GameDifficulty.HELL -> 8
            GameDifficulty.EPIC -> 9
        }
    }

    fun generatePuzzle(difficulty: GameDifficulty): CatSudokuPuzzle {
        val size = getSizeForDifficulty(difficulty)
        
        // 1. 尋找一組保證無衝突的皇后位置 (N 隻貓，每行每列恰一隻，且 8 方向不相鄰)
        val solution = findValidQueensPlacement(size)
        
        // 2. 以這 N 個皇后為種子，將 N x N 網格劃分成 N 個連通區域 (每個區域恰含一隻貓)
        val regions = generateRegions(size, solution)
        
        val cells = mutableListOf<CatCell>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                val regId = regions[r][c]
                cells.add(CatCell(row = r, col = c, regionId = regId, state = CatCellState.EMPTY))
            }
        }
        
        return CatSudokuPuzzle(
            size = size,
            grid = cells,
            solutionQueens = solution
        )
    }

    /**
     * 使用隨機化回溯演算法尋找一組合法皇后解
     * 規則：每行 1 隻、每列 1 隻、周圍 8 格不相鄰
     */
    private fun findValidQueensPlacement(size: Int): List<Pair<Int, Int>> {
        val maxAttempts = 100
        for (attempt in 0 until maxAttempts) {
            val placement = IntArray(size) { -1 }
            if (solveQueens(0, size, placement)) {
                return placement.mapIndexed { row, col -> Pair(row, col) }
            }
        }
        // 理論上 size 4..9 在回溯下皆有解，若極罕見失敗則提供對應預設備案
        return fallbackQueens(size)
    }

    private fun solveQueens(row: Int, size: Int, placement: IntArray): Boolean {
        if (row == size) return true

        val cols = (0 until size).toList().shuffled()
        for (col in cols) {
            if (isValidPlacement(row, col, placement)) {
                placement[row] = col
                if (solveQueens(row + 1, size, placement)) {
                    return true
                }
                placement[row] = -1
            }
        }
        return false
    }

    private fun isValidPlacement(row: Int, col: Int, placement: IntArray): Boolean {
        for (r in 0 until row) {
            val c = placement[r]
            // 同列衝突
            if (c == col) return false
            // 周圍 8 格相鄰 (包含對角線距離為 1)
            if (kotlin.math.abs(r - row) <= 1 && kotlin.math.abs(c - col) <= 1) {
                return false
            }
        }
        return true
    }

    private fun fallbackQueens(size: Int): List<Pair<Int, Int>> {
        return when (size) {
            4 -> listOf(Pair(0, 1), Pair(1, 3), Pair(2, 0), Pair(3, 2))
            5 -> listOf(Pair(0, 0), Pair(1, 2), Pair(2, 4), Pair(3, 1), Pair(4, 3))
            6 -> listOf(Pair(0, 1), Pair(1, 3), Pair(2, 5), Pair(3, 0), Pair(4, 2), Pair(5, 4))
            7 -> listOf(Pair(0, 0), Pair(1, 2), Pair(2, 4), Pair(3, 6), Pair(4, 1), Pair(5, 3), Pair(6, 5))
            8 -> listOf(Pair(0, 1), Pair(1, 3), Pair(2, 5), Pair(3, 7), Pair(4, 0), Pair(5, 2), Pair(6, 4), Pair(7, 6))
            else -> listOf(Pair(0, 0), Pair(1, 2), Pair(2, 4), Pair(3, 6), Pair(4, 8), Pair(5, 1), Pair(6, 3), Pair(7, 5), Pair(8, 7))
        }
    }

    /**
     * 隨機生成 N 個連通區域，每個區域恰好包含一隻解中的皇后
     */
    private fun generateRegions(size: Int, solutionQueens: List<Pair<Int, Int>>): Array<IntArray> {
        val grid = Array(size) { IntArray(size) { -1 } }
        
        // 初始種子點：每隻皇后指定為區域 i
        solutionQueens.forEachIndexed { regionId, (r, c) ->
            grid[r][c] = regionId
        }

        // 隨機生長種子佇列
        val directions = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        
        // 使用多源隨機擴展直到所有格子都被填充
        var unfilled = size * size - size
        var loopCount = 0

        while (unfilled > 0 && loopCount < 1000) {
            loopCount++
            // 找到所有與已填格子相鄰的未填格子候選 (r, c, candidateRegionIds)
            val frontier = mutableListOf<Pair<Pair<Int, Int>, Int>>()
            
            for (r in 0 until size) {
                for (c in 0 until size) {
                    if (grid[r][c] != -1) {
                        val currentReg = grid[r][c]
                        for ((dr, dc) in directions) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until size && nc in 0 until size && grid[nr][nc] == -1) {
                                frontier.add(Pair(Pair(nr, nc), currentReg))
                            }
                        }
                    }
                }
            }

            if (frontier.isEmpty()) break

            frontier.shuffle()
            for ((pos, regId) in frontier) {
                val (fr, fc) = pos
                if (grid[fr][fc] == -1) {
                    grid[fr][fc] = regId
                    unfilled--
                    // 每次隨機只擴展 1~2 格以保持形狀自然交錯
                    if (Random.nextBoolean()) break
                }
            }
        }

        // 如果還有孤立未填滿的格子，直接指派給相鄰有效區域
        if (unfilled > 0) {
            for (r in 0 until size) {
                for (c in 0 until size) {
                    if (grid[r][c] == -1) {
                        for ((dr, dc) in directions) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until size && nc in 0 until size && grid[nr][nc] != -1) {
                                grid[r][c] = grid[nr][nc]
                                break
                            }
                        }
                        if (grid[r][c] == -1) grid[r][c] = 0
                    }
                }
            }
        }

        return grid
    }
}
