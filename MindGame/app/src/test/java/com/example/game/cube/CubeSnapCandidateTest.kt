package com.example.game.cube

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.findHoverSnapCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CubeSnapCandidateTest {

    @Test
    fun testSnapCandidateOnEmptyFrontFaceReturnsValid() {
        val canvasBounds = Rect(0f, 0f, 600f, 600f)
        val canvasCenter = Offset(300f, 300f)

        // 建立一個 2 格拼塊: (0,0), (0,1)
        val piece = PuzzlePiece(
            id = 1,
            cells = listOf(CellPos(0, 0), CellPos(0, 1)),
            color = Color.Cyan
        )

        // 初始標準視角 (rotX = 25f, rotY = 35f)
        val candidate = findHoverSnapCandidate(
            localTouch = canvasCenter,
            canvasBounds = canvasBounds,
            rotX = 25f,
            rotY = 35f,
            piece = piece,
            canPlace = { _, _, _, _ -> true } // 空棋盤全合法
        )

        assertNotNull("Candidate should not be null when hovering around center", candidate)
        assertTrue("Candidate should be valid on empty cube", candidate!!.isValid)
        assertTrue("Origin row must be within 0..2", candidate.originRow in 0..2)
        assertTrue("Origin col must be within 0..2", candidate.originCol in 0..2)
    }

    @Test
    fun testSnapCandidateWhenNoSpaceReturnsInvalidForWarningShadow() {
        val canvasBounds = Rect(0f, 0f, 600f, 600f)
        val canvasCenter = Offset(300f, 300f)

        val piece = PuzzlePiece(
            id = 2,
            cells = listOf(CellPos(0, 0), CellPos(1, 0)),
            color = Color.Red
        )

        // 模擬該面所有位置均被佔用 (canPlace 永遠 false)
        val candidate = findHoverSnapCandidate(
            localTouch = canvasCenter,
            canvasBounds = canvasBounds,
            rotX = 25f,
            rotY = 35f,
            piece = piece,
            canPlace = { _, _, _, _ -> false }
        )

        assertNotNull("Candidate should still return for warning preview", candidate)
        assertEquals("Should be marked invalid so piece cannot be dropped", false, candidate!!.isValid)
    }

    @Test
    fun testEdgeToleranceSnapFindsValidPosition() {
        val canvasBounds = Rect(0f, 0f, 600f, 600f)
        // 稍微偏離中心但仍在寬容半徑內
        val slightlyOffCenter = Offset(315f, 290f)

        val piece = PuzzlePiece(
            id = 3,
            cells = listOf(CellPos(0, 0)),
            color = Color.Green
        )

        val candidate = findHoverSnapCandidate(
            localTouch = slightlyOffCenter,
            canvasBounds = canvasBounds,
            rotX = 25f,
            rotY = 35f,
            piece = piece,
            canPlace = { _, r, c, _ -> r in 0..2 && c in 0..2 }
        )

        assertNotNull("Edge tolerance snapping should find candidate", candidate)
        assertTrue("Should be valid", candidate!!.isValid)
    }
}
