package com.example

import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.data.model.LaserDirection
import com.example.data.model.LaserPiece
import com.example.data.model.OpticalPieceType
import com.example.data.model.RankingType
import com.example.game.lasermaze.LaserMazeGenerator
import com.example.game.lasermaze.LaserRaycaster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 《雷射迷宮》(Laser Maze) 光學演算法與進階機關自動化單元測試
 */
class LaserMazeTest {

    @Test
    fun testLaserMazeRankingTypeIsTimeAsc() {
        assertEquals(RankingType.TIME_ASC, GameType.LASER_MAZE.rankingType)
    }

    @Test
    fun testSingleMirrorReflections() {
        // rotation = 0: 斜切 / (有效面朝右下)
        assertEquals(LaserDirection.DOWN, LaserRaycaster.calculateSingleMirrorReflection(LaserDirection.LEFT, 0))
        assertEquals(LaserDirection.RIGHT, LaserRaycaster.calculateSingleMirrorReflection(LaserDirection.UP, 0))
        assertNull(LaserRaycaster.calculateSingleMirrorReflection(LaserDirection.RIGHT, 0))
        assertNull(LaserRaycaster.calculateSingleMirrorReflection(LaserDirection.DOWN, 0))

        // rotation = 1: 斜切 \ (順時針轉 90°，有效面朝左下)
        assertEquals(LaserDirection.DOWN, LaserRaycaster.calculateSingleMirrorReflection(LaserDirection.RIGHT, 1))
        assertEquals(LaserDirection.LEFT, LaserRaycaster.calculateSingleMirrorReflection(LaserDirection.UP, 1))
        assertNull(LaserRaycaster.calculateSingleMirrorReflection(LaserDirection.LEFT, 1))
        assertNull(LaserRaycaster.calculateSingleMirrorReflection(LaserDirection.DOWN, 1))
    }

    @Test
    fun testDoubleMirrorReflections() {
        // rotation % 2 == 0: 斜切 /
        assertEquals(LaserDirection.DOWN, LaserRaycaster.calculateDoubleMirrorReflection(LaserDirection.LEFT, 0))
        assertEquals(LaserDirection.LEFT, LaserRaycaster.calculateDoubleMirrorReflection(LaserDirection.DOWN, 0))
        assertEquals(LaserDirection.UP, LaserRaycaster.calculateDoubleMirrorReflection(LaserDirection.RIGHT, 0))
        assertEquals(LaserDirection.RIGHT, LaserRaycaster.calculateDoubleMirrorReflection(LaserDirection.UP, 0))

        // rotation % 2 == 1: 斜切 \
        assertEquals(LaserDirection.DOWN, LaserRaycaster.calculateDoubleMirrorReflection(LaserDirection.RIGHT, 1))
        assertEquals(LaserDirection.RIGHT, LaserRaycaster.calculateDoubleMirrorReflection(LaserDirection.DOWN, 1))
        assertEquals(LaserDirection.UP, LaserRaycaster.calculateDoubleMirrorReflection(LaserDirection.LEFT, 1))
        assertEquals(LaserDirection.LEFT, LaserRaycaster.calculateDoubleMirrorReflection(LaserDirection.UP, 1))
    }

    @Test
    fun testBeamSplitterAndTunnelRaytracing() {
        val size = 5
        val grid = MutableList(size) { MutableList<LaserPiece?>(size) { null } }
        grid[0][0] = LaserPiece(type = OpticalPieceType.EMITTER, rotation = 1, isFixed = true)
        grid[0][2] = LaserPiece(type = OpticalPieceType.BEAM_SPLITTER, rotation = 1, isFixed = false)
        grid[1][2] = LaserPiece(type = OpticalPieceType.TUNNEL, rotation = 0, isFixed = true)
        grid[0][4] = LaserPiece(type = OpticalPieceType.RECEIVER, isFixed = true)
        grid[3][2] = LaserPiece(type = OpticalPieceType.RECEIVER, isFixed = true)

        val receivers = listOf(Pair(0, 4), Pair(3, 2))

        val result = LaserRaycaster.trace(
            gridSize = size,
            grid = grid,
            emitterRow = 0,
            emitterCol = 0,
            emitterDir = LaserDirection.RIGHT,
            receivers = receivers
        )

        assertEquals(2, result.litReceiverIndices.size)
        assertTrue(result.isCleared)
    }

    @Test
    fun testColorFilterAndResonanceReceiver() {
        val size = 5
        val grid = MutableList(size) { MutableList<LaserPiece?>(size) { null } }
        // (0, 0) 發射青光 (CYAN)
        grid[0][0] = LaserPiece(type = OpticalPieceType.EMITTER, rotation = 1, isFixed = true)
        // (0, 2) 紅色濾鏡 -> 轉為紅光
        grid[0][2] = LaserPiece(type = OpticalPieceType.FILTER_RED, isFixed = true)
        // (0, 4) 紅色共振水晶
        grid[0][4] = LaserPiece(type = OpticalPieceType.RECEIVER, isFixed = true, requiredColorHex = LaserRaycaster.COLOR_RED)

        val receivers = listOf(Pair(0, 4))
        val result = LaserRaycaster.trace(
            gridSize = size,
            grid = grid,
            emitterRow = 0,
            emitterCol = 0,
            emitterDir = LaserDirection.RIGHT,
            receivers = receivers
        )

        assertTrue("紅光濾鏡成功匹配紅色共振水晶", result.isCleared)
        assertEquals(1, result.litReceiverIndices.size)
    }

    @Test
    fun testSpaceTimePortal() {
        val size = 6
        val grid = MutableList(size) { MutableList<LaserPiece?>(size) { null } }
        // (0, 0) 發射器朝右
        grid[0][0] = LaserPiece(type = OpticalPieceType.EMITTER, rotation = 1, isFixed = true)
        // (0, 2) 傳送門 A，目標連到 (4, 2) 傳送門 B
        grid[0][2] = LaserPiece(type = OpticalPieceType.PORTAL, isFixed = true, portalTarget = Pair(4, 2))
        grid[4][2] = LaserPiece(type = OpticalPieceType.PORTAL, isFixed = true, portalTarget = Pair(0, 2))
        // (4, 5) 水晶
        grid[4][5] = LaserPiece(type = OpticalPieceType.RECEIVER, isFixed = true)

        val receivers = listOf(Pair(4, 5))
        val result = LaserRaycaster.trace(
            gridSize = size,
            grid = grid,
            emitterRow = 0,
            emitterCol = 0,
            emitterDir = LaserDirection.RIGHT,
            receivers = receivers
        )

        assertTrue("時空傳送門成功躍遷並抵達水晶", result.isCleared)
        assertEquals(1, result.litReceiverIndices.size)
    }

    @Test
    fun testObstacleAbsorberStopsRay() {
        val size = 5
        val grid = MutableList(size) { MutableList<LaserPiece?>(size) { null } }
        grid[0][0] = LaserPiece(type = OpticalPieceType.EMITTER, rotation = 1, isFixed = true)
        grid[0][1] = LaserPiece(type = OpticalPieceType.OBSTACLE_ABSORBER, isFixed = true)
        grid[0][4] = LaserPiece(type = OpticalPieceType.RECEIVER, isFixed = true)

        val receivers = listOf(Pair(0, 4))
        val result = LaserRaycaster.trace(
            gridSize = size,
            grid = grid,
            emitterRow = 0,
            emitterCol = 0,
            emitterDir = LaserDirection.RIGHT,
            receivers = receivers
        )

        assertEquals(0, result.litReceiverIndices.size)
        assertFalse(result.isCleared)
    }

    @Test
    fun testLaserMazeCategoryIsDeduction() {
        assertEquals(com.example.data.model.GameCategory.DEDUCTION, GameType.LASER_MAZE.category)
        for (type in GameType.entries) {
            assertNotNull(type.category)
        }
    }

    @Test
    fun testGridDimensionsAndBorderAnchorMandate() {
        val random = Random(42)
        val expectedSizes = mapOf(
            GameDifficulty.BEGINNER to 5,
            GameDifficulty.INTERMEDIATE to 6,
            GameDifficulty.ADVANCED to 8,
            GameDifficulty.HARD to 9,
            GameDifficulty.HELL to 10,
            GameDifficulty.EPIC to 12
        )

        for (diff in GameDifficulty.entries) {
            val expectedSize = expectedSizes[diff]!!
            for (level in listOf(1, 5, 10)) {
                val board = LaserMazeGenerator.generate(diff, level, random)
                assertNotNull("關卡生成結果不應為空", board)
                assertEquals("棋盤規格應為 $expectedSize x $expectedSize (diff: $diff)", expectedSize, board.gridSize)
                assertTrue("必須至少有 1 顆接收水晶", board.receivers.isNotEmpty())

                // 驗證起點發射器 100% 位於外圍邊界
                val isEmitterOnBorder = (board.emitterRow == 0 || board.emitterRow == board.gridSize - 1 ||
                        board.emitterCol == 0 || board.emitterCol == board.gridSize - 1)
                assertTrue("發射器必須位於最外圍邊框 (diff: $diff)", isEmitterOnBorder)

                // 驗證初始盤面不可開局即勝
                assertFalse("初始盤面絕對不可開局即勝 (diff: $diff, level: $level)", board.isCleared)

                // 驗證可操作鏡片數量
                val controllableCount = board.grid.flatten().count {
                    it != null && !it.isFixed && (it.type == OpticalPieceType.MIRROR_SINGLE || it.type == OpticalPieceType.MIRROR_DOUBLE || it.type == OpticalPieceType.BEAM_SPLITTER)
                }
                assertTrue("可操作鏡片數量至少 >= 2 (實際: $controllableCount, diff: $diff)", controllableCount >= 2)

                // 驗證長程穿梭曼哈頓距離門檻
                val maxDist = board.receivers.maxOf {
                    Math.abs(board.emitterRow - it.first) + Math.abs(board.emitterCol - it.second)
                }
                val expectedMinDist = when (diff) {
                    GameDifficulty.BEGINNER -> 4
                    GameDifficulty.INTERMEDIATE -> 5
                    GameDifficulty.ADVANCED -> 7
                    GameDifficulty.HARD -> 8
                    GameDifficulty.HELL -> 9
                    GameDifficulty.EPIC -> 11
                }
                assertTrue("曼哈頓距離應達標 (實際: $maxDist, 預期: $expectedMinDist, diff: $diff)", maxDist >= expectedMinDist)
            }
        }
    }

    @Test
    fun testGeneratorBatchRobustnessAndNoDirectLaserToCrystal() {
        val random = Random(20261008)
        for (i in 0 until 50) {
            val diff = GameDifficulty.entries[i % GameDifficulty.entries.size]
            val level = (i % 10) + 1
            val board = LaserMazeGenerator.generate(diff, level, random)

            // 1. 開局絕不處於過關狀態
            assertFalse("題局 $i 開局不可過關", board.isCleared)

            // 2. 可旋轉鏡片數量至少 >= 2
            val mirrors = board.grid.flatten().count {
                it != null && !it.isFixed && (it.type == OpticalPieceType.MIRROR_SINGLE || it.type == OpticalPieceType.MIRROR_DOUBLE || it.type == OpticalPieceType.BEAM_SPLITTER)
            }
            assertTrue("題局 $i 鏡片數量不足: $mirrors", mirrors >= 2)

            // 3. 起點射線上不可直達任何水晶
            var cr = board.emitterRow + board.emitterDir.dy
            var cc = board.emitterCol + board.emitterDir.dx
            while (cr in 0 until board.gridSize && cc in 0 until board.gridSize) {
                assertFalse("題局 $i 起點直線不可直接射中水晶", Pair(cr, cc) in board.receivers)
                if (board.grid[cr][cc] != null) break
                cr += board.emitterDir.dy
                cc += board.emitterDir.dx
            }
        }
    }
}
