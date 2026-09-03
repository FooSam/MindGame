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

class AvatarWhackGameLogicTest {

    @Test
    fun testAllDifficultyConfigs() {
        val beginner = WhackDifficultyConfig.getConfig(GameDifficulty.BEGINNER)
        assertEquals(2, beginner.rows)
        assertEquals(2, beginner.cols)
        assertEquals(4, beginner.totalHoles)
        assertEquals(1500L, beginner.stayDurationMs)
        assertEquals(0.0f, beginner.fakeOutProb, 0.001f)
        assertEquals(0.0f, beginner.bombProb, 0.001f)

        val intermediate = WhackDifficultyConfig.getConfig(GameDifficulty.INTERMEDIATE)
        assertEquals(2, intermediate.rows)
        assertEquals(3, intermediate.cols)
        assertEquals(6, intermediate.totalHoles)
        assertEquals(1200L, intermediate.stayDurationMs)
        assertEquals(0.10f, intermediate.fakeOutProb, 0.001f)

        val advanced = WhackDifficultyConfig.getConfig(GameDifficulty.ADVANCED)
        assertEquals(3, advanced.rows)
        assertEquals(3, advanced.cols)
        assertEquals(9, advanced.totalHoles)
        assertEquals(900L, advanced.stayDurationMs)
        assertEquals(0.20f, advanced.fakeOutProb, 0.001f)
        assertTrue(advanced.bombProb > 0f)

        val hard = WhackDifficultyConfig.getConfig(GameDifficulty.HARD)
        assertEquals(3, hard.rows)
        assertEquals(3, hard.cols)
        assertEquals(9, hard.totalHoles)
        assertEquals(700L, hard.stayDurationMs)
        assertEquals(0.30f, hard.fakeOutProb, 0.001f)
        assertEquals(2, hard.simultaneousTargets)

        val hell = WhackDifficultyConfig.getConfig(GameDifficulty.HELL)
        assertEquals(4, hell.rows)
        assertEquals(3, hell.cols)
        assertEquals(12, hell.totalHoles)
        assertEquals(550L, hell.stayDurationMs)
        assertEquals(0.40f, hell.fakeOutProb, 0.001f)

        val epic = WhackDifficultyConfig.getConfig(GameDifficulty.EPIC)
        assertEquals(4, epic.rows)
        assertEquals(3, epic.cols)
        assertEquals(12, epic.totalHoles)
        assertEquals(450L, epic.stayDurationMs)
        assertEquals(0.50f, epic.fakeOutProb, 0.001f)
        assertEquals(3, epic.simultaneousTargets)

        // 驗證假動作停留時間範圍與隨機浮動
        for (diff in GameDifficulty.entries) {
            val cfg = WhackDifficultyConfig.getConfig(diff)
            val fakeStay = cfg.calculateStayDuration(isFakeOut = true)
            assertTrue("Stay duration should be at least 380ms", fakeStay >= 380L)
            assertTrue("Stay duration should not exceed stayDurationMs", fakeStay <= cfg.stayDurationMs)
        }
    }

    @Test
    fun testAvatarCatalogDrawableMapping() {
        assertEquals(21, AvatarCatalog.TOTAL_AVATARS)

        for (i in 1..21) {
            val normalRes = AvatarCatalog.getDrawableRes(i, AvatarExpression.NORMAL, AvatarType.NORMAL)
            val fakeRes = AvatarCatalog.getDrawableRes(i, AvatarExpression.FAKE_OUT, AvatarType.NORMAL)
            val hitRes = AvatarCatalog.getDrawableRes(i, AvatarExpression.HIT, AvatarType.NORMAL)

            assertTrue(normalRes != 0)
            assertTrue(fakeRes != 0)
            assertTrue(hitRes != 0)
        }

        val bombRes = AvatarCatalog.getDrawableRes(1, AvatarExpression.NORMAL, AvatarType.BOMB)
        val bonusRes = AvatarCatalog.getDrawableRes(1, AvatarExpression.NORMAL, AvatarType.BONUS)
        assertEquals(R.drawable.avatar_bomb, bombRes)
        assertEquals(R.drawable.avatar_bonus, bonusRes)
    }

    @Test
    fun testRandomAvatarIndexRange() {
        val indices = (1..100).map { AvatarCatalog.getRandomAvatarIndex() }
        for (idx in indices) {
            assertTrue(idx in 1..21)
        }
    }
}
