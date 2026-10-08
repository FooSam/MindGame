package com.example

import com.example.data.model.GameDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTestVisualTest {

    @Test
    fun testSchulteGridDifficultyDimensions() {
        // 驗證 3x3 ~ 8x8 舒爾特方格維度與總格數
        val configs = listOf(
            GameDifficulty.BEGINNER to (3 to 9),
            GameDifficulty.INTERMEDIATE to (4 to 16),
            GameDifficulty.ADVANCED to (5 to 25),
            GameDifficulty.HARD to (6 to 36),
            GameDifficulty.HELL to (7 to 49),
            GameDifficulty.EPIC to (8 to 64)
        )

        for ((diff, expected) in configs) {
            val (dim, total) = expected
            assertEquals("難度 ${diff.key} 網格維度應為 $dim", dim, diff.gridDim)
            assertEquals("難度 ${diff.key} 總格數應為 $total", total, diff.totalCells)
            assertEquals(dim * dim, diff.totalCells)
        }
    }

    @Test
    fun testRatingAndStarThresholds() {
        val totalCells = 25 // ADVANCED 5x5
        fun getRatingAndStars(timeMillis: Long, total: Int): Pair<String, Int> {
            val key = when {
                timeMillis < total * 600L -> "focus_rating_fast"
                timeMillis < total * 1200L -> "focus_rating_great"
                else -> "focus_rating_good"
            }
            val stars = when {
                timeMillis < total * 600L -> 3
                timeMillis < total * 1200L -> 2
                else -> 1
            }
            return key to stars
        }

        // 極速通關 (25 * 600 = 15000ms 內)
        val fastResult = getRatingAndStars(12000L, totalCells)
        assertEquals("focus_rating_fast", fastResult.first)
        assertEquals(3, fastResult.second)

        // 優秀通關 (15000ms ~ 30000ms)
        val greatResult = getRatingAndStars(20000L, totalCells)
        assertEquals("focus_rating_great", greatResult.first)
        assertEquals(2, greatResult.second)

        // 普通通關 (>= 30000ms)
        val goodResult = getRatingAndStars(35000L, totalCells)
        assertEquals("focus_rating_good", goodResult.first)
        assertEquals(1, goodResult.second)
    }

    @Test
    fun testSequentialTargetProgression() {
        var currentTarget = 1
        val clearedIndices = mutableSetOf<Int>()
        val totalCells = 9
        val grid = (1..totalCells).shuffled()

        // 模擬依序點擊 1 到 totalCells
        for (step in 1..totalCells) {
            val tappedIdx = grid.indexOf(step)
            assertTrue("目標 $step 必須在網格中存在", tappedIdx >= 0)

            if (grid[tappedIdx] == currentTarget) {
                clearedIndices.add(tappedIdx)
                currentTarget++
            }
        }

        assertEquals(totalCells + 1, currentTarget)
        assertEquals(totalCells, clearedIndices.size)
    }
}
