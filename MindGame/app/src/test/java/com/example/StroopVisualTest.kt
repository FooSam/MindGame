package com.example

import com.example.data.model.GameDifficulty
import com.example.game.stroop.StroopColor
import com.example.game.stroop.StroopGenerator
import com.example.game.stroop.StroopInstruction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StroopVisualTest {

    @Test
    fun testStarRatingCalculation() {
        fun calculateStars(score: Int, wrongCount: Int): Int {
            return when {
                score >= 2500 && wrongCount <= 2 -> 3
                score >= 1500 && wrongCount <= 5 -> 2
                else -> 1
            }
        }

        // 3星評價：高分且低錯誤
        assertEquals(3, calculateStars(3000, 0))
        assertEquals(3, calculateStars(2500, 2))

        // 2星評價
        assertEquals(2, calculateStars(2500, 3)) // 錯誤超過2
        assertEquals(2, calculateStars(1800, 1))
        assertEquals(2, calculateStars(1500, 5))

        // 1星評價
        assertEquals(1, calculateStars(1400, 0))
        assertEquals(1, calculateStars(1800, 6))
        assertEquals(1, calculateStars(500, 10))
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
        assertTrue(!isUrgentTime(45_000L))
    }

    @Test
    fun testAllColorsHaveValidProperties() {
        for (color in StroopColor.entries) {
            assertTrue("顏色代碼不可為空", color.key.isNotEmpty())
            assertTrue("中文名稱不可為空", color.zhName.isNotEmpty())
            assertTrue("英文名稱不可為空", color.enName.isNotEmpty())
            assertNotNull("Compose Color 不可為空", color.color)
        }
    }

    @Test
    fun testAllDifficultiesQuestionVisualIntegrity() {
        for (diff in GameDifficulty.entries) {
            val q = StroopGenerator.generateQuestion(diff)
            assertNotNull("題目不可為空", q)
            assertEquals("難度需一致", diff, q.difficulty)
            assertTrue("選項必須至少有 2 個", q.options.size >= 2)
            assertTrue("必須有且僅有 1 個正確選項", q.options.count { it.isCorrect } == 1)

            if (diff == GameDifficulty.ADVANCED) {
                assertTrue("高級必須為是/否二選一模式", q.isYesNoMode)
                assertEquals(2, q.options.size)
                assertNotNull("左字義不可為空", q.leftWordMeaning)
                assertNotNull("右顏色不可為空", q.rightDisplayColor)
            }

            if (diff == GameDifficulty.EPIC) {
                assertNotNull("史詩級必須有形狀干擾", q.shape)
                assertNotNull("史詩級必須有排除條件", q.notConditionColor)
            }
        }
    }

    @Test
    fun testOptionsColorPaletteCompleteness() {
        // 驗證初級與中級各選項皆有相應關聯色
        val qBeginner = StroopGenerator.generateQuestion(GameDifficulty.BEGINNER, StroopInstruction.CHOOSE_COLOR)
        assertEquals(4, qBeginner.options.size)
        for (opt in qBeginner.options) {
            assertNotNull(opt.associatedColor)
            assertTrue(opt.labelZh.isNotEmpty())
            assertTrue(opt.labelEn.isNotEmpty())
        }
    }
}
