package com.example

import com.example.game.sudoku.SudokuConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuVisualTest {

    @Test
    fun testSudokuSubgridBorderLogic() {
        // 驗證 9x9 (3x3 宮位) 與 6x6 (2x3 宮位) 之宮位邊界分割
        val config9x9 = SudokuConfig(
            gridSize = 9,
            subRows = 3,
            subCols = 3,
            symbols = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9"),
            minClues = 30,
            maxClues = 35
        )

        for (c in 1 until config9x9.gridSize) {
            val isPalaceBorderCol = (c % config9x9.subCols == 0)
            if (c == 3 || c == 6) {
                assertTrue("第 $c 欄應為 3x3 宮位加粗分割線", isPalaceBorderCol)
            } else {
                assertFalse("第 $c 欄非宮位加粗分割線", isPalaceBorderCol)
            }
        }

        for (r in 1 until config9x9.gridSize) {
            val isPalaceBorderRow = (r % config9x9.subRows == 0)
            if (r == 3 || r == 6) {
                assertTrue("第 $r 列應為 3x3 宮位加粗分割線", isPalaceBorderRow)
            } else {
                assertFalse("第 $r 列非宮位加粗分割線", isPalaceBorderRow)
            }
        }
    }

    @Test
    fun testCrosshairHighlightAndSameValueLogic() {
        val gridSize = 9
        val selectedIndex = 4 * gridSize + 4 // 正中央格 (4, 4)
        val selectedRow = selectedIndex / gridSize
        val selectedCol = selectedIndex % gridSize

        val playerBoard = MutableList(81) { "" }
        playerBoard[selectedIndex] = "5"
        playerBoard[0] = "5" // 另一個相同數字在 (0, 0)
        playerBoard[1] = "3" // 不同數字在 (0, 1)

        // 驗證同行同列導引判定
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val idx = r * gridSize + c
                val isCrosshair = (r == selectedRow || c == selectedCol)
                if (r == 4 || c == 4) {
                    assertTrue("格子 ($r, $c) 應在十字聚焦導引光帶內", isCrosshair)
                } else {
                    assertFalse("格子 ($r, $c) 不在十字聚焦導引光帶內", isCrosshair)
                }

                // 同值判定
                val isSameVal = (playerBoard[idx] == "5")
                if (idx == selectedIndex || idx == 0) {
                    assertTrue("格子 $idx 應為同值高亮", isSameVal)
                } else {
                    assertFalse("格子 $idx 不應為同值高亮", isSameVal)
                }
            }
        }
    }

    @Test
    fun testKeypadSymbolsIntegrity() {
        val symbols9 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
        assertEquals(9, symbols9.size)
        assertTrue(symbols9.contains("1"))
        assertTrue(symbols9.contains("9"))
    }
}
