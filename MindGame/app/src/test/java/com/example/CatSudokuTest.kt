package com.example

import com.example.data.model.GameDifficulty
import com.example.game.catsudoku.CatSudokuGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatSudokuTest {

    @Test
    fun testCatSudokuGenerationForAllDifficulties() {
        val difficulties = listOf(
            GameDifficulty.BEGINNER,
            GameDifficulty.INTERMEDIATE,
            GameDifficulty.ADVANCED,
            GameDifficulty.HARD,
            GameDifficulty.HELL,
            GameDifficulty.EPIC
        )

        for (diff in difficulties) {
            val puzzle = CatSudokuGenerator.generatePuzzle(diff)
            val size = CatSudokuGenerator.getSizeForDifficulty(diff)

            assertEquals(size, puzzle.size)
            assertEquals(size * size, puzzle.grid.size)
            assertEquals(size, puzzle.solutionQueens.size)

            // 驗證解中每隻貓咪在不同行、不同列
            val rows = puzzle.solutionQueens.map { it.first }.toSet()
            val cols = puzzle.solutionQueens.map { it.second }.toSet()
            assertEquals("每行恰一隻貓", size, rows.size)
            assertEquals("每列恰一隻貓", size, cols.size)

            // 驗證解中任兩隻貓在 8 方向不相鄰
            for (i in 0 until puzzle.solutionQueens.size) {
                for (j in i + 1 until puzzle.solutionQueens.size) {
                    val q1 = puzzle.solutionQueens[i]
                    val q2 = puzzle.solutionQueens[j]
                    val isAdjacent = kotlin.math.abs(q1.first - q2.first) <= 1 && kotlin.math.abs(q1.second - q2.second) <= 1
                    assertTrue("任兩隻貓咪周圍8格不相鄰", !isAdjacent)
                }
            }

            // 驗證每隻貓座落在不同 region
            val catRegions = puzzle.solutionQueens.map { (r, c) ->
                puzzle.grid[r * size + c].regionId
            }.toSet()
            assertEquals("每個區域恰有一隻貓", size, catRegions.size)
        }
    }
}
