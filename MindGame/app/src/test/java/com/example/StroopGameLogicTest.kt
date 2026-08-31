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
        for (i in 1..20) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.BEGINNER)
            assertEquals(GameDifficulty.BEGINNER, q.difficulty)
            assertEquals(StroopInstruction.CHOOSE_COLOR, q.instruction)
            assertEquals(4, q.options.size)
            // Exactly 1 correct option
            val correctCount = q.options.count { it.isCorrect }
            assertEquals(1, correctCount)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.displayColor.key, correctOption.id)
        }
    }

    @Test
    fun testIntermediateQuestionGeneration() {
        var sawMeaning = false
        var sawColor = false
        for (i in 1..40) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.INTERMEDIATE)
            assertEquals(GameDifficulty.INTERMEDIATE, q.difficulty)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            if (q.instruction == StroopInstruction.CHOOSE_TEXT) {
                sawMeaning = true
                assertEquals(q.mainWord.key, correctOption.id)
            } else {
                sawColor = true
                assertEquals(q.displayColor.key, correctOption.id)
            }
        }
        assertTrue(sawMeaning)
        assertTrue(sawColor)
    }

    @Test
    fun testAdvancedComparisonQuestionGeneration() {
        var sawEqual = false
        var sawNotEqual = false
        for (i in 1..40) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.ADVANCED)
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
        for (i in 1..20) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.HARD)
            assertEquals(GameDifficulty.HARD, q.difficulty)
            assertEquals(StroopInstruction.DISTRACTED_CHOOSE, q.instruction)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.displayColor.key, correctOption.id)
        }
    }

    @Test
    fun testEpicCompositeQuestionGeneration() {
        for (i in 1..20) {
            val q = StroopGenerator.generateQuestion(GameDifficulty.EPIC)
            assertEquals(GameDifficulty.EPIC, q.difficulty)
            assertEquals(StroopInstruction.COMPOSITE_NOT, q.instruction)
            assertNotNull(q.shape)
            assertNotNull(q.notConditionColor)
            assertEquals(4, q.options.size)
            val correctOption = q.options.first { it.isCorrect }
            assertEquals(q.displayColor.key, correctOption.id)
        }
    }
}
