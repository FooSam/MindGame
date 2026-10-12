package com.example

import com.example.data.model.GameDifficulty
import com.example.game.pinball.NightMarketBallState
import com.example.game.pinball.NightMarketPhysicsEngine
import com.example.ui.screens.calculateNightMarketStars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 《夜市珠霸王》2024-2026 次世代手遊視覺與核心規範自動化單元測試
 */
class NightMarketVisualTest {

    @Test
    fun testNightMarketStarRatingCalculation() {
        // 初級 (BEGINNER): 6 槽, avg >= 35 -> 3星 (總分 >= 210), avg >= 22 -> 2星 (總分 >= 132), 其他 1星
        assertEquals(3, calculateNightMarketStars(GameDifficulty.BEGINNER, 240, 6))
        assertEquals(3, calculateNightMarketStars(GameDifficulty.BEGINNER, 210, 6))
        assertEquals(2, calculateNightMarketStars(GameDifficulty.BEGINNER, 209, 6))
        assertEquals(2, calculateNightMarketStars(GameDifficulty.BEGINNER, 132, 6))
        assertEquals(1, calculateNightMarketStars(GameDifficulty.BEGINNER, 131, 6))
        assertEquals(1, calculateNightMarketStars(GameDifficulty.BEGINNER, 0, 6))

        // 中級 (INTERMEDIATE): 8 槽, avg >= 38 -> 3星 (總分 >= 304), avg >= 25 -> 2星 (總分 >= 200), 其他 1星
        assertEquals(3, calculateNightMarketStars(GameDifficulty.INTERMEDIATE, 320, 8))
        assertEquals(3, calculateNightMarketStars(GameDifficulty.INTERMEDIATE, 304, 8))
        assertEquals(2, calculateNightMarketStars(GameDifficulty.INTERMEDIATE, 303, 8))
        assertEquals(2, calculateNightMarketStars(GameDifficulty.INTERMEDIATE, 200, 8))
        assertEquals(1, calculateNightMarketStars(GameDifficulty.INTERMEDIATE, 199, 8))
        assertEquals(1, calculateNightMarketStars(GameDifficulty.INTERMEDIATE, 50, 8))

        // 高級/其他 (ADVANCED / HARD / HELL / EPIC): 10 或 13 槽, avg >= 42 -> 3星, avg >= 28 -> 2星, 其他 1星
        assertEquals(3, calculateNightMarketStars(GameDifficulty.ADVANCED, 450, 10))
        assertEquals(3, calculateNightMarketStars(GameDifficulty.HARD, 550, 13))
        assertEquals(2, calculateNightMarketStars(GameDifficulty.ADVANCED, 350, 10))
        assertEquals(2, calculateNightMarketStars(GameDifficulty.HARD, 400, 13))
        assertEquals(1, calculateNightMarketStars(GameDifficulty.HELL, 200, 13))
        assertEquals(1, calculateNightMarketStars(GameDifficulty.EPIC, 0, 13))
    }

    @Test
    fun testNightMarketEngineDimensionsAndDifficultyScaling() {
        val beginnerEngine = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        val intermediateEngine = NightMarketPhysicsEngine(difficulty = GameDifficulty.INTERMEDIATE)
        val advancedEngine = NightMarketPhysicsEngine(difficulty = GameDifficulty.ADVANCED)
        val hardEngine = NightMarketPhysicsEngine(difficulty = GameDifficulty.HARD)
        val hellEngine = NightMarketPhysicsEngine(difficulty = GameDifficulty.HELL)
        val epicEngine = NightMarketPhysicsEngine(difficulty = GameDifficulty.EPIC)

        // 畫布基礎長寬驗證
        assertEquals(340f, beginnerEngine.virtualWidth, 0.001f)
        assertEquals(560f, beginnerEngine.virtualHeight, 0.001f)

        // 槽數驗證 (台灣夜市鐵律：幾個槽就是打幾顆彈珠)
        assertEquals(6, beginnerEngine.slotCount)
        assertEquals(8, intermediateEngine.slotCount)
        assertEquals(10, advancedEngine.slotCount)
        assertEquals(13, hardEngine.slotCount)
        assertEquals(13, hellEngine.slotCount)
        assertEquals(13, epicEngine.slotCount)

        // 鋼珠半徑階梯驗證
        assertEquals(11.0f, beginnerEngine.ballRadius, 0.001f)
        assertEquals(9.5f, intermediateEngine.ballRadius, 0.001f)
        assertEquals(8.2f, advancedEngine.ballRadius, 0.001f)
        assertEquals(6.8f, hardEngine.ballRadius, 0.001f)

        // 發射走道寬度階梯驗證
        assertEquals(28f, beginnerEngine.alleyWidth, 0.001f)
        assertEquals(25f, intermediateEngine.alleyWidth, 0.001f)
        assertEquals(22f, advancedEngine.alleyWidth, 0.001f)
        assertEquals(18f, hardEngine.alleyWidth, 0.001f)

        // 主盤面寬度驗證
        assertEquals(340f - 28f, beginnerEngine.mainBoardWidth, 0.001f)
        assertEquals(340f - 25f, intermediateEngine.mainBoardWidth, 0.001f)
    }

    @Test
    fun testNightMarketBallStateAndLaunch() {
        val engine = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        // 初始狀態：待發
        assertEquals(NightMarketBallState.WAITING_LAUNCH, engine.ball.state)
        assertFalse("待發時 isLaunched 應為 false", engine.ball.isLaunched)
        assertFalse("待發時 isSettled 應為 false", engine.ball.isSettled)

        // 彈射發射
        val launched = engine.launch(0.85f)
        assertTrue("應成功彈射", launched)
        assertEquals(NightMarketBallState.LAUNCHING_ALLEY, engine.ball.state)
        assertTrue("彈射初速度 vy 應向上 (負值)", engine.ball.vy < -200f)
        assertTrue("發射後 isLaunched 應為 true", engine.ball.isLaunched)

        // 重置回發射走道
        engine.resetBallToAlley()
        assertEquals(NightMarketBallState.WAITING_LAUNCH, engine.ball.state)
        assertEquals(0f, engine.ball.vx, 0.001f)
        assertEquals(0f, engine.ball.vy, 0.001f)
    }

    @Test
    fun testRewardSlotsGeometryAndDistribution() {
        val engine = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        assertEquals("獎勵槽數量應與配置 slotCount 一致", engine.slotCount, engine.rewardSlots.size)
        assertTrue("獎勵槽清單不得為空", engine.rewardSlots.isNotEmpty())

        var prevMaxX = 0f
        for (slot in engine.rewardSlots) {
            assertTrue("槽寬必須大於 0", slot.maxX > slot.minX)
            assertTrue("槽分數必須大於 0", slot.points > 0)
            assertNotNull("槽顏色不得為空", slot.color)
            // 驗證相鄰性
            if (slot.id > 0) {
                assertEquals(prevMaxX, slot.minX, 0.01f)
            }
            prevMaxX = slot.maxX
        }
        // 最終槽應覆蓋至主盤面右側邊界
        assertEquals(engine.mainBoardWidth, prevMaxX, 0.01f)
    }

    @Test
    fun testPinsArrayIntegrityAndSettledBallsStacking() {
        val engine = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        assertTrue("釘陣不得為空", engine.pins.isNotEmpty())
        for (pin in engine.pins) {
            assertEquals("初級釘子半徑應為 6.0f", 6.0f, pin.radius, 0.001f)
            assertTrue("釘子 x 座標應在主盤面內", pin.x in 0f..engine.mainBoardWidth)
            assertTrue("釘子 y 座標應在合法高度", pin.y in 50f..engine.virtualHeight)
        }

        // 驗證落球堆疊初始為空
        assertTrue("初始堆疊落球應為空", engine.settledBalls.isEmpty())

        // 模擬落入獎勵槽並永久堆疊
        val slot = engine.rewardSlots.first()
        val stackOrder = engine.settledBalls.count { it.slotIndex == slot.id }
        engine.settledBalls.add(
            com.example.game.pinball.SettledBall(
                slotIndex = slot.id,
                stackOrder = stackOrder + 1,
                x = (slot.minX + slot.maxX) / 2f,
                y = 540f
            )
        )
        assertEquals(1, engine.settledBalls.size)
        assertEquals(slot.id, engine.settledBalls.first().slotIndex)
        assertEquals(1, engine.settledBalls.first().stackOrder)

        // 清理堆疊球
        engine.clearSettledBalls()
        assertTrue("清理後堆疊落球應為空", engine.settledBalls.isEmpty())
    }
}
