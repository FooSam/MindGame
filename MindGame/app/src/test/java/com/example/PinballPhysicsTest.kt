package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.data.model.Localization
import com.example.game.pinball.FlipperPhysicsEngine
import com.example.game.pinball.NightMarketBallState
import com.example.game.pinball.NightMarketPhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos

class PinballPhysicsTest {

    // ==========================================
    // 1.「珠過來啊！」(FlipperPinball) 核心物理與幾何測試
    // ==========================================

    @Test
    fun testAllDifficultiesEpicLaunchCanEnterPlayfield() {
        // 嚴苛測試：對包含史詩級 (EPIC) 在內的所有 6 個難度，驗證發射動能是否充沛，100% 越過頂弧進場
        val difficulties = listOf(
            GameDifficulty.BEGINNER,
            GameDifficulty.INTERMEDIATE,
            GameDifficulty.ADVANCED,
            GameDifficulty.HARD,
            GameDifficulty.HELL,
            GameDifficulty.EPIC
        )

        for (diff in difficulties) {
            val engine = FlipperPhysicsEngine(difficulty = diff)
            assertEquals("初始鋼珠應在走道內", true, engine.pinball.inAlley)
            assertFalse("初始鋼珠尚未進入主場地", engine.hasEnteredPlayfield)

            // 執行發射
            val launched = engine.launchBall(0.95f)
            assertTrue("難度 $diff 發射必須成功", launched)
            assertFalse("發射後鋼珠不再處於 Alley 待發狀態", engine.pinball.inAlley)

            // 進行 100 幀物理步進模擬 (約 1.6 秒)
            var reachedPlayfield = false
            for (frame in 0 until 100) {
                engine.update(0.016f)
                if (engine.hasEnteredPlayfield) {
                    reachedPlayfield = true
                    break
                }
            }

            assertTrue("難度 $diff 鋼珠必須在 1.6 秒內越過頂部圓弧進入主場地，絕不半路卡死或掉回！", reachedPlayfield)
            assertTrue("進場後鋼珠存活狀態正常", engine.pinball.isAlive)
        }
    }

    @Test
    fun testLaunchFailureProtectionDoesNotDrainBall() {
        // 測試發球保護機制：若蓄力極微弱在通道內掉回，必須安全回位發射底座，絕不扣命！
        var drainCount = 0
        val engine = FlipperPhysicsEngine(
            difficulty = GameDifficulty.EPIC,
            onBallDrained = { drainCount++ }
        )

        engine.launchBall(0.3f)
        // 手動模擬向上動能微弱，受重力在通道內掉回
        engine.pinball.vy = 200f
        engine.pinball.y = 550f

        // 步進 20 幀
        repeat(20) {
            engine.update(0.016f)
        }

        assertEquals("通道內掉回底座絕不觸發漏球扣命 (Drain)", 0, drainCount)
        assertTrue("鋼珠安全回歸發射底座", engine.pinball.inAlley)
        assertTrue("鋼珠狀態正常存活", engine.pinball.isAlive)
    }

    @Test
    fun testFlipperGoldenRatioAndSlantedInlaneGuidesGeometry() {
        // 測試常規彈珠台黃金佈局：消除太空漏洞，驗證兩側導流斜壁與擋板守備間隙
        val engine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        // 1. 擋板軸心位置 (x=70 與 x=258)
        assertEquals(70f, engine.leftFlipper.pivot.x, 0.01f)
        assertEquals(258f, engine.rightFlipper.pivot.x, 0.01f)
        assertEquals(520f, engine.leftFlipper.pivot.y, 0.01f)

        // 2. 兩側導流斜壁幾何
        assertEquals(14f, engine.leftGuideP1.x, 0.01f)
        assertEquals(68f, engine.leftGuideP2.x, 0.01f)
        assertTrue("左斜壁能將球導至左擋板軸心外側", engine.leftGuideP2.x <= engine.leftFlipper.pivot.x)

        assertEquals(engine.mainPlayWidth - 14f, engine.rightGuideP1.x, 0.01f)
        assertEquals(260f, engine.rightGuideP2.x, 0.01f)
        assertTrue("右斜壁能將球導至右擋板軸心外側", engine.rightGuideP2.x >= engine.rightFlipper.pivot.x)

        // 3. 擋板長度與靜止 Central Drain 間隙
        val leftLen = engine.leftFlipper.length
        val rightLen = engine.rightFlipper.length

        val leftRestTipX = 70f + cos(26.0 * PI / 180.0).toFloat() * leftLen
        val rightRestTipX = 258f - cos(26.0 * PI / 180.0).toFloat() * rightLen
        val drainGap = rightRestTipX - leftRestTipX

        // 中央落球孔間距應在 40f ~ 50f (約 1.7 ~ 2.1 倍球徑，防守常規比例)
        assertTrue("靜止時中央落球空隙必須在 40f ~ 50f 之間 (當前: $drainGap)", drainGap in 40f..50f)

        // 4. 向上揮打時兩端點距離
        val leftActiveTipX = 70f + cos(-24.0 * PI / 180.0).toFloat() * leftLen
        val rightActiveTipX = 258f - cos(-24.0 * PI / 180.0).toFloat() * rightLen
        val activeGap = rightActiveTipX - leftActiveTipX
        assertTrue("揮打至頂點時兩擋板端點間距必須 >= 35f，絕不重疊打架 (當前: $activeGap)", activeGap >= 35f)
    }

    @Test
    fun testFlipperPassiveCollisionDoesNotBounceUpwardsAndDrainsUnderGravity() {
        // 驗證規範：若玩家未按壓橫板，鋼珠接觸靜止橫板時，絕不產生像反彈圈那樣的大力向上彈射，且在重力作用下自然向下滑落漏球
        var drained = false
        val engine = FlipperPhysicsEngine(
            difficulty = GameDifficulty.BEGINNER,
            onBallDrained = { drained = true }
        )

        // 放置於主場地，位於左擋板上方，給予適當向下初速模擬自然下落
        engine.pinball.inAlley = false
        engine.hasEnteredPlayfield = true
        engine.pinball.x = 100f
        engine.pinball.y = 510f
        engine.pinball.vx = 0f
        engine.pinball.vy = 250f

        // 驗證左擋板處於靜止狀態 (未按壓)
        assertFalse(engine.leftFlipper.isPressed)
        assertEquals(0f, engine.leftFlipper.angularVelocity, 0.01f)

        // 模擬 1 幀碰撞更新
        engine.update(0.016f)

        // 核心驗證 1：絕不產生如舊版 vy < -500f 的暴衝彈射 (向上速度嚴格限制在非完全彈性鈍性反彈範圍內，vy > -150f)
        assertTrue("靜止橫板接觸時向上速度不可過大（當前 vy: ${engine.pinball.vy}）", engine.pinball.vy > -150f)

        // 模擬後續 90 幀物理更新 (約 1.5 秒)，驗證在重力作用下鋼珠自然順著傾斜橫板向下滑向中央排水溝 Drain
        for (i in 0 until 90) {
            engine.update(0.016f)
            if (drained) break
        }

        assertTrue("未揮動橫板時，鋼珠必須在重力作用下自然向下滑動落入排水孔 (Drain) 漏球，絕不可產生死循環！", drained)
    }

    @Test
    fun testFlipperActiveSwingLaunchesBallUpward() {
        // 驗證規範：當玩家主動按下擋板、擋板向上高速旋轉擊球時，必須能傳遞動量將鋼珠有力擊出
        val engine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        engine.pinball.inAlley = false
        engine.hasEnteredPlayfield = true
        engine.pinball.x = 110f
        engine.pinball.y = 515f
        engine.pinball.vx = 0f
        engine.pinball.vy = 100f

        // 模擬玩家按下左擋板
        engine.leftFlipper.isPressed = true

        // 步進 1 幀使擋板向上旋轉產生角速度並撞擊鋼珠
        engine.update(0.016f)

        // 核心驗證：主動揮擊時，鋼珠必須獲得充足的向上擊球速度 (vy < -240f)
        assertTrue("主動揮擊時鋼珠必須獲得強勁向上推進力（當前 vy: ${engine.pinball.vy}）", engine.pinball.vy < -240f)
    }

    // ==========================================
    // 2.「夜市珠霸王」(NightMarketPinball) 核心物理與軌跡測試
    // ==========================================

    @Test
    fun testNightMarketBallNeverPrematurelySettles() {
        // 抓死致命 Bug：驗證發射第 1 幀與前 10 幀絕對不會因為 y > 485f 被誤判入槽結算！
        var slotLandedCount = 0
        val engine = NightMarketPhysicsEngine(
            difficulty = GameDifficulty.BEGINNER,
            onSlotLanded = { slotLandedCount++ }
        )

        assertEquals(NightMarketBallState.WAITING_LAUNCH, engine.ball.state)
        assertEquals(0, slotLandedCount)

        // 發射鋼珠
        val launched = engine.launch(0.85f)
        assertTrue("發射應成功", launched)
        assertEquals(NightMarketBallState.LAUNCHING_ALLEY, engine.ball.state)

        // 執行前 10 幀物理步進 (球在發射通道向上衝刺)
        for (i in 0 until 10) {
            engine.update(0.016f)
            assertEquals("發射通道衝刺期間絕對不允許被提前判定入槽結算！", NightMarketBallState.LAUNCHING_ALLEY, engine.ball.state)
            assertEquals("未入槽前絕對不觸發結算回調", 0, slotLandedCount)
            assertTrue("鋼珠在通道內筆直向上飛", engine.ball.vy < 0f)
        }
    }

    @Test
    fun testNightMarketFullTrajectoryFromLaunchToPinsToSlot() {
        // 完整連續軌跡測試：驗證鋼珠完整走完「走道衝刺 -> 頂弧轉向 -> 釘陣彈跳 -> 最終入槽」的全流程
        var hitPinCount = 0
        var landedSlotId = -1

        val engine = NightMarketPhysicsEngine(
            difficulty = GameDifficulty.BEGINNER,
            onPinHit = { pin, impulse -> hitPinCount++ },
            onSlotLanded = { slot -> landedSlotId = slot.id }
        )

        engine.launch(0.9f)

        var enteredPins = false
        var settled = false

        // 模擬 600 幀物理更新 (約 9.6 秒，充分觀察完整連續滾動至落槽)
        for (frame in 0 until 600) {
            engine.update(0.016f)

            if (engine.ball.state == NightMarketBallState.IN_PINS) {
                enteredPins = true
            }

            if (engine.ball.state == NightMarketBallState.SETTLED) {
                settled = true
                break
            }
        }

        assertTrue("鋼珠必須順利進入釘陣木台 (IN_PINS)", enteredPins)
        assertTrue("鋼珠在釘陣中滾動期間必須發生多次碰撞反彈 (當前碰撞次數: $hitPinCount)", hitPinCount >= 3)
        assertTrue("鋼珠最終必須在經過釘陣後順暢落入底部槽位結算 (SETTLED, 當前狀態: ${engine.ball.state}, y: ${engine.ball.y}, vy: ${engine.ball.vy})", settled)
        assertTrue("結算時獎勵槽 ID 必須有效 (0 ~ 5)", landedSlotId in 0..5)
    }

    @Test
    fun testFlipperNoCenterPostAndSlingshotDamping() {
        // 驗證規範：初級中央下方無敵彈回點已徹底拔除
        val engine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        assertEquals("初級不再有常駐無敵 CenterPost", null, engine.centerPost)

        // 驗證 Slingshot 法向量偏向下導流，引導球至擋板
        val leftSling = engine.slingshots[0]
        val rightSling = engine.slingshots[1]
        assertTrue("左側 Slingshot 法向 y 必須為正 (向下偏流)", leftSling.normal.y > 0f)
        assertTrue("右側 Slingshot 法向 y 必須為正 (向下偏流)", rightSling.normal.y > 0f)

        // 驗證鋼珠掉落中央排水孔能正常觸發 Drain 結算
        var drainTriggered = false
        val drainEngine = FlipperPhysicsEngine(
            difficulty = GameDifficulty.BEGINNER,
            onBallDrained = { drainTriggered = true }
        )
        drainEngine.pinball.inAlley = false
        drainEngine.hasEnteredPlayfield = true
        drainEngine.pinball.x = drainEngine.mainPlayWidth * 0.50f
        drainEngine.pinball.y = drainEngine.virtualHeight + 10f
        drainEngine.pinball.vy = 200f

        repeat(5) {
            drainEngine.update(0.016f)
        }
        assertTrue("鋼珠掉入底部排水孔必須正常觸發失誤/結算 (Drain)，絕不被無敵點彈回", drainTriggered)
    }

    @Test
    fun testNightMarketMultiDifficultySlotsAndBallsCount() {
        // 驗證規範：初級 6 槽 6 顆、中級 8 槽 8 顆、高級 10 槽 10 顆、困難級 13 槽 13 顆
        val beginner = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        val intermediate = NightMarketPhysicsEngine(difficulty = GameDifficulty.INTERMEDIATE)
        val advanced = NightMarketPhysicsEngine(difficulty = GameDifficulty.ADVANCED)
        val hard = NightMarketPhysicsEngine(difficulty = GameDifficulty.HARD)

        assertEquals("初級 6 槽", 6, beginner.rewardSlots.size)
        assertEquals("中級 8 槽", 8, intermediate.rewardSlots.size)
        assertEquals("高級 10 槽", 10, advanced.rewardSlots.size)
        assertEquals("困難級 13 槽", 13, hard.rewardSlots.size)

        // 驗證鋼珠半徑等比例縮小 (視覺大台化)
        assertTrue("高級鋼珠半徑小於初級", advanced.ballRadius < beginner.ballRadius)
        assertTrue("困難級鋼珠半徑小於高級", hard.ballRadius < advanced.ballRadius)
        assertEquals(11.0f, beginner.ballRadius, 0.01f)
        assertEquals(6.8f, hard.ballRadius, 0.01f)
    }

    @Test
    fun testNightMarketGrandSlamLogic() {
        // 驗證台灣夜市靈魂：大滿貫 (每個槽都只有 1 顆珠子)
        val engine = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        val slotCount = engine.rewardSlots.size // 6 槽

        // 模擬 6 顆球恰好分別落入 0..5 槽
        for (i in 0 until slotCount) {
            engine.settledBalls.add(
                com.example.game.pinball.SettledBall(
                    slotIndex = i,
                    stackOrder = 0,
                    x = 50f * i,
                    y = 500f
                )
            )
        }

        val slotCounts = (0 until slotCount).map { id -> engine.settledBalls.count { it.slotIndex == id } }
        val isGrandSlam = slotCounts.all { it == 1 }
        assertTrue("6 槽各 1 顆球必須判定為傳奇大滿貫！", isGrandSlam)

        // 模擬未達成大滿貫 (某槽 2 顆，某槽空)
        engine.clearSettledBalls()
        engine.settledBalls.add(com.example.game.pinball.SettledBall(0, 0, 0f, 0f))
        engine.settledBalls.add(com.example.game.pinball.SettledBall(0, 1, 0f, 0f))
        engine.settledBalls.add(com.example.game.pinball.SettledBall(1, 0, 0f, 0f))
        engine.settledBalls.add(com.example.game.pinball.SettledBall(2, 0, 0f, 0f))
        engine.settledBalls.add(com.example.game.pinball.SettledBall(3, 0, 0f, 0f))
        engine.settledBalls.add(com.example.game.pinball.SettledBall(4, 0, 0f, 0f))

        val notGrandSlamCounts = (0 until slotCount).map { id -> engine.settledBalls.count { it.slotIndex == id } }
        assertFalse("有槽為空或重複時絕對不是大滿貫", notGrandSlamCounts.all { it == 1 })
    }

    // ==========================================
    // 3. 多語系與遊戲分類驗證
    // ==========================================

    @Test
    fun testLocalizationAndGameTypes() {
        val flipperType = GameType.fromKey("PINBALL_FLIPPER")
        val marketType = GameType.fromKey("NIGHT_MARKET_PINBALL")

        assertEquals(GameType.PINBALL_FLIPPER, flipperType)
        assertEquals(GameType.NIGHT_MARKET_PINBALL, marketType)

        val flipperZh = Localization.getString("game_pinball_flipper", AppLanguage.TRADITIONAL_CHINESE)
        val marketZh = Localization.getString("game_night_market_pinball", AppLanguage.TRADITIONAL_CHINESE)
        val flipperEn = Localization.getString("game_pinball_flipper", AppLanguage.ENGLISH)
        val marketEn = Localization.getString("game_night_market_pinball", AppLanguage.ENGLISH)

        assertEquals("珠過來啊！", flipperZh)
        assertEquals("夜市珠霸王", marketZh)
        assertEquals("Pinball Flipper", flipperEn)
        assertEquals("Night Market Pinball", marketEn)
    }

    // ==========================================
    // 4. 動態出球與區域分佈物理驗證 (消滅倒勾與寫死速度)
    // ==========================================

    @Test
    fun testFlipperDynamicLaunchPowerProducesDifferentTrajectoriesAndNoFixedVelocity() {
        // 驗證 Flipper 出球不再是固定寫死的 vx=-220f, vy=120f，而是與蓄力比例真實連動
        val lightEngine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        val fullEngine = FlipperPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        lightEngine.launchBall(0.35f)
        fullEngine.launchBall(1.0f)

        var lightVxOnEntry = 0f
        var fullVxOnEntry = 0f

        for (i in 0 until 120) {
            lightEngine.update(0.016f)
            if (lightEngine.hasEnteredPlayfield && lightVxOnEntry == 0f) {
                lightVxOnEntry = lightEngine.pinball.vx
            }
            fullEngine.update(0.016f)
            if (fullEngine.hasEnteredPlayfield && fullVxOnEntry == 0f) {
                fullVxOnEntry = fullEngine.pinball.vx
            }
        }

        assertTrue("輕蓄力進場必須成功", lightEngine.hasEnteredPlayfield)
        assertTrue("滿蓄力進場必須成功", fullEngine.hasEnteredPlayfield)
        assertTrue("出球初速不再是寫死固定值", lightVxOnEntry != fullVxOnEntry)
        assertTrue("滿蓄力向左水平速度量級必須大於輕蓄力", Math.abs(fullVxOnEntry) > Math.abs(lightVxOnEntry))
        // 驗證初速不是原先硬寫死的 -220f
        assertTrue("滿蓄力初速不再是硬寫死的 -220f", Math.abs(fullVxOnEntry) > 300f)
    }

    @Test
    fun testNightMarketFullPowerLaunchCanReachLeftZone() {
        // 驗證夜市珠霸王在滿蓄力發射下，鋼珠能平滑滑過頂部軌道直達左側大獎區 (x < mainBoardWidth * 0.35f)
        val engine = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        val launched = engine.launch(1.0f)
        assertTrue("滿蓄力發射成功", launched)

        var minXReached = engine.virtualWidth
        for (frame in 0 until 150) {
            engine.update(0.016f)
            if (engine.ball.state == NightMarketBallState.IN_PINS) {
                if (engine.ball.x < minXReached) {
                    minXReached = engine.ball.x
                }
            }
        }

        val leftZoneThreshold = engine.mainBoardWidth * 0.35f
        assertTrue(
            "滿蓄力鋼珠必須能夠到達左側區域 (minX = $minXReached 必須小於 $leftZoneThreshold)",
            minXReached < leftZoneThreshold
        )
    }

    @Test
    fun testNightMarketVariablePowerControlsZoneDistribution() {
        // 驗證夜市珠霸王輕蓄力與滿蓄力能分別控制鋼珠落入右側與左側區域
        val lightEngine = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)
        val fullEngine = NightMarketPhysicsEngine(difficulty = GameDifficulty.BEGINNER)

        lightEngine.launch(0.30f)
        fullEngine.launch(1.0f)

        var lightEntryX = 0f
        var fullEntryX = 0f

        for (frame in 0 until 150) {
            lightEngine.update(0.016f)
            if (lightEngine.ball.y >= 75f && lightEntryX == 0f && lightEngine.ball.state == NightMarketBallState.IN_PINS) {
                lightEntryX = lightEngine.ball.x
            }
            fullEngine.update(0.016f)
            if (fullEngine.ball.y >= 75f && fullEntryX == 0f && fullEngine.ball.state == NightMarketBallState.IN_PINS) {
                fullEntryX = fullEngine.ball.x
            }
        }

        assertTrue("滿蓄力進場位置必須比輕蓄力更加偏左", fullEntryX < lightEntryX)
        assertTrue("輕蓄力於中右側落入釘陣 (lightEntryX = $lightEntryX 應在右側)", lightEntryX > lightEngine.mainBoardWidth * 0.40f)
        assertTrue("滿蓄力直達左側大獎區 (fullEntryX = $fullEntryX 應在左側)", fullEntryX < fullEngine.mainBoardWidth * 0.35f)
    }
}
