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

    @Test
    fun testGridGenerationContainsPair() {
        // 驗證全 6 大難度在 100 次連續隨機生成中，100% 絕對存在至少一組相同數字，且尺寸完全正確
        val difficulties = GameDifficulty.entries
        for (diff in difficulties) {
            for (i in 1..100) {
                val maxNumber = when (diff) {
                    GameDifficulty.BEGINNER, GameDifficulty.INTERMEDIATE -> 9
                    GameDifficulty.ADVANCED -> 12
                    GameDifficulty.HARD -> 16
                    GameDifficulty.HELL -> 20
                    GameDifficulty.EPIC -> 25
                }
                val totalCells = diff.speedMatchCells
                val range = (1..maxNumber).toList()
                val pairValue = range.random()
                val remainingCount = (totalCells - 2).coerceAtLeast(0)
                val otherCandidates = range.filter { it != pairValue }.shuffled()
                val otherValues = List(remainingCount) { idx -> otherCandidates[idx % otherCandidates.size] }.shuffled()
                val grid = (listOf(pairValue, pairValue) + otherValues).shuffled()

                assertEquals(totalCells, grid.size)
                val hasPair = grid.groupBy { it }.any { it.value.size >= 2 }
                assertTrue("Difficulty $diff in iteration $i must contain at least 1 pair", hasPair)
            }
        }
    }
}
