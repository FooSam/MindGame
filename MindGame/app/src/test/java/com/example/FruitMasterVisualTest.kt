package com.example

import com.example.game.fruit.FruitGameMode
import com.example.game.fruit.FruitType
import com.example.game.fruit.SpecialFruitType
import com.example.game.fruit.WorkshopItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 《水果切切樂》2024-2026 次世代手遊視覺與核心規範自動化單元測試
 */
class FruitMasterVisualTest {

    @Test
    fun testFruitGameModesCompleteness() {
        assertEquals("三大模式完整性驗證", 3, FruitGameMode.entries.size)
        assertEquals("模式一為心跳果刃戰", FruitGameMode.HEARTBEAT_SLICER, FruitGameMode.fromKey("HEARTBEAT"))
        assertEquals("模式二為避雷狂刀客", FruitGameMode.BLADE_AND_BOMB, FruitGameMode.fromKey("BLADE_BOMB"))
        assertEquals("模式三為切片工坊", FruitGameMode.WORKSHOP, FruitGameMode.fromKey("WORKSHOP"))
    }

    @Test
    fun testSevenFruitsPaletteAndDimensions() {
        assertEquals("7大擬真水果種類數量", 7, FruitType.entries.size)
        for (fruit in FruitType.entries) {
            assertTrue("半徑必須大於等於50dp", fruit.radiusDp >= 50f)
            assertTrue("基礎得分必須大於等於10分", fruit.baseScore >= 10)
            assertNotNull("外皮色彩不可為空", fruit.outerColor)
            assertNotNull("果肉色彩不可為空", fruit.innerColor)
            assertNotNull("果汁色彩不可為空", fruit.juiceColor)
        }
    }

    @Test
    fun testSpecialFruitTypes() {
        val types = SpecialFruitType.entries
        assertTrue("包含搞怪炸彈", types.contains(SpecialFruitType.FUNNY_BOMB))
        assertTrue("包含冰凍香蕉", types.contains(SpecialFruitType.FREEZE_BANANA))
        assertTrue("包含彩虹西瓜", types.contains(SpecialFruitType.RAINBOW_WATERMELON))
    }

    @Test
    fun testStarRatingCalculation() {
        fun calculateFruitStars(score: Int): Int {
            return when {
                score >= 1200 -> 3
                score >= 500 -> 2
                else -> 1
            }
        }

        assertEquals(3, calculateFruitStars(2000))
        assertEquals(3, calculateFruitStars(1200))
        assertEquals(2, calculateFruitStars(1199))
        assertEquals(2, calculateFruitStars(500))
        assertEquals(1, calculateFruitStars(499))
        assertEquals(1, calculateFruitStars(0))
    }

    @Test
    fun testWorkshopItemsSpecifications() {
        val items = WorkshopItemType.entries
        assertTrue("工坊項目包含障礙物與蔬果", items.size >= 5)
        val metalBoard = WorkshopItemType.METAL_OBSTACLE
        assertTrue("金屬砧板必須標記為障礙物", metalBoard.isObstacle)
        val carrot = WorkshopItemType.CARROT
        assertTrue("胡蘿蔔不得為障礙物", !carrot.isObstacle)
    }
}
