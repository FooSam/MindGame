package com.example.game.fruit

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 水果切切樂三大模式（心跳果刃戰、避雷狂刀客、切片工坊）核心邏輯與物理演算法單元測試
 */
class FruitMasterEngineTest {

    @Test
    fun testHeartbeatSlicerInitialState() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.HEARTBEAT_SLICER)
        engine.reset()

        assertEquals(0, engine.score)
        assertEquals(0, engine.hearts) // 心跳模式不設生命限制
        assertEquals(60f, engine.remainingSeconds, 0.01f)
        assertFalse(engine.isGameOver)
        assertFalse(engine.isCriticalHeartbeat)
        assertEquals(1.0f, engine.heartbeatSpeedMultiplier, 0.01f)
    }

    @Test
    fun testBladeAndBombInitialState() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.BLADE_AND_BOMB)
        engine.reset()

        assertEquals(0, engine.score)
        assertEquals(3, engine.hearts) // 避雷狂刀客具備 3 顆心
        assertEquals(0f, engine.remainingSeconds, 0.01f)
        assertFalse(engine.isGameOver)
    }

    @Test
    fun testFruitSegmentSliceCollision() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.HEARTBEAT_SLICER)
        engine.reset()

        // 手動放置一顆西瓜在 (500, 500)，半徑 60
        val watermelon = FruitItem(
            id = 999L,
            x = 500f,
            y = 500f,
            vx = 0f,
            vy = -200f,
            type = FruitType.WATERMELON,
            specialType = SpecialFruitType.NONE,
            radius = 60f
        )
        engine.fruits.add(watermelon)

        // 刀痕由 (400, 500) 劃至 (600, 500) 橫穿西瓜中心
        engine.onPointerDown()
        val result = engine.processSlice(Offset(400f, 500f), Offset(600f, 500f))

        assertTrue("西瓜應被切開", watermelon.isSliced)
        assertEquals(1, result.hitFruits.size)
        assertEquals(10, engine.score) // 西瓜基礎 10 分
        assertEquals(1, engine.totalSliced)
    }

    @Test
    fun testComboSlicingBonus() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.HEARTBEAT_SLICER)
        engine.reset()

        // 放置 3 顆水果在一條橫線上
        for (i in 0 until 3) {
            engine.fruits.add(
                FruitItem(
                    id = 100L + i,
                    x = 400f + i * 80f, // 400, 480, 560
                    y = 500f,
                    vx = 0f,
                    vy = 0f,
                    type = FruitType.BANANA,
                    radius = 50f
                )
            )
        }

        // 一刀劃過這 3 顆水果
        engine.onPointerDown()
        val result = engine.processSlice(Offset(350f, 500f), Offset(650f, 500f))

        assertEquals(3, result.hitFruits.size)
        assertEquals(3, result.comboCount)
        // 3 顆香蕉 (3 * 10 = 30) + Combo x3 額外加分 ( (3-2)*15 = 15 ) = 45
        assertEquals(45, engine.score)
        assertEquals(3, engine.maxCombo)
    }

    @Test
    fun testBladeAndBombMissedFruitLosesHeart() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.BLADE_AND_BOMB, screenHeight = 1000f)
        engine.reset()

        val fruit = FruitItem(
            id = 1L,
            x = 500f,
            y = 1060f, // 已超出螢幕底端 1000 + 半徑
            vx = 0f,
            vy = 100f, // 向下運動
            type = FruitType.STRAWBERRY,
            radius = 40f
        )
        engine.fruits.add(fruit)

        engine.update(0.1f)

        assertEquals("未切水果掉落底端應扣 1 顆心", 2, engine.hearts)
        assertFalse(engine.isGameOver)
    }

    @Test
    fun testBladeAndBombHittingBombTriggersInkAndLoss() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.BLADE_AND_BOMB)
        engine.reset()

        val bomb = FruitItem(
            id = 777L,
            x = 500f,
            y = 500f,
            vx = 0f,
            vy = 0f,
            type = FruitType.WATERMELON,
            specialType = SpecialFruitType.FUNNY_BOMB,
            radius = 50f
        )
        engine.fruits.add(bomb)

        engine.onPointerDown()
        val res = engine.processSlice(Offset(450f, 500f), Offset(550f, 500f))

        assertTrue(res.hitBomb)
        assertEquals("切中炸彈應扣除 1 顆心", 2, engine.hearts)
        assertEquals("切中炸彈應觸發墨水覆蓋", 1.0f, engine.inkAlpha, 0.01f)
    }

    @Test
    fun testHeartbeatCriticalTimeAcceleration() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.HEARTBEAT_SLICER)
        engine.reset()

        // 模擬經過 46 秒，剩餘 14 秒
        for (i in 0 until 46) {
            engine.update(1.0f)
        }

        assertTrue("剩餘 14 秒應進入危機心跳狀態", engine.isCriticalHeartbeat)
        assertTrue("心跳頻率應大於 1.0x", engine.heartbeatSpeedMultiplier > 1.0f)
    }

    @Test
    fun testFeverTimeActivation() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.HEARTBEAT_SLICER)
        engine.reset()

        // 連續切 20 顆水果累積狂熱 (每次 +0.05f)
        for (i in 0 until 20) {
            val f = FruitItem(
                id = 1000L + i,
                x = 500f,
                y = 500f,
                vx = 0f,
                vy = 0f,
                type = FruitType.BANANA,
                radius = 50f
            )
            engine.fruits.add(f)
            engine.onPointerDown()
            engine.processSlice(Offset(450f, 500f), Offset(550f, 500f))
        }

        assertTrue("滿額後應進入狂熱模式", engine.isFeverActive)
        assertEquals(5.0f, engine.feverTimer, 0.01f)
    }

    @Test
    fun testWorkshopEngineChopAndConveyor() {
        val workshop = SlicingWorkshopEngine(screenWidth = 1000f, screenHeight = 1000f)
        workshop.reset()

        // 確保刀口下方 (bladeX = 350f) 存在長條蔬果
        workshop.items.clear()
        workshop.items.add(
            WorkshopItem(
                id = 1L,
                type = WorkshopItemType.CARROT,
                x = 200f,
                totalLength = 300f
            )
        )

        val chop = workshop.performChop()
        assertTrue("應切中胡蘿蔔", chop.hitItem)
        assertFalse("不是金屬障礙", chop.isMetalClang)
        assertEquals(1, workshop.totalCuts)
        assertTrue(workshop.score > 0)
        assertEquals(1, workshop.fallingPieces.size)
    }

    @Test
    fun testWorkshopMetalObstacleClang() {
        val workshop = SlicingWorkshopEngine(screenWidth = 1000f, screenHeight = 1000f)
        workshop.reset()

        // 在刀口下方放置金屬砧板障礙
        workshop.items.clear()
        workshop.items.add(
            WorkshopItem(
                id = 2L,
                type = WorkshopItemType.METAL_OBSTACLE,
                x = 300f,
                totalLength = 150f
            )
        )

        val chop = workshop.performChop()
        assertTrue("應判定切中物品", chop.hitItem)
        assertTrue("應觸發金屬彈刀 Clang", chop.isMetalClang)
        assertEquals("金屬障礙不計入成功切片數", 0, workshop.totalCuts)
        assertEquals(0, workshop.comboCuts)
    }

    @Test
    fun testWorkshopJuiceEvaluation() {
        val workshop = SlicingWorkshopEngine()
        workshop.reset()

        // 模擬切了 30 片草莓卷與 15 片香蕉
        workshop.cutCountsByType[WorkshopItemType.STRAWBERRY_ROLL] = 30
        workshop.cutCountsByType[WorkshopItemType.BANANA] = 15

        val result = workshop.evaluateJuiceResult()
        assertEquals("juice_berry_banana", result.juiceNameKey)
        assertEquals("草莓芭娜娜極光奶昔", result.customTitleZh)
        assertTrue(result.gradientColors.size >= 2)
        assertTrue(result.flavorDescriptionZh.isNotBlank())
        assertTrue(result.ingredientRatios.isNotEmpty())
        assertEquals(WorkshopItemType.STRAWBERRY_ROLL, result.garnishType)
    }

    @Test
    fun testWorkshopBlendingStateTransition() {
        val workshop = SlicingWorkshopEngine(screenWidth = 1000f, screenHeight = 1000f)
        workshop.reset()

        assertEquals(WorkshopState.SLICING, workshop.state)
        assertFalse(workshop.isJuiceReady)

        // 刀口下放置長條食材 (bladeX = 350f)
        workshop.items.clear()
        workshop.items.add(
            WorkshopItem(
                id = 1L,
                type = WorkshopItemType.STRAWBERRY_ROLL,
                x = 100f,
                totalLength = 1000f
            )
        )

        workshop.conveyorSpeed = 0f
        // 切滿 targetCutsForJuice (40 片，每次下刀間隔大於 minChopInterval)
        for (i in 0 until workshop.targetCutsForJuice) {
            workshop.update(0.12f)
            workshop.performChop()
        }

        assertEquals("達到目標切片數應切換至 BLENDING 攪拌狀態", WorkshopState.BLENDING, workshop.state)
        assertEquals(0f, workshop.blendingProgress, 0.01f)

        // 模擬攪拌 1.0 秒
        workshop.update(1.0f)
        assertTrue(workshop.blendingProgress > 0.5f)
        assertEquals(WorkshopState.BLENDING, workshop.state)

        // 模擬攪拌完成 (再過 1.0 秒，累計 2.0 秒 > 1.8 秒)
        workshop.update(1.0f)
        assertEquals("攪拌完成應切換至 RESULT 結果狀態", WorkshopState.RESULT, workshop.state)
        assertTrue("結果就緒", workshop.isJuiceReady)
        assertTrue("關卡完成", workshop.isStageCompleted)
    }

    @Test
    fun testPlayAgainResetAndSpawn() {
        val engine = FruitSlicerEngine(mode = FruitGameMode.HEARTBEAT_SLICER)
        engine.reset()

        // 模擬 60 秒結束
        engine.update(60.1f)
        assertTrue("時間結束應判定 isGameOver", engine.isGameOver)
        assertEquals(0f, engine.remainingSeconds, 0.01f)

        // 點擊「再來一局」，呼叫 reset()
        engine.reset()
        assertFalse("再來一局後 isGameOver 應為 false", engine.isGameOver)
        assertEquals(60f, engine.remainingSeconds, 0.01f)
        assertEquals(0, engine.score)
        assertTrue("水果池應被清空等待新一輪拋果", engine.fruits.isEmpty())

        // 模擬新一局開局更新 0.6 秒 (超過 spawnTimer 0.5 秒)
        engine.update(0.6f)
        assertTrue("再來一局後應成功拋出第一批水果", engine.fruits.isNotEmpty())
    }
}
