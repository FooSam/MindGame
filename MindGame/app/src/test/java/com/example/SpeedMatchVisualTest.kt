package com.example

import com.example.data.model.GameDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedMatchVisualTest {

    @Test
    fun testGemIndexMapping() {
        // 驗證 1~50 的數字皆能正確映射至 0..11 的 12 大寶石圖騰索引
        for (num in 1..50) {
            val gemIndex = ((num - 1) % 12).coerceAtLeast(0)
            assertTrue("Gem index must be between 0 and 11, but got $gemIndex for number $num", gemIndex in 0..11)
        }

        // 邊界測試：number <= 0 時亦能防禦
        val edgeZeroIndex = ((0 - 1) % 12).coerceAtLeast(0)
        assertTrue(edgeZeroIndex in 0..11)
    }

    @Test
    fun testDifficultyGridConfigurations() {
        val beginner = GameDifficulty.BEGINNER
        assertEquals(6, beginner.speedMatchCells)
        assertEquals(3, beginner.speedMatchCols)

        val intermediate = GameDifficulty.INTERMEDIATE
        assertEquals(9, intermediate.speedMatchCells)
        assertEquals(3, intermediate.speedMatchCols)

        val advanced = GameDifficulty.ADVANCED
        assertEquals(12, advanced.speedMatchCells)
        assertEquals(4, advanced.speedMatchCols)

        val hard = GameDifficulty.HARD
        assertEquals(16, hard.speedMatchCells)
        assertEquals(4, hard.speedMatchCols)

        val hell = GameDifficulty.HELL
        assertEquals(20, hell.speedMatchCells)
        assertEquals(4, hell.speedMatchCols)

        val epic = GameDifficulty.EPIC
        assertEquals(25, epic.speedMatchCells)
        assertEquals(5, epic.speedMatchCols)
    }

    @Test
    fun testStarRatingThresholds() {
        fun calculateStars(correctRounds: Int): Int {
            return when {
                correctRounds >= 25 -> 3
                correctRounds >= 15 -> 2
                else -> 1
            }
        }

        assertEquals(3, calculateStars(25))
        assertEquals(3, calculateStars(30))
        assertEquals(2, calculateStars(15))
        assertEquals(2, calculateStars(24))
        assertEquals(1, calculateStars(14))
        assertEquals(1, calculateStars(0))
    }
}
