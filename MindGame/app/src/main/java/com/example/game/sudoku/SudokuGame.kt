package com.example.game.sudoku

import com.example.data.model.GameDifficulty

data class SudokuConfig(
    val gridSize: Int,          // 4, 6, 9, 12
    val subRows: Int,           // 2, 2, 3, 3
    val subCols: Int,           // 2, 3, 3, 4
    val symbols: List<String>,  // ["1".."4"], ["1".."6"], ["1".."9"], ["1".."9", "A", "B", "C"]
    val minClues: Int,          // Clues count
    val maxClues: Int
)

object SudokuGameConfig {
    fun getConfig(difficulty: GameDifficulty): SudokuConfig {
        return when (difficulty) {
            GameDifficulty.BEGINNER -> SudokuConfig(
                gridSize = 4,
                subRows = 2,
                subCols = 2,
                symbols = listOf("1", "2", "3", "4"),
                minClues = 8,
                maxClues = 10
            )
            GameDifficulty.INTERMEDIATE -> SudokuConfig(
                gridSize = 6,
                subRows = 2,
                subCols = 3,
                symbols = listOf("1", "2", "3", "4", "5", "6"),
                minClues = 18,
                maxClues = 20
            )
            GameDifficulty.ADVANCED -> SudokuConfig(
                gridSize = 9,
                subRows = 3,
                subCols = 3,
                symbols = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9"),
                minClues = 36,
                maxClues = 45
            )
            GameDifficulty.HARD -> SudokuConfig(
                gridSize = 9,
                subRows = 3,
                subCols = 3,
                symbols = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9"),
                minClues = 30,
                maxClues = 35
            )
            GameDifficulty.HELL -> SudokuConfig(
                gridSize = 9,
                subRows = 3,
                subCols = 3,
                symbols = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9"),
                minClues = 22,
                maxClues = 29
            )
            GameDifficulty.EPIC -> SudokuConfig(
                gridSize = 12,
                subRows = 3,
                subCols = 4,
                symbols = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C"),
                minClues = 38,
                maxClues = 64
            )
        }
    }
}

data class SudokuMove(
    val index: Int,
    val previousValue: String,
    val newValue: String,
    val previousNotes: Set<String>,
    val newNotes: Set<String>
)

object SudokuGenerator {
    fun generatePuzzle(difficulty: GameDifficulty): Triple<List<String>, List<String>, SudokuConfig> {
        val config = SudokuGameConfig.getConfig(difficulty)
        val n = config.gridSize
        val subR = config.subRows
        val subC = config.subCols
        val symbolList = config.symbols

        // 1. Generate valid complete solution using matrix block permutation
        val mappedSymbols = symbolList.shuffled()

        val solutionBoard = MutableList(n * n) { "" }

        // Shuffle row bands and col bands
        val rowBandCount = n / subR
        val colBandCount = n / subC

        val shuffledRowBands = (0 until rowBandCount).shuffled()
        val shuffledColBands = (0 until colBandCount).shuffled()

        val rowMap = IntArray(n)
        for (bandIdx in 0 until rowBandCount) {
            val targetBand = shuffledRowBands[bandIdx]
            val rowsInBand = (0 until subR).shuffled()
            for (r in 0 until subR) {
                rowMap[bandIdx * subR + r] = targetBand * subR + rowsInBand[r]
            }
        }

        val colMap = IntArray(n)
        for (bandIdx in 0 until colBandCount) {
            val targetBand = shuffledColBands[bandIdx]
            val colsInBand = (0 until subC).shuffled()
            for (c in 0 until subC) {
                colMap[bandIdx * subC + c] = targetBand * subC + colsInBand[c]
            }
        }

        for (r in 0 until n) {
            for (c in 0 until n) {
                val origR = rowMap[r]
                val origC = colMap[c]
                val symbolIdx = ((origR % subR) * subC + (origR / subR) + origC) % n
                solutionBoard[r * n + c] = mappedSymbols[symbolIdx]
            }
        }

        // 2. Remove cells to create puzzle clues
        val initialBoard = solutionBoard.toMutableList()
        val totalCells = n * n
        val targetClues = (config.minClues..config.maxClues).random().coerceIn(config.minClues, totalCells)
        val cellsToRemove = totalCells - targetClues

        val removeIndices = (0 until totalCells).shuffled().take(cellsToRemove)
        for (idx in removeIndices) {
            initialBoard[idx] = ""
        }

        return Triple(initialBoard, solutionBoard, config)
    }
}
