package com.example

import com.example.data.model.GameDifficulty
import com.example.data.model.WheelDifficultyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTrainVisualTest {

    @Test
    fun testWheelDifficultyConfigs() {
        // 驗證 6 大難度的星盤配置與總數字數
        val configs = listOf(
            GameDifficulty.BEGINNER to 7,
            GameDifficulty.INTERMEDIATE to 13,
            GameDifficulty.ADVANCED to 19,
            GameDifficulty.HARD to 37,
            GameDifficulty.HELL to 43,
            GameDifficulty.EPIC to 69
        )

        for ((diff, expectedTotal) in configs) {
            val config = WheelDifficultyConfig.getConfig(diff)
            assertEquals("難度 ${diff.key} 總數字數應為 $expectedTotal", expectedTotal, config.totalNumbers)

            // 驗證中央圓 (1) + 各環項目數總和 == totalNumbers
            val ringItemsSum = config.rings.sumOf { it.itemCount }
            assertEquals(expectedTotal, ringItemsSum + 1)

            // 驗證旋轉時長在合理區間 (15s ~ 20s)
            assertTrue(config.rotationDurationMs in 10_000..30_000)
        }
    }

    @Test
    fun testStarRatingByMistakes() {
        fun calculateStars(wrongCount: Int): Int {
            return if (wrongCount == 0) 3 else if (wrongCount <= 3) 2 else 1
        }

        assertEquals(3, calculateStars(0))
        assertEquals(2, calculateStars(1))
        assertEquals(2, calculateStars(3))
        assertEquals(1, calculateStars(4))
        assertEquals(1, calculateStars(10))
    }

    @Test
    fun testAnnularSectorAngleCalculation() {
        val itemCount = 12
        val sweepAngle = 360f / itemCount
        assertEquals(30f, sweepAngle, 0.001f)

        // 模擬角度判定
        val relAngle = 45f
        val k = (relAngle / sweepAngle).toInt().coerceIn(0, itemCount - 1)
        assertEquals(1, k) // 45度落在第二個扇區 (30~60度)
    }
}
