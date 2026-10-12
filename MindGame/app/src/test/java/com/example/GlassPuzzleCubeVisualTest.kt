package com.example

import com.example.data.model.GameDifficulty
import com.example.game.cube.CellPos
import com.example.game.cube.CubeFace
import com.example.game.cube.CubePuzzleGenerator
import com.example.game.cube.GlassTheme
import com.example.game.cube.PuzzlePiece
import com.example.game.cube.getCell3DVertices
import com.example.game.cube.projectPoint
import com.example.game.cube.rotatePoint
import com.example.ui.screens.calculateCubeStars
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * 《六面合體》2024-2026 次世代手遊視覺與核心規範自動化單元測試
 */
class GlassPuzzleCubeVisualTest {

    @Test
    fun testGlassThemesCompleteness() {
        assertEquals("6大水晶玻璃色彩主題完整性", 6, GlassTheme.entries.size)
        val sky = GlassTheme.SKY_BLUE
        assertEquals("淡藍晶瑩", sky.nameZh)
        assertNotNull("基礎色不可為空", sky.baseColor)
        assertNotNull("高光色不可為空", sky.edgeHighlightColor)
        assertNotNull("光暈色不可為空", sky.glowColor)

        for (theme in GlassTheme.entries) {
            assertTrue("主題名稱不得為空", theme.nameZh.isNotEmpty())
            assertTrue("英文名稱不得為空", theme.nameEn.isNotEmpty())
        }
    }

    @Test
    fun testCubeStarRatingCalculation() {
        // 初級: <= 90s -> 3星, <= 180s -> 2星, 其他 1星
        assertEquals(3, calculateCubeStars(GameDifficulty.BEGINNER, 80_000L))
        assertEquals(3, calculateCubeStars(GameDifficulty.BEGINNER, 90_000L))
        assertEquals(2, calculateCubeStars(GameDifficulty.BEGINNER, 91_000L))
        assertEquals(2, calculateCubeStars(GameDifficulty.BEGINNER, 180_000L))
        assertEquals(1, calculateCubeStars(GameDifficulty.BEGINNER, 181_000L))

        // 中級: <= 180s -> 3星, <= 360s -> 2星, 其他 1星
        assertEquals(3, calculateCubeStars(GameDifficulty.INTERMEDIATE, 150_000L))
        assertEquals(3, calculateCubeStars(GameDifficulty.INTERMEDIATE, 180_000L))
        assertEquals(2, calculateCubeStars(GameDifficulty.INTERMEDIATE, 181_000L))
        assertEquals(2, calculateCubeStars(GameDifficulty.INTERMEDIATE, 360_000L))
        assertEquals(1, calculateCubeStars(GameDifficulty.INTERMEDIATE, 400_000L))

        // 高級: <= 300s -> 3星, <= 600s -> 2星, 其他 1星
        assertEquals(3, calculateCubeStars(GameDifficulty.HARD, 250_000L))
        assertEquals(3, calculateCubeStars(GameDifficulty.HARD, 300_000L))
        assertEquals(2, calculateCubeStars(GameDifficulty.HARD, 301_000L))
        assertEquals(2, calculateCubeStars(GameDifficulty.HARD, 600_000L))
        assertEquals(1, calculateCubeStars(GameDifficulty.HARD, 700_000L))
    }

    @Test
    fun testCubeFaceVerticesAndNormals() {
        assertEquals("6個面完整性", 6, CubeFace.entries.size)
        val center = Offset(200f, 200f)
        val radius = 100f

        for (face in CubeFace.entries) {
            for (r in 0..2) {
                for (c in 0..2) {
                    val vertices = getCell3DVertices(face, r, c)
                    assertEquals("每個單元格必須為4邊形頂點", 4, vertices.size)
                    for (v in vertices) {
                        assertEquals("3D頂點包含xyz", 3, v.size)
                        val proj = projectPoint(v[0], v[1], v[2], 25f, 35f, center, radius)
                        assertTrue("投影 X 不可為 NaN", !proj.first.x.isNaN())
                        assertTrue("投影 Y 不可為 NaN", !proj.first.y.isNaN())
                        assertTrue("深度 Z 不可為 NaN", !proj.second.isNaN())
                    }
                }
            }
        }
    }

    @Test
    fun testPieceRotationGeometry() {
        // L 型拼塊
        val initialCells = listOf(CellPos(0, 0), CellPos(1, 0), CellPos(2, 0), CellPos(2, 1))
        val piece = PuzzlePiece(
            id = 1,
            cells = initialCells,
            color = Color(0xFF38BDF8)
        )
        assertEquals("初始尺寸為4格", 4, piece.size)

        var rotated = piece
        for (i in 1..4) {
            rotated = rotated.rotatedClockwise()
            assertEquals("旋轉後格數必須恆為4格", 4, rotated.size)
            val minR = rotated.cells.minOf { it.row }
            val minC = rotated.cells.minOf { it.col }
            assertEquals("旋轉後正規化座標 minRow 必須為 0", 0, minR)
            assertEquals("旋轉後正規化座標 minCol 必須為 0", 0, minC)
        }
        assertEquals("旋轉 360 度後回到原始角度", piece.cells, rotated.cells)
    }

    @Test
    fun test54CellsPartitionGuarantee() {
        val rand = Random(42L)
        val difficulties = listOf(GameDifficulty.BEGINNER, GameDifficulty.INTERMEDIATE, GameDifficulty.HARD)

        for (diff in difficulties) {
            val pieces = CubePuzzleGenerator.generatePuzzle(diff, rand)
            val totalCells = pieces.sumOf { it.size }
            assertEquals("全難度拼塊加總格數必須精確等於 54 格", 54, totalCells)
        }
    }
}
