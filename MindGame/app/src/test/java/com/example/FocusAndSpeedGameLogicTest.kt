package com.example

import com.example.data.model.GameDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusAndSpeedGameLogicTest {

    @Test
    fun testFocusGameGridGeneration() {
        val difficulties = listOf(
            GameDifficulty.BEGINNER,
            GameDifficulty.INTERMEDIATE,
            GameDifficulty.ADVANCED,
            GameDifficulty.HARD,
            GameDifficulty.HELL,
            GameDifficulty.EPIC
        )

        for (diff in difficulties) {
            val totalCells = diff.totalCells
            val numbers = (1..totalCells).shuffled()
            assertEquals("方格總數符合難度定義", totalCells, numbers.size)
            assertEquals("數字必須包含 1 到 totalCells", (1..totalCells).toSet(), numbers.toSet())
        }
    }

    @Test
    fun testSpeedMatchGridPairGeneration() {
        val difficulties = listOf(
            GameDifficulty.BEGINNER,
            GameDifficulty.INTERMEDIATE,
            GameDifficulty.ADVANCED,
            GameDifficulty.HARD,
            GameDifficulty.HELL,
            GameDifficulty.EPIC
        )

        for (diff in difficulties) {
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
            val otherValues = List(remainingCount) { i -> otherCandidates[i % otherCandidates.size] }.shuffled()
            val grid = (listOf(pairValue, pairValue) + otherValues).shuffled()

            assertEquals("方格總數符合極速配對定義", totalCells, grid.size)

            // 統計各數字出現頻率
            val frequencyMap = grid.groupingBy { it }.eachCount()
            val duplicateEntries = frequencyMap.filter { it.value > 1 }

            // 驗證恰有一組相同數字（成對）
            assertEquals("恰有一組重複數字供配對", 1, duplicateEntries.size)
            assertEquals("該重複數字恰出現 2 次", 2, duplicateEntries.values.first())
        }
    }
}
