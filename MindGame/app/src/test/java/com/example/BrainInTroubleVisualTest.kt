package com.example

import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.brainintrouble.BrainColor
import com.example.game.brainintrouble.BrainInTroubleConfig
import com.example.game.brainintrouble.BrainInTroubleGenerator
import com.example.game.brainintrouble.BrainShape
import com.example.ui.screens.calculateBrainInTroubleStars
import com.example.ui.screens.calculateNeuralBalanceIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 《左右為難》(Brain in Trouble) 次世代視覺與核心演算法自動化測試
 */
class BrainInTroubleVisualTest {

    @Before
    fun setup() {
        BrainInTroubleGenerator.resetId()
    }

    @Test
    fun testBrainInTroubleStarRatingCalculation() {
        // 1. 初級 (BEGINNER): 3500 滿星, 2000 2星
        assertEquals(3, calculateBrainInTroubleStars(GameDifficulty.BEGINNER, 3600))
        assertEquals(3, calculateBrainInTroubleStars(GameDifficulty.BEGINNER, 3500))
        assertEquals(2, calculateBrainInTroubleStars(GameDifficulty.BEGINNER, 2000))
        assertEquals(2, calculateBrainInTroubleStars(GameDifficulty.BEGINNER, 3499))
        assertEquals(1, calculateBrainInTroubleStars(GameDifficulty.BEGINNER, 1999))
        assertEquals(1, calculateBrainInTroubleStars(GameDifficulty.BEGINNER, 500))

        // 2. 中級 (INTERMEDIATE): 5000 滿星, 3000 2星
        assertEquals(3, calculateBrainInTroubleStars(GameDifficulty.INTERMEDIATE, 5500))
        assertEquals(3, calculateBrainInTroubleStars(GameDifficulty.INTERMEDIATE, 5000))
        assertEquals(2, calculateBrainInTroubleStars(GameDifficulty.INTERMEDIATE, 3000))
        assertEquals(2, calculateBrainInTroubleStars(GameDifficulty.INTERMEDIATE, 4999))
        assertEquals(1, calculateBrainInTroubleStars(GameDifficulty.INTERMEDIATE, 2999))

        // 3. 高級 (ADVANCED): 7000 滿星, 4500 2星
        assertEquals(3, calculateBrainInTroubleStars(GameDifficulty.ADVANCED, 7200))
        assertEquals(3, calculateBrainInTroubleStars(GameDifficulty.ADVANCED, 7000))
        assertEquals(2, calculateBrainInTroubleStars(GameDifficulty.ADVANCED, 4500))
        assertEquals(1, calculateBrainInTroubleStars(GameDifficulty.ADVANCED, 4499))

        // 4. 困難級以上 (HARD, HELL, EPIC): 9000 滿星, 6000 2星
        assertEquals(3, calculateBrainInTroubleStars(GameDifficulty.HARD, 9000))
        assertEquals(2, calculateBrainInTroubleStars(GameDifficulty.HELL, 6000))
        assertEquals(1, calculateBrainInTroubleStars(GameDifficulty.EPIC, 5999))
    }

    @Test
    fun testNeuralBalanceIndexFormula() {
        // 零次消除或初始狀態
        assertEquals(100, calculateNeuralBalanceIndex(0, 0))

        // 完全平衡消除 (左右相等)
        assertEquals(100, calculateNeuralBalanceIndex(15, 15))
        assertEquals(100, calculateNeuralBalanceIndex(1, 1))

        // 微幅偏差：左 15, 右 10 -> diff=5, total=25 -> 20% 偏差 -> 平衡度 80%
        assertEquals(80, calculateNeuralBalanceIndex(15, 10))
        assertEquals(80, calculateNeuralBalanceIndex(10, 15))

        // 極端偏差：左 20, 右 0 -> diff=20, total=20 -> 100% 偏差 -> 下限約束 50%
        assertEquals(50, calculateNeuralBalanceIndex(20, 0))
        assertEquals(50, calculateNeuralBalanceIndex(0, 30))
    }

    @Test
    fun testAllBrainShapesProperties() {
        for (shape in BrainShape.entries) {
            assertTrue("形狀代碼不可為空", shape.key.isNotEmpty())
            assertTrue("形狀中文不可為空", shape.displayNameZh.isNotEmpty())
            assertTrue("形狀英文不可為空", shape.displayNameEn.isNotEmpty())
        }
        assertEquals(6, BrainShape.entries.size)
    }

    @Test
    fun testAllBrainColorsProperties() {
        for (color in BrainColor.entries) {
            assertTrue("色彩代碼不可為空", color.key.isNotEmpty())
            assertTrue("色彩中文不可為空", color.displayNameZh.isNotEmpty())
            assertTrue("色彩英文不可為空", color.displayNameEn.isNotEmpty())
            assertNotNull("色彩 Compose Color 必須存在", color.color)
        }
        assertEquals(6, BrainColor.entries.size)
    }

    @Test
    fun test3LanesDistributionForBothTracks() {
        val config = BrainInTroubleConfig.of(GameDifficulty.BEGINNER)
        val leftLanesEncountered = mutableSetOf<Int>()
        val rightLanesEncountered = mutableSetOf<Int>()

        for (i in 0 until 50) {
            val leftItem = BrainInTroubleGenerator.createDropItem(config, forceSide = true)
            assertTrue(leftItem.isLeftSide)
            assertTrue(leftItem.laneIndex in 0..2)
            leftLanesEncountered.add(leftItem.laneIndex)

            val rightItem = BrainInTroubleGenerator.createDropItem(config, forceSide = false)
            assertFalse(rightItem.isLeftSide)
            assertTrue(rightItem.laneIndex in 0..2)
            rightLanesEncountered.add(rightItem.laneIndex)
        }

        assertEquals(setOf(0, 1, 2), leftLanesEncountered)
        assertEquals(setOf(0, 1, 2), rightLanesEncountered)
    }

    @Test
    fun testLocalizationIntegrityForBrainInTrouble() {
        val keys = listOf(
            "game_brain_in_trouble",
            "brain_in_trouble_title",
            "brain_in_trouble_score",
            "brain_in_trouble_lives",
            "brain_in_trouble_combo",
            "brain_in_trouble_max_combo",
            "brain_in_trouble_hint",
            "brain_in_trouble_stroop_rule",
            "brain_in_trouble_game_over",
            "brain_in_trouble_balance_index",
            "brain_in_trouble_rank_3star",
            "brain_in_trouble_rank_2star",
            "brain_in_trouble_rank_1star"
        )

        for (key in keys) {
            val zh = Localization.getString(key, AppLanguage.TRADITIONAL_CHINESE)
            val en = Localization.getString(key, AppLanguage.ENGLISH)
            assertFalse("繁體中文不能回退為原始key: $key", zh == key)
            assertFalse("英文不能回退為原始key: $key", en == key)
            assertTrue("繁體中文不可為空: $key", zh.isNotEmpty())
            assertTrue("英文不可為空: $key", en.isNotEmpty())
        }
    }
}
