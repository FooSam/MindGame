package com.example

import com.example.data.model.GameType
import com.example.data.model.RankingType
import com.example.game.cattrap.CatEmotion
import com.example.game.cattrap.CatTrapGame
import com.example.game.cattrap.CatTrapGameStatus
import com.example.game.cattrap.CatTrapLevels
import com.example.game.cattrap.CatTrapMode
import com.example.game.cattrap.HexCellType
import com.example.game.cattrap.HexCoord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 《喵影圍捕：別想逃！》(Cat Trap) 尖頂六角網格、BFS 逃跑演算法與道具機制單元測試
 */
class CatTrapLogicTest {

    @Test
    fun testCatTrapRankingMandate() {
        // 專案全域排行榜規範：CAT_TRAP 必須為 SCORE_DESC
        assertEquals(RankingType.SCORE_DESC, GameType.CAT_TRAP.rankingType)
    }

    @Test
    fun testHexCoordNeighborsAndParity() {
        // 偶數行 (r = 2)
        val evenCoord = HexCoord(q = 5, r = 2)
        val evenNeighbors = evenCoord.getNeighbors(11)
        assertEquals(6, evenNeighbors.size)
        // 偶數行相鄰 offset: (-1, 0), (1, 0), (-1, -1), (0, -1), (-1, 1), (0, 1)
        assertTrue(evenNeighbors.contains(HexCoord(4, 2)))
        assertTrue(evenNeighbors.contains(HexCoord(6, 2)))
        assertTrue(evenNeighbors.contains(HexCoord(4, 1)))
        assertTrue(evenNeighbors.contains(HexCoord(5, 1)))
        assertTrue(evenNeighbors.contains(HexCoord(4, 3)))
        assertTrue(evenNeighbors.contains(HexCoord(5, 3)))

        // 奇數行 (r = 3)
        val oddCoord = HexCoord(q = 5, r = 3)
        val oddNeighbors = oddCoord.getNeighbors(11)
        assertEquals(6, oddNeighbors.size)
        // 奇數行相鄰 offset: (-1, 0), (1, 0), (0, -1), (1, -1), (0, 1), (1, 1)
        assertTrue(oddNeighbors.contains(HexCoord(4, 3)))
        assertTrue(oddNeighbors.contains(HexCoord(6, 3)))
        assertTrue(oddNeighbors.contains(HexCoord(5, 2)))
        assertTrue(oddNeighbors.contains(HexCoord(6, 2)))
        assertTrue(oddNeighbors.contains(HexCoord(5, 4)))
        assertTrue(oddNeighbors.contains(HexCoord(6, 4)))
    }

    @Test
    fun testHexCoordEdgeDetection() {
        val boardSize = 11
        // 邊界格子
        assertTrue(HexCoord(0, 5).isEdge(boardSize))
        assertTrue(HexCoord(10, 5).isEdge(boardSize))
        assertTrue(HexCoord(5, 0).isEdge(boardSize))
        assertTrue(HexCoord(5, 10).isEdge(boardSize))

        // 內部中心格子
        assertFalse(HexCoord(5, 5).isEdge(boardSize))
        assertFalse(HexCoord(3, 4).isEdge(boardSize))
    }

    @Test
    fun testLevelsCompleteness() {
        assertEquals(60, CatTrapLevels.TOTAL_LEVELS)
        for (lvlId in 1..CatTrapLevels.TOTAL_LEVELS) {
            val level = CatTrapLevels.getLevel(lvlId)
            assertEquals(lvlId, level.levelId)
            assertTrue("Level $lvlId name key should not be blank", level.nameKey.isNotBlank())
            assertTrue("Level $lvlId should have ponds", level.initialPonds.isNotEmpty())
            assertTrue("Star goal extreme steps must be <= target steps", level.starGoal.extremeSteps <= level.starGoal.targetSteps)
            // 驗證初始水坑障礙皆在合法邊界內且不在貓咪初始位置
            level.initialPonds.forEach { coord ->
                assertTrue(coord.q in 0 until 11)
                assertTrue(coord.r in 0 until 11)
                assertFalse("Initial pond should not be on cat spawn", coord == level.startCatPos)
            }
        }
    }

    @Test
    fun testBfsEscapePathFindsEdge() {
        val game = CatTrapGame(mode = CatTrapMode.LevelMode(1))
        val path = game.findShortestEscapePath(game.catPos)
        assertTrue("Path to edge should exist on open board", path.isNotEmpty())
        assertTrue("Path should lead to edge", path.last().isEdge(11))
        // 從中心到邊界的步數應在合理範圍
        assertTrue(path.size in 3..7)
    }

    @Test
    fun testCatTrapMoveAndStepsIncrement() {
        val game = CatTrapGame(mode = CatTrapMode.LevelMode(1))
        val initialPos = game.catPos
        val initialSteps = game.stepsTaken

        // 點擊遠處空格放置障礙
        val target = HexCoord(0, 0)
        val moveSuccess = game.onCellClicked(target)
        assertTrue(moveSuccess)
        assertEquals(HexCellType.BLOCK, game.grid[target])
        assertEquals(initialSteps + 1, game.stepsTaken)
        // 貓咪已移動，且新座標為舊座標的鄰居之一
        assertTrue(initialPos.getNeighbors(11).contains(game.catPos))
    }

    @Test
    fun testFreezeToolEffect() {
        val game = CatTrapGame(mode = CatTrapMode.LevelMode(1))
        val initialCatPos = game.catPos
        val used = game.useFreezeTool()
        assertTrue(used)
        assertTrue(game.isFreezeActive)

        // 玩家下子
        game.onCellClicked(HexCoord(0, 0))
        // 貓咪被冰凍，本回合不移動，座標保持不變
        assertEquals(initialCatPos, game.catPos)
        // 冰凍狀態在回合後解除
        assertFalse(game.isFreezeActive)
    }

    @Test
    fun testDoubleBlockToolEffect() {
        val game = CatTrapGame(mode = CatTrapMode.LevelMode(1))
        val initialCatPos = game.catPos
        val used = game.useDoubleBlockTool()
        assertTrue(used)
        assertEquals(2, game.doubleBlockRemaining)

        // 玩家下第 1 個障礙
        game.onCellClicked(HexCoord(0, 0))
        // 貓咪不應移動
        assertEquals(initialCatPos, game.catPos)
        assertEquals(1, game.doubleBlockRemaining)

        // 玩家下第 2 個障礙
        game.onCellClicked(HexCoord(1, 0))
        // 第 2 次下完後貓咪移動
        assertEquals(0, game.doubleBlockRemaining)
        assertTrue(initialCatPos.getNeighbors(11).contains(game.catPos))
    }

    @Test
    fun testUndoRestoresState() {
        val game = CatTrapGame(mode = CatTrapMode.LevelMode(1))
        val initialCatPos = game.catPos
        val initialSteps = game.stepsTaken

        val testTarget = HexCoord(0, 0)
        game.onCellClicked(testTarget)
        assertEquals(HexCellType.BLOCK, game.grid[testTarget])
        assertTrue(game.undoStack.isNotEmpty())

        val undoSuccess = game.undo()
        assertTrue(undoSuccess)
        assertEquals(initialCatPos, game.catPos)
        assertEquals(initialSteps, game.stepsTaken)
        assertEquals(null, game.grid[testTarget])
    }

    @Test
    fun testStarCalculation() {
        val game = CatTrapGame(mode = CatTrapMode.LevelMode(1))
        // 模擬獲勝
        val statusField = game.javaClass.getDeclaredField("status")
        statusField.isAccessible = true
        statusField.set(game, CatTrapGameStatus.VICTORY)

        val stepsField = game.javaClass.getDeclaredField("stepsTaken")
        stepsField.isAccessible = true

        val lvl = CatTrapLevels.getLevel(1)

        // 極限步數內且無道具 -> 3 星
        stepsField.set(game, lvl.starGoal.extremeSteps)
        assertEquals(3, game.calculateStars())

        // 目標步數內 -> 2 星
        stepsField.set(game, lvl.starGoal.targetSteps)
        val toolsField = game.javaClass.getDeclaredField("toolsUsedCount")
        toolsField.isAccessible = true
        toolsField.set(game, 1)
        assertEquals(2, game.calculateStars())

        // 超出目標步數 -> 1 星
        stepsField.set(game, lvl.starGoal.targetSteps + 10)
        assertEquals(1, game.calculateStars())
    }
}
