package com.example

import com.example.data.model.GameDifficulty
import com.example.game.sudoku.SudokuGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGeneratorTest {

    @Test
    fun testSudokuGenerationForAllDifficulties() {
        val difficulties = listOf(
            GameDifficulty.BEGINNER,
            GameDifficulty.INTERMEDIATE,
            GameDifficulty.ADVANCED,
            GameDifficulty.HARD,
            GameDifficulty.HELL,
            GameDifficulty.EPIC
        )

        for (diff in difficulties) {
            val (initial, solution, config) = SudokuGenerator.generatePuzzle(diff)
            val n = config.gridSize
            val totalCells = n * n

            assertEquals("盤面大小相符", totalCells, initial.size)
            assertEquals("解答盤面大小相符", totalCells, solution.size)

            // 驗證解答每一行皆包含完整符號集合
            for (r in 0 until n) {
                val rowSymbols = (0 until n).map { c -> solution[r * n + c] }.toSet()
                assertEquals("每行符號不重複且完整", n, rowSymbols.size)
            }

            // 驗證解答每一列皆包含完整符號集合
            for (c in 0 until n) {
                val colSymbols = (0 until n).map { r -> solution[r * n + c] }.toSet()
                assertEquals("每列符號不重複且完整", n, colSymbols.size)
            }

            // 驗證解答每個九宮格/子區塊不重複
            val subR = config.subRows
            val subC = config.subCols
            for (br in 0 until (n / subR)) {
                for (bc in 0 until (n / subC)) {
                    val blockSymbols = mutableSetOf<String>()
                    for (r in 0 until subR) {
                        for (c in 0 until subC) {
                            val row = br * subR + r
                            val col = bc * subC + c
                            blockSymbols.add(solution[row * n + col])
                        }
                    }
                    assertEquals("每個子宮格符號不重複且完整", n, blockSymbols.size)
                }
            }

            // 驗證初始題目為解答的子集
            var filledCount = 0
            for (i in 0 until totalCells) {
                if (initial[i].isNotEmpty()) {
                    assertEquals("初始提示格必須與解答一致", solution[i], initial[i])
                    filledCount++
                }
            }
            assertTrue("提示數在有效範圍內", filledCount in config.minClues..config.maxClues)
        }
    }
}
