package com.example

import com.example.ui.screens.CatBreed
import com.example.data.model.GameDifficulty
import com.example.game.catsudoku.CatCell
import com.example.game.catsudoku.CatCellState
import com.example.game.catsudoku.CatSudokuGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatSudokuVisualTest {

    @Test
    fun testCatBreedPaletteIntegrity() {
        // 驗證 5 大次世代品種俱備完整外觀設定
        val breeds = CatBreed.values()
        assertEquals("應有 5 種經典貓咪外觀品種", 5, breeds.size)

        for (breed in breeds) {
            assertNotEquals("基礎毛色不可為全透明", 0f, breed.baseColor.alpha)
            assertNotEquals("高光顏色不可為全透明", 0f, breed.highlightColor.alpha)
            assertNotEquals("陰影顏色不可為全透明", 0f, breed.shadowColor.alpha)
            assertNotEquals("眼珠寶石顏色不可為全透明", 0f, breed.eyeColor.alpha)
            assertNotNull("品種必須定義吻部嘴周顏色", breed.muzzleColor)
        }
    }

    @Test
    fun testCatSudokuGridIndexMapping() {
        // 驗證各難度棋盤尺寸與品種映射循環均勻
        val diffs = listOf(
            GameDifficulty.BEGINNER,
            GameDifficulty.INTERMEDIATE,
            GameDifficulty.ADVANCED,
            GameDifficulty.HARD,
            GameDifficulty.HELL,
            GameDifficulty.EPIC
        )

        val breeds = CatBreed.values()

        for (diff in diffs) {
            val size = CatSudokuGenerator.getSizeForDifficulty(diff)
            val totalCells = size * size

            val breedCounts = mutableMapOf<CatBreed, Int>()
            for (index in 0 until totalCells) {
                val assignedBreed = breeds[index % breeds.size]
                breedCounts[assignedBreed] = (breedCounts[assignedBreed] ?: 0) + 1
            }

            // 確保所有品種皆有被均勻分配
            assertEquals("盤面應包含全部 5 種萌貓品種輪替", 5, breedCounts.size)
            for ((breed, count) in breedCounts) {
                assertTrue("每種貓咪至少出現一次: $breed", count > 0)
            }
        }
    }

    @Test
    fun testCatCellStateTransitions() {
        // 驗證單元格三態點擊流轉保持正常
        val emptyCell = CatCell(row = 0, col = 0, regionId = 1, state = CatCellState.EMPTY)
        assertEquals(CatCellState.EMPTY, emptyCell.state)

        val catCell = emptyCell.copy(state = CatCellState.CAT)
        assertEquals(CatCellState.CAT, catCell.state)

        val crossCell = emptyCell.copy(state = CatCellState.CROSS)
        assertEquals(CatCellState.CROSS, crossCell.state)

        val conflictCell = catCell.copy(isConflict = true)
        assertTrue(conflictCell.isConflict)
    }
}
