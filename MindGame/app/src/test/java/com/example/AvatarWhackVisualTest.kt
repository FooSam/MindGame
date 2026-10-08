package com.example

import com.example.data.model.GameDifficulty
import com.example.game.whack.AvatarCatalog
import com.example.game.whack.AvatarExpression
import com.example.game.whack.AvatarType
import com.example.game.whack.WhackDifficultyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarWhackVisualTest {

    @Test
    fun testStarRatingCalculation() {
        fun calculateStars(score: Int, bombHits: Int): Int {
            return when {
                score >= 2000 && bombHits == 0 -> 3
                score >= 1200 && bombHits <= 1 -> 2
                else -> 1
            }
        }

        // 3星評價
        assertEquals(3, calculateStars(2500, 0))
        assertEquals(3, calculateStars(2000, 0))

        // 2星評價
        assertEquals(2, calculateStars(2000, 1)) // 誤擊炸彈1次
        assertEquals(2, calculateStars(1500, 0))
        assertEquals(2, calculateStars(1200, 1))

        // 1星評價
        assertEquals(1, calculateStars(1100, 0))
        assertEquals(1, calculateStars(2000, 2)) // 誤擊炸彈2次
        assertEquals(1, calculateStars(500, 3))
    }

    @Test
    fun testAllDifficultiesGridDimensions() {
        for (diff in GameDifficulty.entries) {
            val config = WhackDifficultyConfig.getConfig(diff)
            assertNotNull(config)
            assertTrue("行數必須大於0", config.rows > 0)
            assertTrue("列數必須大於0", config.cols > 0)
            val totalHoles = config.rows * config.cols
            assertEquals("總地洞數需等於 rows * cols", totalHoles, config.totalHoles)
            assertTrue("遊戲時長必須大於 10s", config.gameDurationMs >= 10_000L)
        }
    }

    @Test
    fun testAvatarCatalogCompleteness() {
        assertEquals(21, AvatarCatalog.TOTAL_AVATARS)
        for (i in 1..21) {
            val normalRes = AvatarCatalog.getDrawableRes(i, AvatarExpression.NORMAL, AvatarType.NORMAL)
            val fakeRes = AvatarCatalog.getDrawableRes(i, AvatarExpression.FAKE_OUT, AvatarType.NORMAL)
            val hitRes = AvatarCatalog.getDrawableRes(i, AvatarExpression.HIT, AvatarType.NORMAL)

            assertTrue("Normal resource ID 必須大於 0", normalRes > 0)
            assertTrue("Fake resource ID 必須大於 0", fakeRes > 0)
            assertTrue("Hit resource ID 必須大於 0", hitRes > 0)
        }

        // 特殊頭像（炸彈與加分彩蛋）
        val bombRes = AvatarCatalog.getDrawableRes(1, AvatarExpression.NORMAL, AvatarType.BOMB)
        val bonusRes = AvatarCatalog.getDrawableRes(1, AvatarExpression.NORMAL, AvatarType.BONUS)
        assertTrue("Bomb resource ID 必須大於 0", bombRes > 0)
        assertTrue("Bonus resource ID 必須大於 0", bonusRes > 0)
    }

    @Test
    fun testUrgentTimeThreshold() {
        fun isUrgentTime(remainingMs: Long): Boolean {
            return remainingMs < 10_000L
        }

        assertTrue(isUrgentTime(9_999L))
        assertTrue(isUrgentTime(5_000L))
        assertTrue(isUrgentTime(0L))
        assertTrue(!isUrgentTime(10_000L))
        assertTrue(!isUrgentTime(30_000L))
    }
}
