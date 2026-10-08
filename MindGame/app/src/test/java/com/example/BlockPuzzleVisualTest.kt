package com.example

import com.example.game.blockpuzzle.BlockPuzzleEngine
import com.example.game.blockpuzzle.BlockShape
import com.example.ui.screens.CLAY_COLORS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPuzzleVisualTest {

    @Test
    fun testClayColorsPalette() {
        assertEquals("陶藝自然色盤應為 6 種經典色", 6, CLAY_COLORS.size)
        for (color in CLAY_COLORS) {
            assertNotNull("顏色不可為空", color)
            assertTrue("顏色透明度必須大於0", color.alpha > 0f)
        }
    }

    @Test
    fun testStarRatingCalculation() {
        fun calculateStars(score: Int): Int {
            return when {
                score >= 2500 -> 3
                score >= 1200 -> 2
                else -> 1
            }
        }

        assertEquals(3, calculateStars(3000))
        assertEquals(3, calculateStars(2500))
        assertEquals(2, calculateStars(2499))
        assertEquals(2, calculateStars(1200))
        assertEquals(1, calculateStars(1199))
        assertEquals(1, calculateStars(500))
    }

    @Test
    fun testBoard8x8Dimensions() {
        val emptyBoard = Array(8) { IntArray(8) { 0 } }
        assertEquals(8, emptyBoard.size)
        for (row in emptyBoard) {
            assertEquals(8, row.size)
        }
    }

    @Test
    fun testShapeRotationFourTimesBackToOriginal() {
        val testShapes = listOf(
            BlockShape("line3", listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2))),
            BlockShape("corner", listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1))),
            BlockShape("square2", listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)))
        )

        for (shape in testShapes) {
            val r1 = shape.rotateClockwise()
            val r2 = r1.rotateClockwise()
            val r3 = r2.rotateClockwise()
            val r4 = r3.rotateClockwise()

            assertEquals(shape.cellCount, r4.cellCount)
            assertEquals(shape.width, r4.width)
            assertEquals(shape.height, r4.height)
            assertEquals(shape.coords, r4.coords)
        }
    }

    @Test
    fun testThreePiecesGeneration() {
        val pieces = BlockPuzzleEngine.generateThreePieces()
        assertEquals(3, pieces.size)
        for (p in pieces) {
            assertNotNull(p)
            assertTrue(p.cellCount > 0)
        }
    }
}
