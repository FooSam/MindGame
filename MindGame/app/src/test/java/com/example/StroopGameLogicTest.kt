package com.example

import com.example.data.model.GameDifficulty
import com.example.game.stroop.StroopGenerator
import com.example.game.stroop.StroopInstruction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StroopGameLogicTest {

    @Test
    fun testBeginnerQuestionGeneration() {
        // 1. 指定選顏色
        for (i in 1..10) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.BEGINNER, StroopInstruction.CHOOSE_COLOR)
            assertEquals(GameDifficulty.BEGINNER, q.difficulty)
            assertEquals(StroopInstruction.CHOOSE_COLOR, q.instruction)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.displayColor.key, correctOption.id)
        }
        // 2. 指定選字義
        for (i in 1..10) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.BEGINNER, StroopInstruction.CHOOSE_TEXT)
            assertEquals(GameDifficulty.BEGINNER, q.difficulty)
            assertEquals(StroopInstruction.CHOOSE_TEXT, q.instruction)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.mainWord.key, correctOption.id)
        }
    }

    @Test
    fun testIntermediateQuestionGeneration() {
        // 驗證單局固定選字義：整局每一題 100% 維持選字義，正確答案為文字字義
        for (i in 1..20) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.INTERMEDIATE, StroopInstruction.CHOOSE_TEXT)
            assertEquals(GameDifficulty.INTERMEDIATE, q.difficulty)
            assertEquals(StroopInstruction.CHOOSE_TEXT, q.instruction)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.mainWord.key, correctOption.id)
        }

        // 驗證單局固定選顏色：整局每一題 100% 維持選顏色，正確答案為文字顏色
        for (i in 1..20) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.INTERMEDIATE, StroopInstruction.CHOOSE_COLOR)
            assertEquals(GameDifficulty.INTERMEDIATE, q.difficulty)
            assertEquals(StroopInstruction.CHOOSE_COLOR, q.instruction)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.displayColor.key, correctOption.id)
        }
    }

    @Test
    fun testAdvancedComparisonQuestionGeneration() {
        var sawEqual = false
        var sawNotEqual = false
        for (i in 1..40) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.ADVANCED, StroopInstruction.COMPARE_EQUAL)
            assertEquals(GameDifficulty.ADVANCED, q.difficulty)
            assertEquals(StroopInstruction.COMPARE_EQUAL, q.instruction)
            assertTrue(q.isYesNoMode)
            assertEquals(2, q.options.size)
            
            val isReallyEqual = (q.leftWordMeaning == q.rightDisplayColor)
            assertEquals(isReallyEqual, q.isYesCorrect)
            
            val yesOption = q.options.first { it.id == "YES" }
            val noOption = q.options.first { it.id == "NO" }
            assertEquals(isReallyEqual, yesOption.isCorrect)
            assertEquals(!isReallyEqual, noOption.isCorrect)

            if (isReallyEqual) sawEqual = true else sawNotEqual = true
        }
        assertTrue(sawEqual)
        assertTrue(sawNotEqual)
    }

    @Test
    fun testHardQuestionGeneration() {
        // 1. 困難選顏色 (干擾按鈕)
        for (i in 1..15) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.HARD, StroopInstruction.DISTRACTED_CHOOSE)
            assertEquals(GameDifficulty.HARD, q.difficulty)
            assertEquals(StroopInstruction.DISTRACTED_CHOOSE, q.instruction)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.displayColor.key, correctOption.id)
        }
        // 2. 困難選字義 (干擾按鈕)
        for (i in 1..15) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.HARD, StroopInstruction.DISTRACTED_TEXT)
            assertEquals(GameDifficulty.HARD, q.difficulty)
            assertEquals(StroopInstruction.DISTRACTED_TEXT, q.instruction)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.mainWord.key, correctOption.id)
        }
    }

    @Test
    fun testHellQuestionGeneration() {
        // 地獄級：動態旋轉辨色 vs 選字義
        val qColor = StroopGenerator.generateQuestion(GameDifficulty.HELL, StroopInstruction.DYNAMIC_FAST)
        assertEquals(StroopInstruction.DYNAMIC_FAST, qColor.instruction)
        val qText = StroopGenerator.generateQuestion(GameDifficulty.HELL, StroopInstruction.DYNAMIC_TEXT)
        assertEquals(StroopInstruction.DYNAMIC_TEXT, qText.instruction)
    }

    @Test
    fun testEpicCompositeQuestionGeneration() {
        for (i in 1..20) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.EPIC, StroopInstruction.COMPOSITE_NOT)
            assertEquals(GameDifficulty.EPIC, q.difficulty)
            assertEquals(StroopInstruction.COMPOSITE_NOT, q.instruction)
            assertNotNull(q.shape)
            assertNotNull(q.notConditionColor)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.displayColor.key, correctOption.id)
        }
    }

    @Test
    fun testPickRoundInstructionForAllDifficulties() {
        for (diff in GameDifficulty.entries) {
            val inst = StroopGenerator.pickRoundInstruction(diff)
            assertNotNull(inst)
            val question = StroopGenerator.generateQuestion(diff, inst)
            assertEquals(inst, question.instruction)
        }
    }
}
