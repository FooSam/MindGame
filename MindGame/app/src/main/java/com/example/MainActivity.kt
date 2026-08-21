package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ChangeNameDialog
import com.example.ui.components.LeaderboardDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.CatSudokuScreen
import com.example.ui.screens.CategoryScreen
import com.example.ui.screens.FocusGameScreen
import com.example.ui.screens.FocusTrainScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SpeedMatchScreen
import com.example.ui.screens.SudokuScreen
import com.example.game.sudoku.SudokuConfig
import com.example.game.sudoku.SudokuGameConfig
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(themeStyle = appTheme) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val playerName by viewModel.playerName.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedGameType by viewModel.selectedGameType.collectAsStateWithLifecycle()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()

    // Focus Game state
    val gameStatus by viewModel.gameStatus.collectAsStateWithLifecycle()
    val gridNumbers by viewModel.gridNumbers.collectAsStateWithLifecycle()
    val currentTarget by viewModel.currentTarget.collectAsStateWithLifecycle()
    val clearedIndices by viewModel.clearedIndices.collectAsStateWithLifecycle()
    val elapsedTimeMillis by viewModel.elapsedTimeMillis.collectAsStateWithLifecycle()
    val wrongTapIndex by viewModel.wrongTapIndex.collectAsStateWithLifecycle()
    val lastCompletedTimeMillis by viewModel.lastCompletedTimeMillis.collectAsStateWithLifecycle()

    // Focus Train state (Dynamic Schulte Wheel)
    val focusTrainStatus by viewModel.focusTrainStatus.collectAsStateWithLifecycle()
    val focusTrainConfig by viewModel.focusTrainConfig.collectAsStateWithLifecycle()
    val focusTrainNumbers by viewModel.focusTrainNumbers.collectAsStateWithLifecycle()
    val focusTrainTarget by viewModel.focusTrainTarget.collectAsStateWithLifecycle()
    val focusTrainClearedIndices by viewModel.focusTrainClearedIndices.collectAsStateWithLifecycle()
    val focusTrainElapsedTimeMillis by viewModel.focusTrainElapsedTimeMillis.collectAsStateWithLifecycle()
    val focusTrainWrongTapIndex by viewModel.focusTrainWrongTapIndex.collectAsStateWithLifecycle()
    val focusTrainWrongCount by viewModel.focusTrainWrongCount.collectAsStateWithLifecycle()
    val focusTrainLastCompletedTimeMillis by viewModel.focusTrainLastCompletedTimeMillis.collectAsStateWithLifecycle()

    // Speed Match state
    val speedMatchStatus by viewModel.speedMatchStatus.collectAsStateWithLifecycle()
    val speedMatchGrid by viewModel.speedMatchGrid.collectAsStateWithLifecycle()
    val selectedMatchCellIndex by viewModel.selectedMatchCellIndex.collectAsStateWithLifecycle()
    val correctRounds by viewModel.correctRounds.collectAsStateWithLifecycle()
    val wrongTaps by viewModel.wrongTaps.collectAsStateWithLifecycle()
    val speedMatchRemainingMs by viewModel.speedMatchRemainingMs.collectAsStateWithLifecycle()
    val isWrongFlash by viewModel.isWrongFlash.collectAsStateWithLifecycle()

    // Sudoku state
    val sudokuStatus by viewModel.sudokuStatus.collectAsStateWithLifecycle()
    val sudokuElapsedTimeMillis by viewModel.sudokuElapsedTimeMillis.collectAsStateWithLifecycle()
    val sudokuWrongCount by viewModel.sudokuWrongCount.collectAsStateWithLifecycle()
    val sudokuInitialBoard by viewModel.sudokuInitialBoard.collectAsStateWithLifecycle()
    val sudokuSolutionBoard by viewModel.sudokuSolutionBoard.collectAsStateWithLifecycle()
    val sudokuPlayerBoard by viewModel.sudokuPlayerBoard.collectAsStateWithLifecycle()
    val sudokuNotesBoard by viewModel.sudokuNotesBoard.collectAsStateWithLifecycle()
    val sudokuConfig by viewModel.sudokuConfig.collectAsStateWithLifecycle()
    val selectedSudokuCellIndex by viewModel.selectedSudokuCellIndex.collectAsStateWithLifecycle()
    val isPencilMode by viewModel.isPencilMode.collectAsStateWithLifecycle()

    // Cat Sudoku state
    val catSudokuStatus by viewModel.catSudokuStatus.collectAsStateWithLifecycle()
    val catSudokuSize by viewModel.catSudokuSize.collectAsStateWithLifecycle()
    val catSudokuGrid by viewModel.catSudokuGrid.collectAsStateWithLifecycle()
    val catSudokuElapsedTimeMillis by viewModel.catSudokuElapsedTimeMillis.collectAsStateWithLifecycle()
    val catSudokuWrongCount by viewModel.catSudokuWrongCount.collectAsStateWithLifecycle()

    val leaderboardList by viewModel.leaderboardList.collectAsStateWithLifecycle()
    val showLeaderboardDialog by viewModel.showLeaderboardDialog.collectAsStateWithLifecycle()
    val showNameDialog by viewModel.showNameDialog.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val isSfxEnabled by viewModel.isSfxEnabled.collectAsStateWithLifecycle()
    val isBgmEnabled by viewModel.isBgmEnabled.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Modifier.padding(innerPadding)

        when (currentScreen) {
            ScreenState.HOME -> {
                HomeScreen(
                    playerName = playerName,
                    language = language,
                    appTheme = appTheme,
                    onSettingsClick = { viewModel.openSettingsDialog() },
                    onChangeNameClick = { viewModel.openNameDialog() },
                    onCategoryClick = { viewModel.selectCategory(it) }
                )
            }

            ScreenState.CATEGORY_DETAIL -> {
                CategoryScreen(
                    category = selectedCategory,
                    selectedGameType = selectedGameType,
                    language = language,
                    onBackClick = { viewModel.navigateTo(ScreenState.HOME) },
                    onGameTypeSelect = { gameType -> viewModel.selectGameType(gameType) },
                    onDifficultySelect = { diff ->
                        viewModel.selectDifficultyAndStart(diff)
                    },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.FOCUS_GAME -> {
                FocusGameScreen(
                    difficulty = selectedDifficulty,
                    gameStatus = gameStatus,
                    gridNumbers = gridNumbers,
                    currentTarget = currentTarget,
                    clearedIndices = clearedIndices,
                    elapsedTimeMillis = elapsedTimeMillis,
                    wrongTapIndex = wrongTapIndex,
                    lastCompletedTimeMillis = lastCompletedTimeMillis,
                    language = language,
                    onBackClick = { viewModel.navigateTo(ScreenState.CATEGORY_DETAIL) },
                    onStartClick = { viewModel.startGame() },
                    onResetClick = { viewModel.resetGame() },
                    onCellClick = { index -> viewModel.onCellTapped(index) },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.FOCUS_TRAIN_GAME -> {
                FocusTrainScreen(
                    difficulty = selectedDifficulty,
                    gameStatus = focusTrainStatus,
                    config = focusTrainConfig,
                    numbers = focusTrainNumbers,
                    currentTarget = focusTrainTarget,
                    clearedIndices = focusTrainClearedIndices,
                    elapsedTimeMillis = focusTrainElapsedTimeMillis,
                    wrongTapIndex = focusTrainWrongTapIndex,
                    wrongCount = focusTrainWrongCount,
                    lastCompletedTimeMillis = focusTrainLastCompletedTimeMillis,
                    language = language,
                    onBackClick = { viewModel.navigateTo(ScreenState.CATEGORY_DETAIL) },
                    onStartClick = { viewModel.startFocusTrainGame() },
                    onResetClick = { viewModel.resetFocusTrainGame() },
                    onCellClick = { index -> viewModel.onFocusTrainCellTapped(index) },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.SPEED_MATCH_GAME -> {
                SpeedMatchScreen(
                    difficulty = selectedDifficulty,
                    gameStatus = speedMatchStatus,
                    gridNumbers = speedMatchGrid,
                    selectedCellIndex = selectedMatchCellIndex,
                    correctRounds = correctRounds,
                    wrongTaps = wrongTaps,
                    remainingTimeMs = speedMatchRemainingMs,
                    isWrongFlash = isWrongFlash,
                    language = language,
                    onBackClick = { viewModel.navigateTo(ScreenState.CATEGORY_DETAIL) },
                    onStartClick = { viewModel.startSpeedMatchGame() },
                    onResetClick = { viewModel.resetSpeedMatchGame() },
                    onCellClick = { index -> viewModel.onSpeedMatchCellTapped(index) },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.SUDOKU_GAME -> {
                SudokuScreen(
                    difficulty = selectedDifficulty,
                    language = language,
                    status = sudokuStatus,
                    elapsedTimeMillis = sudokuElapsedTimeMillis,
                    wrongCount = sudokuWrongCount,
                    initialBoard = sudokuInitialBoard,
                    solutionBoard = sudokuSolutionBoard,
                    playerBoard = sudokuPlayerBoard,
                    notesBoard = sudokuNotesBoard,
                    config = sudokuConfig ?: SudokuGameConfig.getConfig(selectedDifficulty),
                    selectedCellIndex = selectedSudokuCellIndex,
                    isPencilMode = isPencilMode,
                    onBackClick = { viewModel.navigateTo(ScreenState.CATEGORY_DETAIL) },
                    onCellClick = { index -> viewModel.selectSudokuCell(index) },
                    onSymbolInput = { symbol -> viewModel.inputSudokuSymbol(symbol) },
                    onEraseClick = { viewModel.eraseSudokuCell() },
                    onUndoClick = { viewModel.undoSudokuMove() },
                    onTogglePencilClick = { viewModel.togglePencilMode() },
                    onResetClick = { viewModel.resetSudokuGame() },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.CAT_SUDOKU_GAME -> {
                CatSudokuScreen(
                    difficulty = selectedDifficulty,
                    language = language,
                    status = catSudokuStatus,
                    elapsedTimeMillis = catSudokuElapsedTimeMillis,
                    wrongCount = catSudokuWrongCount,
                    boardSize = catSudokuSize,
                    gridCells = catSudokuGrid,
                    onBackClick = { viewModel.navigateTo(ScreenState.CATEGORY_DETAIL) },
                    onCellClick = { index -> viewModel.onCatSudokuCellTapped(index) },
                    onUndoClick = { viewModel.undoCatSudokuMove() },
                    onResetBoardClick = { viewModel.clearCatSudokuBoard() },
                    onNewGameClick = { viewModel.resetCatSudokuGame() },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }
        }

        // Global Dialogs
        if (showNameDialog) {
            ChangeNameDialog(
                currentName = playerName,
                language = language,
                onDismiss = { viewModel.closeNameDialog() },
                onConfirm = { newName ->
                    viewModel.setPlayerName(newName)
                    viewModel.closeNameDialog()
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                currentTheme = appTheme,
                currentLanguage = language,
                isSfxEnabled = isSfxEnabled,
                isBgmEnabled = isBgmEnabled,
                onSelectTheme = { selectedStyle ->
                    viewModel.setThemeStyle(selectedStyle)
                },
                onSelectLanguage = { selectedLang ->
                    viewModel.setLanguage(selectedLang)
                },
                onToggleSfx = { enabled ->
                    viewModel.toggleSfx(enabled)
                },
                onToggleBgm = { enabled ->
                    viewModel.toggleBgm(enabled)
                },
                onDismiss = { viewModel.closeSettingsDialog() }
            )
        }

        if (showLeaderboardDialog) {
            LeaderboardDialog(
                selectedDifficulty = selectedDifficulty,
                selectedGameType = selectedGameType,
                scores = leaderboardList,
                language = language,
                onDifficultySelected = { diff ->
                    viewModel.setLeaderboardDifficultyFilter(diff)
                },
                onClearScores = {
                    viewModel.clearScoresForCurrentLevel()
                },
                onDismiss = { viewModel.closeLeaderboardDialog() }
            )
        }
    }
}
