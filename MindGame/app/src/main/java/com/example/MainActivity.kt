package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
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
import com.example.ui.screens.AvatarWhackScreen
import com.example.ui.screens.StroopScreen
import com.example.ui.screens.CasualCategoryScreen
import com.example.ui.screens.BlockPuzzleScreen
import com.example.ui.screens.FruitMasterScreen
import com.example.ui.screens.GlassPuzzleCubeScreen
import com.example.ui.screens.turtlesoup.TurtleSoupScreen
import com.example.ui.screens.turtlesoup.TurtleSoupPlayState
import com.example.game.sudoku.SudokuConfig
import com.example.game.sudoku.SudokuGameConfig
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.ScreenState

import android.app.Activity
import com.example.ad.AdManager
import com.example.ui.viewmodel.GameStatus

import com.example.audio.SoundManager

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SoundManager.initialize(applicationContext)
        AdManager.initialize(this)

        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(themeStyle = appTheme) {
                MainApp(viewModel = viewModel)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SoundManager.onAppFocusChanged(hasFocus)
    }

    override fun onResume() {
        super.onResume()
        SoundManager.resumeBgm()
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseBgm()
    }

    override fun onStop() {
        super.onStop()
        SoundManager.pauseBgm()
    }

    override fun onStart() {
        super.onStart()
        SoundManager.resumeBgm()
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
    val leaderboardDifficulty by viewModel.leaderboardDifficulty.collectAsStateWithLifecycle()

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

    // Turtle Soup state
    val turtleSoupPuzzles by viewModel.turtleSoupPuzzles.collectAsStateWithLifecycle()
    val turtleSoupCurrentPuzzle by viewModel.turtleSoupCurrentPuzzle.collectAsStateWithLifecycle()
    val turtleSoupPlayState by viewModel.turtleSoupPlayState.collectAsStateWithLifecycle()
    val turtleSoupQueryLogs by viewModel.turtleSoupQueryLogs.collectAsStateWithLifecycle()
    val turtleSoupSelectedDimensions by viewModel.turtleSoupSelectedDimensions.collectAsStateWithLifecycle()
    val turtleSoupDiscoveredCoreCount by viewModel.turtleSoupDiscoveredCoreCount.collectAsStateWithLifecycle()
    val turtleSoupShowAdDialog by viewModel.turtleSoupShowAdDialog.collectAsStateWithLifecycle()
    val turtleSoupUnlockedQuestions by viewModel.turtleSoupUnlockedQuestions.collectAsStateWithLifecycle()
    val turtleSoupSelectedSlots by viewModel.turtleSoupSelectedSlots.collectAsStateWithLifecycle()
    val turtleSoupRemainingChances by viewModel.turtleSoupRemainingChances.collectAsStateWithLifecycle()
    val turtleSoupUsedChances by viewModel.turtleSoupUsedChances.collectAsStateWithLifecycle()
    val turtleSoupElapsedSeconds by viewModel.turtleSoupElapsedSeconds.collectAsStateWithLifecycle()
    val turtleSoupIsDeductionError by viewModel.turtleSoupIsDeductionError.collectAsStateWithLifecycle()
    val turtleSoupFinalScore by viewModel.turtleSoupFinalScore.collectAsStateWithLifecycle()
    val turtleSoupFinalStars by viewModel.turtleSoupFinalStars.collectAsStateWithLifecycle()
    val turtleSoupUsedAdReward by viewModel.turtleSoupUsedAdReward.collectAsStateWithLifecycle()
    val turtleSoupSaveData by viewModel.turtleSoupSaveData.collectAsStateWithLifecycle()

    // Avatar Whack state
    val whackStatus by viewModel.whackStatus.collectAsStateWithLifecycle()
    val whackConfig by viewModel.whackConfig.collectAsStateWithLifecycle()
    val whackHoles by viewModel.whackHoles.collectAsStateWithLifecycle()
    val whackScore by viewModel.whackScore.collectAsStateWithLifecycle()
    val whackCombo by viewModel.whackCombo.collectAsStateWithLifecycle()
    val whackMaxCombo by viewModel.whackMaxCombo.collectAsStateWithLifecycle()
    val whackHits by viewModel.whackHits.collectAsStateWithLifecycle()
    val whackMisses by viewModel.whackMisses.collectAsStateWithLifecycle()
    val whackBombHits by viewModel.whackBombHits.collectAsStateWithLifecycle()
    val whackRemainingMs by viewModel.whackRemainingMs.collectAsStateWithLifecycle()
    val whackLastCompletedScore by viewModel.whackLastCompletedScore.collectAsStateWithLifecycle()

    // Stroop Effect state
    val stroopStatus by viewModel.stroopStatus.collectAsStateWithLifecycle()
    val stroopCurrentQuestion by viewModel.stroopCurrentQuestion.collectAsStateWithLifecycle()
    val stroopScore by viewModel.stroopScore.collectAsStateWithLifecycle()
    val stroopCombo by viewModel.stroopCombo.collectAsStateWithLifecycle()
    val stroopMaxCombo by viewModel.stroopMaxCombo.collectAsStateWithLifecycle()
    val stroopCorrectCount by viewModel.stroopCorrectCount.collectAsStateWithLifecycle()
    val stroopWrongCount by viewModel.stroopWrongCount.collectAsStateWithLifecycle()
    val stroopRemainingGameTimeMs by viewModel.stroopRemainingGameTimeMs.collectAsStateWithLifecycle()
    val stroopQuestionTimeProgress by viewModel.stroopQuestionTimeProgress.collectAsStateWithLifecycle()
    val stroopIsWrongFlash by viewModel.stroopIsWrongFlash.collectAsStateWithLifecycle()
    val stroopLastCompletedScore by viewModel.stroopLastCompletedScore.collectAsStateWithLifecycle()

    val leaderboardList by viewModel.leaderboardList.collectAsStateWithLifecycle()
    val globalLeaderboardList by viewModel.globalLeaderboardList.collectAsStateWithLifecycle()
    val myGlobalRankEntry by viewModel.myGlobalRankEntry.collectAsStateWithLifecycle()
    val isFetchingGlobalLeaderboard by viewModel.isFetchingGlobalLeaderboard.collectAsStateWithLifecycle()
    val isUploadingGlobalScore by viewModel.isUploadingGlobalScore.collectAsStateWithLifecycle()
    val globalUploadMessage by viewModel.globalUploadMessage.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val showLeaderboardDialog by viewModel.showLeaderboardDialog.collectAsStateWithLifecycle()
    val showNameDialog by viewModel.showNameDialog.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val isFullScreenEnabled by viewModel.isFullScreenEnabled.collectAsStateWithLifecycle()
    val isSfxEnabled by viewModel.isSfxEnabled.collectAsStateWithLifecycle()
    val isBgmEnabled by viewModel.isBgmEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(globalUploadMessage) {
        globalUploadMessage?.let { msgKey ->
            val text = com.example.data.model.Localization.getString(msgKey, language)
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.clearGlobalUploadMessage()
        }
    }
    LaunchedEffect(isFullScreenEnabled) {
        val activity = context as? ComponentActivity
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullScreenEnabled) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 監聽各遊戲通關結算事件，每滿 3 局觸發一次插頁式廣告
    LaunchedEffect(gameStatus) {
        if (gameStatus == GameStatus.COMPLETED) {
            AdManager.recordGameFinished(context as? Activity)
        }
    }
    LaunchedEffect(focusTrainStatus) {
        if (focusTrainStatus == GameStatus.COMPLETED) {
            AdManager.recordGameFinished(context as? Activity)
        }
    }
    LaunchedEffect(speedMatchStatus) {
        if (speedMatchStatus == GameStatus.COMPLETED) {
            AdManager.recordGameFinished(context as? Activity)
        }
    }
    LaunchedEffect(sudokuStatus) {
        if (sudokuStatus == GameStatus.COMPLETED) {
            AdManager.recordGameFinished(context as? Activity)
        }
    }
    LaunchedEffect(catSudokuStatus) {
        if (catSudokuStatus == GameStatus.COMPLETED) {
            AdManager.recordGameFinished(context as? Activity)
        }
    }
    LaunchedEffect(turtleSoupPlayState) {
        if (turtleSoupPlayState == TurtleSoupPlayState.SUCCESS) {
            AdManager.recordGameFinished(context as? Activity)
        }
    }
    LaunchedEffect(whackStatus) {
        if (whackStatus == GameStatus.COMPLETED) {
            AdManager.recordGameFinished(context as? Activity)
        }
    }
    LaunchedEffect(stroopStatus) {
        if (stroopStatus == GameStatus.COMPLETED) {
            AdManager.recordGameFinished(context as? Activity)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                    onBackClick = {
                        if (gameStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.navigateTo(ScreenState.CATEGORY_DETAIL)
                    },
                    onStartClick = { viewModel.startGame() },
                    onResetClick = {
                        if (gameStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.resetGame()
                    },
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
                    onBackClick = {
                        if (focusTrainStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.navigateTo(ScreenState.CATEGORY_DETAIL)
                    },
                    onStartClick = { viewModel.startFocusTrainGame() },
                    onResetClick = {
                        if (focusTrainStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.resetFocusTrainGame()
                    },
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
                    onBackClick = {
                        if (speedMatchStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.navigateTo(ScreenState.CATEGORY_DETAIL)
                    },
                    onStartClick = { viewModel.startSpeedMatchGame() },
                    onResetClick = {
                        if (speedMatchStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.resetSpeedMatchGame()
                    },
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
                    onBackClick = {
                        if (sudokuStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.navigateTo(ScreenState.CATEGORY_DETAIL)
                    },
                    onCellClick = { index -> viewModel.selectSudokuCell(index) },
                    onSymbolInput = { symbol -> viewModel.inputSudokuSymbol(symbol) },
                    onEraseClick = { viewModel.eraseSudokuCell() },
                    onUndoClick = { viewModel.undoSudokuMove() },
                    onTogglePencilClick = { viewModel.togglePencilMode() },
                    onResetClick = {
                        if (sudokuStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.resetSudokuGame()
                    },
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
                    onBackClick = {
                        if (catSudokuStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.navigateTo(ScreenState.CATEGORY_DETAIL)
                    },
                    onCellClick = { index -> viewModel.onCatSudokuCellTapped(index) },
                    onUndoClick = { viewModel.undoCatSudokuMove() },
                    onResetBoardClick = { viewModel.clearCatSudokuBoard() },
                    onNewGameClick = {
                        if (catSudokuStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.resetCatSudokuGame()
                    },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.TURTLE_SOUP_GAME -> {
                TurtleSoupScreen(
                    difficulty = selectedDifficulty,
                    puzzles = turtleSoupPuzzles,
                    currentPuzzle = turtleSoupCurrentPuzzle,
                    playState = turtleSoupPlayState,
                    queryLogs = turtleSoupQueryLogs,
                    selectedDimensions = turtleSoupSelectedDimensions,
                    discoveredCoreCount = turtleSoupDiscoveredCoreCount,
                    showAdDialog = turtleSoupShowAdDialog,
                    unlockedQuestionIndices = turtleSoupUnlockedQuestions,
                    selectedSlotIndices = turtleSoupSelectedSlots,
                    remainingChances = turtleSoupRemainingChances,
                    usedChances = turtleSoupUsedChances,
                    elapsedSeconds = turtleSoupElapsedSeconds,
                    isDeductionErrorFlash = turtleSoupIsDeductionError,
                    finalScore = turtleSoupFinalScore,
                    finalStars = turtleSoupFinalStars,
                    usedAdReward = turtleSoupUsedAdReward,
                    saveData = turtleSoupSaveData,
                    language = language,
                    onBackClick = {
                        if (turtleSoupPlayState == TurtleSoupPlayState.INVESTIGATING || turtleSoupPlayState == TurtleSoupPlayState.SOLVING) {
                            viewModel.giveUpTurtleSoupGame(context as? Activity)
                        }
                        viewModel.navigateTo(ScreenState.CATEGORY_DETAIL)
                    },
                    onSelectPuzzle = { puzzle -> viewModel.selectTurtleSoupPuzzle(puzzle) },
                    onSelectDimensionOption = { dimId, opt -> viewModel.selectTurtleSoupDimensionOption(dimId, opt) },
                    onSubmitInquiry = { viewModel.submitTurtleSoupInquiry(context as? Activity) },
                    onWatchAdForInquiry = { viewModel.watchAdForTurtleSoupInquiry(context as? Activity) },
                    onCloseAdDialog = { viewModel.closeTurtleSoupAdDialog() },
                    onUnlockQuestion = { qIdx -> viewModel.unlockTurtleSoupQuestion(qIdx) },
                    onSelectSlotOption = { sIdx, oIdx -> viewModel.selectTurtleSoupSlotOption(sIdx, oIdx) },
                    onStartSolving = { viewModel.startTurtleSoupSolving() },
                    onBackToInvestigate = { viewModel.backToTurtleSoupInvestigate() },
                    onSubmitDeduction = { viewModel.submitTurtleSoupDeduction() },
                    onWatchAdForChances = { viewModel.watchAdForTurtleSoupChances(context as? Activity) },
                    onGiveUpGame = { viewModel.giveUpTurtleSoupGame(context as? Activity) },
                    onRestartPuzzle = { viewModel.restartTurtleSoupPuzzle(context as? Activity) },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.AVATAR_WHACK_GAME -> {
                AvatarWhackScreen(
                    difficulty = selectedDifficulty,
                    gameStatus = whackStatus,
                    config = whackConfig,
                    holes = whackHoles,
                    score = whackScore,
                    combo = whackCombo,
                    maxCombo = whackMaxCombo,
                    hits = whackHits,
                    misses = whackMisses,
                    bombHits = whackBombHits,
                    remainingTimeMs = whackRemainingMs,
                    lastCompletedScore = whackLastCompletedScore,
                    language = language,
                    onBackClick = {
                        if (whackStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.navigateTo(ScreenState.CATEGORY_DETAIL)
                    },
                    onStartClick = { viewModel.startAvatarWhackGame() },
                    onResetClick = {
                        if (whackStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.resetAvatarWhackGame()
                    },
                    onHoleClick = { index -> viewModel.onWhackHoleTapped(index) },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.STROOP_EFFECT_GAME -> {
                StroopScreen(
                    difficulty = selectedDifficulty,
                    gameStatus = stroopStatus,
                    question = stroopCurrentQuestion,
                    score = stroopScore,
                    combo = stroopCombo,
                    maxCombo = stroopMaxCombo,
                    correctCount = stroopCorrectCount,
                    wrongCount = stroopWrongCount,
                    remainingGameTimeMs = stroopRemainingGameTimeMs,
                    questionTimeProgress = stroopQuestionTimeProgress,
                    isWrongFlash = stroopIsWrongFlash,
                    language = language,
                    onBackClick = {
                        if (stroopStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.navigateTo(ScreenState.CATEGORY_DETAIL)
                    },
                    onStartClick = { viewModel.startStroopGame() },
                    onResetClick = {
                        if (stroopStatus == GameStatus.PLAYING) {
                            AdManager.recordGameInterrupted(context as? Activity)
                        }
                        viewModel.resetStroopGame()
                    },
                    onOptionSelected = { option -> viewModel.onStroopOptionSelected(option) },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.CASUAL_CATEGORY -> {
                CasualCategoryScreen(
                    language = language,
                    appTheme = appTheme,
                    onBackClick = { viewModel.navigateTo(ScreenState.HOME) },
                    onPlayBlockPuzzle = { viewModel.startBlockPuzzleGame() },
                    onPlayFruitMaster = { viewModel.startFruitMasterGame() },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog() }
                )
            }

            ScreenState.BLOCK_PUZZLE_GAME -> {
                val blockPuzzleBestScore by viewModel.blockPuzzleBestScore.collectAsStateWithLifecycle()
                BlockPuzzleScreen(
                    language = language,
                    appTheme = appTheme,
                    bestScore = blockPuzzleBestScore,
                    onBackClick = { viewModel.navigateTo(ScreenState.CASUAL_CATEGORY) },
                    onSaveScore = { score -> viewModel.saveBlockPuzzleScore(score) },
                    onGameOver = { AdManager.recordGameFinished(context as? Activity) },
                    onGameInterrupted = { AdManager.recordGameInterrupted(context as? Activity) }
                )
            }

            ScreenState.FRUIT_MASTER_GAME -> {
                val fruitHeartbeatBestScore by viewModel.fruitHeartbeatBestScore.collectAsStateWithLifecycle()
                val fruitBladeBombBestScore by viewModel.fruitBladeBombBestScore.collectAsStateWithLifecycle()
                val fruitWorkshopBestScore by viewModel.fruitWorkshopBestScore.collectAsStateWithLifecycle()
                FruitMasterScreen(
                    language = language,
                    appTheme = appTheme,
                    heartbeatBestScore = fruitHeartbeatBestScore,
                    bladeBombBestScore = fruitBladeBombBestScore,
                    workshopBestScore = fruitWorkshopBestScore,
                    onBackClick = { viewModel.navigateTo(ScreenState.CASUAL_CATEGORY) },
                    onSaveScore = { mode, score -> viewModel.saveFruitMasterScore(mode, score) },
                    onLeaderboardClick = { diff -> viewModel.openLeaderboardDialog(diff, GameType.FRUIT_MASTER) },
                    onGameOver = { AdManager.recordGameFinished(context as? Activity) },
                    onGameInterrupted = { AdManager.recordGameInterrupted(context as? Activity) }
                )
            }

            ScreenState.GLASS_PUZZLE_CUBE_GAME -> {
                GlassPuzzleCubeScreen(
                    difficulty = selectedDifficulty,
                    language = language,
                    appTheme = appTheme,
                    onBackClick = { viewModel.navigateTo(ScreenState.CATEGORY_DETAIL) },
                    onSaveScore = { timeMillis -> viewModel.saveGlassPuzzleCubeScore(selectedDifficulty, timeMillis) },
                    onLeaderboardClick = { viewModel.openLeaderboardDialog(selectedDifficulty, GameType.GLASS_PUZZLE_CUBE) },
                    onGameCompleted = { AdManager.recordGameFinished(context as? Activity) },
                    onGameInterrupted = { AdManager.recordGameInterrupted(context as? Activity) }
                )
            }
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
                currentCountry = selectedCountry,
                isFullScreenEnabled = isFullScreenEnabled,
                isSfxEnabled = isSfxEnabled,
                isBgmEnabled = isBgmEnabled,
                onSelectTheme = { selectedStyle ->
                    viewModel.setThemeStyle(selectedStyle)
                },
                onSelectLanguage = { selectedLang ->
                    viewModel.setLanguage(selectedLang)
                },
                onSelectCountry = { country ->
                    viewModel.setSelectedCountry(country)
                },
                onToggleFullScreen = { enabled ->
                    viewModel.toggleFullScreen(enabled)
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
                selectedDifficulty = leaderboardDifficulty,
                selectedGameType = selectedGameType,
                scores = leaderboardList,
                globalScores = globalLeaderboardList,
                myGlobalRankEntry = myGlobalRankEntry,
                isFetchingGlobal = isFetchingGlobalLeaderboard,
                isUploadingGlobal = isUploadingGlobalScore,
                language = language,
                onDifficultySelected = { diff ->
                    viewModel.setLeaderboardDifficultyFilter(diff)
                },
                onClearScores = {
                    viewModel.clearScoresForCurrentLevel()
                },
                onUploadToGlobal = {
                    viewModel.uploadBestScoreToGlobal()
                },
                onRefreshGlobal = {
                    viewModel.loadGlobalLeaderboard()
                },
                onDismiss = { viewModel.closeLeaderboardDialog() }
            )
        }
    }
}
