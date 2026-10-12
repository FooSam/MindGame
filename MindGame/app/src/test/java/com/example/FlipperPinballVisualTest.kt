package com.example

import com.example.data.model.GameDifficulty
import com.example.game.pinball.FlipperPhysicsEngine
import com.example.ui.screens.calculatePinballStars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 《珠過來啊！》2024-2026 次世代手遊視覺與核心物理規範自動化單元測試
 */
class FlipperPinballVisualTest {

    @Test
    fun testPinballStarRatingCalculation() {
        // 初級 (BEGINNER): >= 3000 -> 3星, >= 1500 -> 2星, 其他 1星
        assertEquals(3, calculatePinballStars(GameDifficulty.BEGINNER, 3000))
        assertEquals(3, calculatePinballStars(GameDifficulty.BEGINNER, 5000))
        assertEquals(2, calculatePinballStars(GameDifficulty.BEGINNER, 2999))
        assertEquals(2, calculatePinballStars(GameDifficulty.BEGINNER, 1500))
        assertEquals(1, calculatePinballStars(GameDifficulty.BEGINNER, 1499))
        assertEquals(1, calculatePinballStars(GameDifficulty.BEGINNER, 0))

        // 中級 (INTERMEDIATE): >= 4500 -> 3星, >= 2500 -> 2星, 其他 1星
        assertEquals(3, calculatePinballStars(GameDifficulty.INTERMEDIATE, 4500))
        assertEquals(3, calculatePinballStars(GameDifficulty.INTERMEDIATE, 8000))
        assertEquals(2, calculatePinballStars(GameDifficulty.INTERMEDIATE, 4499))
        assertEquals(2, calculatePinballStars(GameDifficulty.INTERMEDIATE, 2500))
        assertEquals(1, calculatePinballStars(GameDifficulty.INTERMEDIATE, 2499))
        assertEquals(1, calculatePinballStars(GameDifficulty.INTERMEDIATE, 100))

        // 高級/其他 (ADVANCED / HARD / HELL / EPIC): >= 6000 -> 3星, >= 3500 -> 2星, 其他 1星
        assertEquals(3, calculatePinballStars(GameDifficulty.ADVANCED, 6000))
        assertEquals(3, calculatePinballStars(GameDifficulty.HARD, 7500))
        assertEquals(2, calculatePinballStars(GameDifficulty.ADVANCED, 5999))
        assertEquals(2, calculatePinballStars(GameDifficulty.HARD, 3500))
        assertEquals(1, calculatePinballStars(GameDifficulty.HELL, 3499))
        assertEquals(1, calculatePinballStars(GameDifficulty.EPIC, 0))
    }

    @Test
    fun testPinballEngineDimensionsAndPhysicsParameters() {
        val engine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        // 驗證黃金比例長寬與走道尺寸
        assertEquals(360f, engine.virtualWidth, 0.001f)
        assertEquals(600f, engine.virtualHeight, 0.001f)
        assertEquals(32f, engine.alleyWidth, 0.001f)
        assertEquals(328f, engine.mainPlayWidth, 0.001f)

        // 驗證物理重力常數
        assertEquals(1450f, engine.gravityScale, 0.001f)

        // 驗證難度得分倍率與擋板覆蓋長度階梯
        val beginnerEngine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        val intermediateEngine = FlipperPhysicsEngine(difficulty = GameDifficulty.INTERMEDIATE)
        val advancedEngine = FlipperPhysicsEngine(difficulty = GameDifficulty.ADVANCED)
        val hardEngine = FlipperPhysicsEngine(difficulty = GameDifficulty.HARD)
        val hellEngine = FlipperPhysicsEngine(difficulty = GameDifficulty.HELL)
        val epicEngine = FlipperPhysicsEngine(difficulty = GameDifficulty.EPIC)

        assertEquals(1.0f, beginnerEngine.scoreMultiplier, 0.001f)
        assertEquals(1.25f, intermediateEngine.scoreMultiplier, 0.001f)
        assertEquals(1.5f, advancedEngine.scoreMultiplier, 0.001f)
        assertEquals(2.0f, hardEngine.scoreMultiplier, 0.001f)
        assertEquals(2.5f, hellEngine.scoreMultiplier, 0.001f)
        assertEquals(3.0f, epicEngine.scoreMultiplier, 0.001f)

        assertEquals(82f, beginnerEngine.flipperLength, 0.001f)
        assertEquals(80f, intermediateEngine.flipperLength, 0.001f)
        assertEquals(78f, advancedEngine.flipperLength, 0.001f)
        assertEquals(76f, hardEngine.flipperLength, 0.001f)
        assertEquals(74f, hellEngine.flipperLength, 0.001f)
        assertEquals(72f, epicEngine.flipperLength, 0.001f)
    }

    @Test
    fun testFlipperMechanicsAndAngles() {
        val engine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        // 驗證初始左右擋板角度
        assertEquals(engine.leftFlipper.restAngleDeg, engine.leftFlipper.currentAngleDeg, 0.001f)
        assertEquals(engine.rightFlipper.restAngleDeg, engine.rightFlipper.currentAngleDeg, 0.001f)
        assertFalse("初始左擋板不應為按下狀態", engine.leftFlipper.isPressed)
        assertFalse("初始右擋板不應為按下狀態", engine.rightFlipper.isPressed)

        // 模擬按下左擋板並進行物理步進更新
        engine.leftFlipper.isPressed = true
        engine.update(0.016f)
        // 擋板應當轉向啟動角 (activeAngleDeg)
        assertTrue("左擋板在按下後角度應朝 activeAngleDeg 轉移", engine.leftFlipper.currentAngleDeg != engine.leftFlipper.restAngleDeg)

        // 鬆開後更新
        engine.leftFlipper.isPressed = false
        // 步進足夠長時間讓其回到原位
        repeat(30) {
            engine.update(0.016f)
        }
        assertEquals("左擋板回彈至靜止角", engine.leftFlipper.restAngleDeg, engine.leftFlipper.currentAngleDeg, 0.5f)
    }

    @Test
    fun testPinballLaunchAndAlleyProtection() {
        val engine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        // 初始狀態：球在發射道內
        assertTrue("初始時球應在發射道 (inAlley)", engine.pinball.inAlley)
        assertTrue("球應存活", engine.pinball.isAlive)
        assertEquals(engine.virtualWidth - engine.alleyWidth / 2f, engine.pinball.x, 0.1f)

        // 觸發蓄力彈射
        val launched = engine.launchBall(0.9f)
        assertTrue("應成功彈射", launched)
        assertTrue("彈射初速度 vy 應向上 (負值)", engine.pinball.vy < -200f)

        // 測試發球保護手動重置
        engine.resetBallToAlley()
        assertTrue("重置後球應重新歸位發射道", engine.pinball.inAlley)
        assertEquals(0f, engine.pinball.vx, 0.001f)
        assertEquals(0f, engine.pinball.vy, 0.001f)
    }

    @Test
    fun testBumpersAndRolloversCompleteness() {
        val engine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        // 驗證 Bumpers 清單
        assertTrue("Bumpers 不得為空", engine.bumpers.isNotEmpty())
        for (bumper in engine.bumpers) {
            assertTrue("Bumper 半徑應大於 0", bumper.radius > 0f)
            assertTrue("Bumper 得分應大於 0", bumper.points > 0)
            assertNotNull("Bumper 顏色不得為空", bumper.color)
        }

        // 驗證 Rollovers 清單
        assertTrue("Rollovers 不得為空", engine.rollovers.isNotEmpty())
        for (rollover in engine.rollovers) {
            assertFalse("初始 Rollover 應為熄滅狀態", rollover.isLit)
            assertTrue("Rollover 寬高應大於 0", rollover.width > 0f && rollover.height > 0f)
            assertNotNull("Rollover 顏色不得為空", rollover.color)
        }
    }
}
