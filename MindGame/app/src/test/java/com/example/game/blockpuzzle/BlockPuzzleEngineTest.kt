package com.example.game.blockpuzzle

import org.junit.Assert.*
import org.junit.Test

class BlockPuzzleEngineTest {

    @Test
    fun testBlockShapeRotateClockwise() {
        // 建立一個橫向 3 格直線: (0,0), (0,1), (0,2)
        val lineH = BlockShape(
            id = "test_line",
            coords = listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2))
        )
        assertEquals(3, lineH.width)
        assertEquals(1, lineH.height)

        // 順時針旋轉 90 度 -> 應該變成直向 3 格: (0,0), (1,0), (2,0)
        val lineV = lineH.rotateClockwise()
        assertEquals(1, lineV.width)
        assertEquals(3, lineV.height)
        assertEquals(listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0)), lineV.coords)

        // 再次旋轉 90 度 -> 橫向 3 格
        val lineH2 = lineV.rotateClockwise()
        assertEquals(3, lineH2.width)
        assertEquals(1, lineH2.height)
    }

    @Test
    fun testCanPlaceAndPlacement() {
        val board = Array(8) { IntArray(8) { 0 } }
        val square2x2 = BlockShape(
            id = "sq2",
            coords = listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)),
            colorIndex = 2
        )

        // 合法位置 (0, 0)
        assertTrue(BlockPuzzleEngine.canPlace(square2x2, 0, 0, board))

        // 超出右邊界 (0, 7) (width=2, 7+1=8 超出)
        assertFalse(BlockPuzzleEngine.canPlace(square2x2, 0, 7, board))

        // 超出下邊界 (7, 0)
        assertFalse(BlockPuzzleEngine.canPlace(square2x2, 7, 0, board))

        // 放置方塊
        BlockPuzzleEngine.placeShape(square2x2, 0, 0, board)
        assertEquals(3, board[0][0]) // colorIndex + 1
        assertEquals(3, board[0][1])
        assertEquals(3, board[1][0])
        assertEquals(3, board[1][1])

        // 同一位置不能重複放置
        assertFalse(BlockPuzzleEngine.canPlace(square2x2, 0, 0, board))
    }

    @Test
    fun testEliminationAndCrossBoom() {
        val board = Array(8) { IntArray(8) { 0 } }

        // 填滿第 2 列 (Row 2) 與第 4 行 (Col 4)
        for (c in 0 until 8) {
            board[2][c] = 1
        }
        for (r in 0 until 8) {
            board[r][4] = 2
        }

        val result = BlockPuzzleEngine.checkEliminations(board, currentCombo = 0)
        assertEquals(listOf(2), result.fullRows)
        assertEquals(listOf(4), result.fullCols)
        assertEquals(listOf(Pair(2, 4)), result.crossPoints) // 十字交會點
        assertEquals(15, result.clearedCellCount) // 8 + 8 - 1 (重疊 1 格)
        assertTrue(result.scoreEarned > 200)

        // 執行清除
        BlockPuzzleEngine.clearLines(board, result.fullRows, result.fullCols)
        for (c in 0 until 8) {
            assertEquals(0, board[2][c])
        }
        for (r in 0 until 8) {
            assertEquals(0, board[r][4])
        }
    }

    @Test
    fun testGameOverDetection() {
        val board = Array(8) { IntArray(8) { 1 } } // 全滿棋盤

        val dot = BlockShape("dot", listOf(Pair(0, 0)))
        val pieces = listOf<BlockShape?>(dot, null, null)

        // 全滿棋盤連 1 格都放不下 -> Game Over
        assertTrue(BlockPuzzleEngine.isGameOver(pieces, board))

        // 空出 1 格 (3, 3)
        board[3][3] = 0
        // 此時 dot 放得下 -> Not Game Over
        assertFalse(BlockPuzzleEngine.isGameOver(pieces, board))

        // 若備選方塊是 2x2 正方形，1 個空格放不下 -> Game Over
        val sq2 = BlockShape("sq2", listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)))
        assertTrue(BlockPuzzleEngine.isGameOver(listOf(sq2), board))
    }
}
