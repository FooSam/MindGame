package com.example

import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.game.brainintrouble.BrainColor
import com.example.game.brainintrouble.BrainInTroubleConfig
import com.example.game.brainintrouble.BrainInTroubleGenerator
import com.example.game.brainintrouble.BrainShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 左右為難 (Brain in Trouble) 核心機制與演算法單元測試
 */
class BrainInTroubleTest {

    @Before
    fun setup() {
        BrainInTroubleGenerator.resetId()
    }

    @Test
    fun testAllDifficultyConfigsMatchSpecifications() {
        // 1. 初級 (BEGINNER): 2形狀 / 2顏色 / 無文字 / 4.5s 掉落 / 1.4s 生成
        val beginner = BrainInTroubleConfig.of(GameDifficulty.BEGINNER)
        assertEquals(2, beginner.leftShapes.size)
        assertEquals(2, beginner.rightColors.size)
        assertFalse(beginner.useTextOnColor)
        assertFalse(beginner.isStroopConflict)
        assertEquals(4500L, beginner.fallDurationMs)
        assertEquals(1400L, beginner.spawnIntervalMs)

        // 2. 中級 (INTERMEDIATE): 3形狀 / 3顏色 / 無文字 / 3.8s 掉落 / 1.1s 生成
        val intermediate = BrainInTroubleConfig.of(GameDifficulty.INTERMEDIATE)
        assertEquals(3, intermediate.leftShapes.size)
        assertEquals(3, intermediate.rightColors.size)
        assertFalse(intermediate.useTextOnColor)
        assertFalse(intermediate.isStroopConflict)
        assertEquals(3800L, intermediate.fallDurationMs)
        assertEquals(1100L, intermediate.spawnIntervalMs)

        // 3. 高級 (ADVANCED): 4形狀 (雙排2x2) / 4顏色 / 無文字 / 3.2s 掉落 / 0.85s 生成
        val advanced = BrainInTroubleConfig.of(GameDifficulty.ADVANCED)
        assertEquals(4, advanced.leftShapes.size)
        assertEquals(4, advanced.rightColors.size)
        assertFalse(advanced.useTextOnColor)
        assertFalse(advanced.isStroopConflict)
        assertEquals(3200L, advanced.fallDurationMs)
        assertEquals(850L, advanced.spawnIntervalMs)

        // 4. 困難級 (HARD): 5形狀 (雙排3+2) / 5顏色 / 有顏色文字 (色字一致) / 2.6s 掉落 / 0.70s 生成
        val hard = BrainInTroubleConfig.of(GameDifficulty.HARD)
        assertEquals(5, hard.leftShapes.size)
        assertEquals(5, hard.rightColors.size)
        assertTrue(hard.useTextOnColor)
        assertFalse(hard.isStroopConflict)
        assertEquals(2600L, hard.fallDurationMs)
        assertEquals(700L, hard.spawnIntervalMs)

        // 5. 地獄級 (HELL): 5形狀 / 5顏色 / 斯特魯普衝突 (Stroop) / 2.2s 掉落 / 0.58s 生成
        val hell = BrainInTroubleConfig.of(GameDifficulty.HELL)
        assertEquals(5, hell.leftShapes.size)
        assertEquals(5, hell.rightColors.size)
        assertTrue(hell.useTextOnColor)
        assertTrue(hell.isStroopConflict)
        assertEquals(2200L, hell.fallDurationMs)
        assertEquals(580L, hell.spawnIntervalMs)

        // 6. 史詩級 (EPIC): 6形狀 (雙排3x2) / 6顏色 (雙排3x2) / 斯特魯普衝突 / 1.8s 掉落 / 0.48s 生成
        val epic = BrainInTroubleConfig.of(GameDifficulty.EPIC)
        assertEquals(6, epic.leftShapes.size)
        assertEquals(6, epic.rightColors.size)
        assertTrue(epic.useTextOnColor)
        assertTrue(epic.isStroopConflict)
        assertEquals(1800L, epic.fallDurationMs)
        assertEquals(480L, epic.spawnIntervalMs)
    }

    @Test
    fun testGeneratorCreatesValidLeftShapeDrops() {
        val config = BrainInTroubleConfig.of(GameDifficulty.BEGINNER)
        val leftItem = BrainInTroubleGenerator.createDropItem(config, forceSide = true)

        assertTrue(leftItem.isLeftSide)
        assertNotNull(leftItem.shape)
        assertTrue(config.leftShapes.contains(leftItem.shape))
        assertTrue("跑道索引必須介於 0..2", leftItem.laneIndex in 0..2)
        assertEquals(0f, leftItem.progress, 0.001f)
        assertFalse(leftItem.isEliminated)
        assertFalse(leftItem.isMissed)
    }

    @Test
    fun testGeneratorCreatesValidRightColorDrops() {
        val config = BrainInTroubleConfig.of(GameDifficulty.INTERMEDIATE)
        val rightItem = BrainInTroubleGenerator.createDropItem(config, forceSide = false)

        assertFalse(rightItem.isLeftSide)
        assertNotNull(rightItem.targetColor)
        assertTrue(config.rightColors.contains(rightItem.targetColor))
        assertTrue("跑道索引必須介於 0..2", rightItem.laneIndex in 0..2)
        assertEquals(0f, rightItem.progress, 0.001f)
    }

    @Test
    fun testHardDifficultyGeneratesColorMatchingText() {
        val config = BrainInTroubleConfig.of(GameDifficulty.HARD)
        // 困難級生成右側物體時，文字應與 targetColor 一致（無衝突）
        for (i in 0 until 20) {
            val rightItem = BrainInTroubleGenerator.createDropItem(config, forceSide = false)
            assertNotNull(rightItem.textLabel)
            assertEquals(rightItem.targetColor?.displayNameZh, rightItem.textLabel)
        }
    }

    @Test
    fun testHellAndEpicDifficultyGeneratesStroopConflicts() {
        val config = BrainInTroubleConfig.of(GameDifficulty.HELL)
        var hasConflict = false
        for (i in 0 until 50) {
            val rightItem = BrainInTroubleGenerator.createDropItem(config, forceSide = false)
            assertNotNull(rightItem.textLabel)
            if (rightItem.textLabel != rightItem.targetColor?.displayNameZh) {
                hasConflict = true
                break
            }
        }
        assertTrue("地獄級應有機率生成文字與顏色相異的 Stroop 衝突物件", hasConflict)
    }

    @Test
    fun testScoreCalculationWithComboMultiplier() {
        var score = 0
        val baseScore = 100
        val comboBonusPerHit = 15

        // Combo 1
        var combo = 1
        score += baseScore + combo * comboBonusPerHit
        assertEquals(115, score)

        // Combo 2
        combo = 2
        score += baseScore + combo * comboBonusPerHit
        assertEquals(245, score)

        // Combo 3
        combo = 3
        score += baseScore + combo * comboBonusPerHit
        assertEquals(390, score)
    }

    @Test
    fun testGameTypeRegistrationAndKeyIntegrity() {
        val gameType = GameType.fromKey("BRAIN_IN_TROUBLE")
        assertEquals(GameType.BRAIN_IN_TROUBLE, gameType)
        assertEquals("BRAIN_IN_TROUBLE", gameType.key)
        assertEquals("game_brain_in_trouble", gameType.titleKey)
        assertEquals("game_brain_in_trouble_desc", gameType.descKey)
        assertEquals(com.example.data.model.RankingType.SCORE_DESC, gameType.rankingType)
    }

    @Test
    fun testEliminationAntiDebounceAndAtomicFrameGuard() {
        // 模擬掉落物列表中存在 2 個相同的形狀 (例如兩個圓形)
        val item1 = com.example.game.brainintrouble.DropItem(
            id = 1L,
            isLeftSide = true,
            laneIndex = 0,
            shape = BrainShape.CIRCLE,
            progress = 0.8f
        )
        val item2 = com.example.game.brainintrouble.DropItem(
            id = 2L,
            isLeftSide = true,
            laneIndex = 1,
            shape = BrainShape.CIRCLE,
            progress = 0.5f
        )
        val items = mutableListOf(item1, item2)

        var lastActionTime = 0L
        var lastEliminatedFrame = -1L
        val currentFrame = 100L

        // 模擬使用者點擊消除函數
        fun simulateClick(now: Long, frame: Long): Boolean {
            if (now - lastActionTime < 120L || lastEliminatedFrame == frame) {
                return false
            }
            lastActionTime = now

            val target = items.filter { it.isLeftSide && !it.isEliminated && !it.isMissed }.maxByOrNull { it.progress }
            if (target != null && target.shape == BrainShape.CIRCLE) {
                target.isEliminated = true
                lastEliminatedFrame = frame
                return true
            }
            return false
        }

        // 第一次點擊：成功消除 item1
        val firstResult = simulateClick(now = 1000L, frame = currentFrame)
        assertTrue("第一次點擊應成功消除", firstResult)
        assertTrue(item1.isEliminated)
        assertFalse("第二個掉落物此時絕不能被消除", item2.isEliminated)

        // 模擬同一幀 (相同 frame) 內或 30ms 內的重複點擊 (硬體微抖動 / 多觸控連鎖)
        val bounceResult = simulateClick(now = 1030L, frame = currentFrame)
        assertFalse("30ms 內同一幀的重複點擊應被防抖阻擋，不可穿透消除第二個目標", bounceResult)
        assertFalse("第二個相同掉落物依然保持存活", item2.isEliminated)

        // 模擬下一個物理幀 (frame 101) 且超過 120ms (如 200ms 後) 的正常第二次點擊
        val secondResult = simulateClick(now = 1200L, frame = currentFrame + 1)
        assertTrue("經過正常間隔後再次點擊，應成功消除第二個掉落物", secondResult)
        assertTrue(item2.isEliminated)
    }
}

