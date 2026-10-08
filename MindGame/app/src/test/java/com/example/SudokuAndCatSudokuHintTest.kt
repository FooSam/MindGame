package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GameDifficulty
import com.example.game.catsudoku.CatCellState
import com.example.ui.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SudokuAndCatSudokuHintTest {

    @Test
    fun testSudokuHint_FreeAndAdFlow() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(app)

        // 1. 初始化數獨
        viewModel.setupSudokuGame(GameDifficulty.BEGINNER)
        assertEquals("新局開始應有 1 次免費提示", 1, viewModel.sudokuFreeHintsRemaining.value)
        assertFalse("初始不應顯示廣告提示彈窗", viewModel.sudokuShowAdHintDialog.value)

        val initialBoard = viewModel.sudokuInitialBoard.value
        val solutionBoard = viewModel.sudokuSolutionBoard.value
        val playerBoardBefore = viewModel.sudokuPlayerBoard.value

        // 2. 第一次按下提示（消耗免費次數）
        viewModel.requestSudokuHint(null)

        assertEquals("使用後免費次數應變為 0", 0, viewModel.sudokuFreeHintsRemaining.value)
        assertFalse("免費次數使用時不應彈出看廣告視窗", viewModel.sudokuShowAdHintDialog.value)

        val playerBoardAfter = viewModel.sudokuPlayerBoard.value
        val selectedIdx = viewModel.selectedSudokuCellIndex.value
        assertNotNull("提示後應選中被提示的格子", selectedIdx)
        assertEquals("提示格應填入正解數值", solutionBoard[selectedIdx!!], playerBoardAfter[selectedIdx])

        // 3. 第二次按下提示（免費次數已用盡，應彈出看廣告視窗）
        viewModel.requestSudokuHint(null)
        assertTrue("免費次數為 0 時再次點擊應彈出看廣告視窗", viewModel.sudokuShowAdHintDialog.value)

        // 4. 關閉彈窗
        viewModel.closeSudokuHintDialog()
        assertFalse("關閉後彈窗應消失", viewModel.sudokuShowAdHintDialog.value)

        // 5. 重新開局，免費次數應重置為 1
        viewModel.resetSudokuGame()
        assertEquals("重置或新局開始時，免費提示次數應重置為 1", 1, viewModel.sudokuFreeHintsRemaining.value)
    }

    @Test
    fun testCatSudokuHint_FreeAndAdFlow() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(app)

        // 1. 初始化貓咪數獨
        viewModel.setupCatSudokuGame(GameDifficulty.BEGINNER)
        assertEquals("新局開始應有 1 次免費提示", 1, viewModel.catSudokuFreeHintsRemaining.value)
        assertFalse("初始不應顯示廣告提示彈窗", viewModel.catSudokuShowAdHintDialog.value)

        val initialCats = viewModel.catSudokuGrid.value.count { it.state == CatCellState.CAT }

        // 2. 第一次按下提示（消耗免費次數）
        viewModel.requestCatSudokuHint(null)

        assertEquals("使用後免費次數應變為 0", 0, viewModel.catSudokuFreeHintsRemaining.value)
        assertFalse("免費次數使用時不應彈出看廣告視窗", viewModel.catSudokuShowAdHintDialog.value)

        val afterCats = viewModel.catSudokuGrid.value.count { it.state == CatCellState.CAT }
        assertTrue("提示後盤面上應放置了正確的貓咪", afterCats >= initialCats + 1)

        // 3. 第二次按下提示（免費次數已用盡，應彈出看廣告視窗）
        viewModel.requestCatSudokuHint(null)
        assertTrue("免費次數為 0 時再次點擊應彈出看廣告視窗", viewModel.catSudokuShowAdHintDialog.value)

        // 4. 關閉彈窗
        viewModel.closeCatSudokuHintDialog()
        assertFalse("關閉後彈窗應消失", viewModel.catSudokuShowAdHintDialog.value)

        // 5. 重新開局，免費次數應重置為 1
        viewModel.resetCatSudokuGame()
        assertEquals("重置或新局開始時，免費提示次數應重置為 1", 1, viewModel.catSudokuFreeHintsRemaining.value)
    }
}
