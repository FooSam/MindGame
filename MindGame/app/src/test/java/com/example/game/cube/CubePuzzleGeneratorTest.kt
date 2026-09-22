package com.example.game.cube

import com.example.data.model.GameDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class CubePuzzleGeneratorTest {

    @Test
    fun testGeneratePuzzleTotalCellsIsAlways54() {
        val random = Random(42)
        val difficulties = listOf(
            GameDifficulty.BEGINNER,
            GameDifficulty.INTERMEDIATE,
            GameDifficulty.ADVANCED
        )

        for (diff in difficulties) {
            for (i in 0 until 20) {
                val pieces = CubePuzzleGenerator.generatePuzzle(diff, random)
                val totalCells = pieces.sumOf { it.size }
                assertEquals("Total cells must be exactly 54 for difficulty $diff at iteration $i", 54, totalCells)
                assertTrue("Piece list should not be empty", pieces.isNotEmpty())

                // 檢查各拼塊座標均非空且正規化 minRow == 0 && minCol == 0
                for (piece in pieces) {
                    assertTrue("Piece must have cells", piece.cells.isNotEmpty())
                    assertEquals("minRow should be 0", 0, piece.cells.minOf { it.row })
                    assertEquals("minCol should be 0", 0, piece.cells.minOf { it.col })
                }
            }
        }
    }

    @Test
    fun testPieceRotationGeometry() {
        // 建立一個 L 形拼塊: (0,0), (1,0), (1,1)
        val piece = PuzzlePiece(
            id = 1,
            cells = listOf(CellPos(0, 0), CellPos(1, 0), CellPos(1, 1)),
            color = androidx.compose.ui.graphics.Color.Red
        )

        val rotated90 = piece.rotatedClockwise()
        assertEquals(90, rotated90.rotation)
        assertEquals(0, rotated90.cells.minOf { it.row })
        assertEquals(0, rotated90.cells.minOf { it.col })
        assertEquals(3, rotated90.size)

        val rotated180 = rotated90.rotatedClockwise()
        assertEquals(180, rotated180.rotation)
        assertEquals(0, rotated180.cells.minOf { it.row })
        assertEquals(0, rotated180.cells.minOf { it.col })

        val rotated270 = rotated180.rotatedClockwise()
        assertEquals(270, rotated270.rotation)

        val rotated360 = rotated270.rotatedClockwise()
        assertEquals(0, rotated360.rotation)
        // 旋轉 360 度後形狀應與原形狀一致
        assertEquals(piece.cells, rotated360.cells)
    }

    @Test
    fun testProjectionParallelLines() {
        // 正交投影核心特性：對邊旋轉後在 2D 螢幕上嚴格平行（向量相等）
        val p00 = projectPoint(0f, 0f, 1f, 30f, 45f, androidx.compose.ui.geometry.Offset(0f, 0f), 100f).first
        val p10 = projectPoint(1f, 0f, 1f, 30f, 45f, androidx.compose.ui.geometry.Offset(0f, 0f), 100f).first
        val p01 = projectPoint(0f, 1f, 1f, 30f, 45f, androidx.compose.ui.geometry.Offset(0f, 0f), 100f).first
        val p11 = projectPoint(1f, 1f, 1f, 30f, 45f, androidx.compose.ui.geometry.Offset(0f, 0f), 100f).first

        val topEdgeVecX = p10.x - p00.x
        val topEdgeVecY = p10.y - p00.y
        val bottomEdgeVecX = p11.x - p01.x
        val bottomEdgeVecY = p11.y - p01.y

        // 浮點數誤差範圍內嚴格相等 (平行且等長)
        assertEquals(topEdgeVecX, bottomEdgeVecX, 0.001f)
        assertEquals(topEdgeVecY, bottomEdgeVecY, 0.001f)
    }
}
