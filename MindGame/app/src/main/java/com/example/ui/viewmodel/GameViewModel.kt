package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ad.AdManager
import com.example.audio.SoundManager
import com.example.data.db.AppDatabase
import com.example.data.db.ScoreRecord
import com.example.data.db.ScoreRepository
import com.example.data.model.AppLanguage
import com.example.data.model.GameCategory
import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.data.model.QuestionAnswer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.game.catsudoku.CatCell
import com.example.game.catsudoku.CatCellState
import com.example.game.catsudoku.CatSudokuGenerator
import com.example.game.catsudoku.CatSudokuPuzzle
import com.example.game.sudoku.SudokuConfig
import com.example.game.sudoku.SudokuGenerator
import com.example.game.sudoku.SudokuMove
import com.example.data.model.TurtleSoupPuzzle
import com.example.data.model.TurtleSoupRecord
import com.example.data.model.TurtleSoupSaveData
import com.example.game.turtlesoup.TurtleSoupRepository
import com.example.ui.screens.turtlesoup.TurtleSoupPlayState

import com.example.data.model.WheelDifficultyConfig

import com.example.game.whack.AvatarCatalog
import com.example.game.whack.AvatarExpression
import com.example.game.whack.AvatarType
import com.example.game.whack.CharacterAvatar
import com.example.game.whack.WhackDifficultyConfig
import com.example.game.whack.WhackHoleState
import com.example.game.stroop.StroopGenerator
import com.example.game.stroop.StroopOption
import com.example.game.stroop.StroopQuestion

enum class ScreenState {
    HOME,
    CATEGORY_DETAIL,
    FOCUS_GAME,
    FOCUS_TRAIN_GAME,
    SPEED_MATCH_GAME,
    SUDOKU_GAME,
    CAT_SUDOKU_GAME,
    TURTLE_SOUP_GAME,
    AVATAR_WHACK_GAME,
    STROOP_EFFECT_GAME
}

enum class GameStatus {
    IDLE,
    PLAYING,
    COMPLETED
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("mindgame_prefs", Context.MODE_PRIVATE)

    private val repository: ScoreRepository = ScoreRepository(
        AppDatabase.getDatabase(application).scoreDao()
    )

    // Global Settings
    private val _language = MutableStateFlow(
        prefs.getString("pref_language", null)?.let { code ->
            AppLanguage.values().firstOrNull { it.code == code }
        } ?: AppLanguage.TRADITIONAL_CHINESE
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _appTheme = MutableStateFlow(
        prefs.getString("pref_theme", null)?.let { key ->
            com.example.ui.theme.AppThemeStyle.values().firstOrNull { it.key == key }
        } ?: com.example.ui.theme.AppThemeStyle.SNOW_WHITE
    )
    val appTheme: StateFlow<com.example.ui.theme.AppThemeStyle> = _appTheme.asStateFlow()

    private val _playerName = MutableStateFlow(prefs.getString("pref_player_name", "玩家 1") ?: "玩家 1")
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _isFullScreenEnabled = MutableStateFlow(prefs.getBoolean("pref_fullscreen", true))
    val isFullScreenEnabled: StateFlow<Boolean> = _isFullScreenEnabled.asStateFlow()

    // Navigation
    private val _currentScreen = MutableStateFlow(ScreenState.HOME)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _selectedCategory = MutableStateFlow(GameCategory.TEST)
    val selectedCategory: StateFlow<GameCategory> = _selectedCategory.asStateFlow()

    private val _selectedGameType = MutableStateFlow(GameType.FOCUS_TEST)
    val selectedGameType: StateFlow<GameType> = _selectedGameType.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(GameDifficulty.BEGINNER)
    val selectedDifficulty: StateFlow<GameDifficulty> = _selectedDifficulty.asStateFlow()

    // Focus Game State (Schulte Grid)
    private val _gameStatus = MutableStateFlow(GameStatus.IDLE)
    val gameStatus: StateFlow<GameStatus> = _gameStatus.asStateFlow()

    private val _gridNumbers = MutableStateFlow<List<Int>>(emptyList())
    val gridNumbers: StateFlow<List<Int>> = _gridNumbers.asStateFlow()

    private val _currentTarget = MutableStateFlow(1)
    val currentTarget: StateFlow<Int> = _currentTarget.asStateFlow()

    private val _clearedIndices = MutableStateFlow<Set<Int>>(emptySet())
    val clearedIndices: StateFlow<Set<Int>> = _clearedIndices.asStateFlow()

    private val _elapsedTimeMillis = MutableStateFlow(0L)
    val elapsedTimeMillis: StateFlow<Long> = _elapsedTimeMillis.asStateFlow()

    private val _wrongTapIndex = MutableStateFlow<Int?>(null)
    val wrongTapIndex: StateFlow<Int?> = _wrongTapIndex.asStateFlow()

    private val _lastCompletedTimeMillis = MutableStateFlow<Long?>(null)
    val lastCompletedTimeMillis: StateFlow<Long?> = _lastCompletedTimeMillis.asStateFlow()

    // Focus Train (Dynamic Schulte Wheel) State
    private val _focusTrainStatus = MutableStateFlow(GameStatus.IDLE)
    val focusTrainStatus: StateFlow<GameStatus> = _focusTrainStatus.asStateFlow()

    private val _focusTrainConfig = MutableStateFlow(WheelDifficultyConfig.getConfig(GameDifficulty.BEGINNER))
    val focusTrainConfig: StateFlow<WheelDifficultyConfig> = _focusTrainConfig.asStateFlow()

    private val _focusTrainNumbers = MutableStateFlow<List<Int>>(emptyList())
    val focusTrainNumbers: StateFlow<List<Int>> = _focusTrainNumbers.asStateFlow()

    private val _focusTrainTarget = MutableStateFlow(1)
    val focusTrainTarget: StateFlow<Int> = _focusTrainTarget.asStateFlow()

    private val _focusTrainClearedIndices = MutableStateFlow<Set<Int>>(emptySet())
    val focusTrainClearedIndices: StateFlow<Set<Int>> = _focusTrainClearedIndices.asStateFlow()

    private val _focusTrainElapsedTimeMillis = MutableStateFlow(0L)
    val focusTrainElapsedTimeMillis: StateFlow<Long> = _focusTrainElapsedTimeMillis.asStateFlow()

    private val _focusTrainWrongTapIndex = MutableStateFlow<Int?>(null)
    val focusTrainWrongTapIndex: StateFlow<Int?> = _focusTrainWrongTapIndex.asStateFlow()

    private val _focusTrainWrongCount = MutableStateFlow(0)
    val focusTrainWrongCount: StateFlow<Int> = _focusTrainWrongCount.asStateFlow()

    private val _focusTrainLastCompletedTimeMillis = MutableStateFlow<Long?>(null)
    val focusTrainLastCompletedTimeMillis: StateFlow<Long?> = _focusTrainLastCompletedTimeMillis.asStateFlow()

    // Speed Match Game State
    private val _speedMatchStatus = MutableStateFlow(GameStatus.IDLE)
    val speedMatchStatus: StateFlow<GameStatus> = _speedMatchStatus.asStateFlow()

    private val _speedMatchGrid = MutableStateFlow<List<Int>>(emptyList())
    val speedMatchGrid: StateFlow<List<Int>> = _speedMatchGrid.asStateFlow()

    private val _selectedMatchCellIndex = MutableStateFlow<Int?>(null)
    val selectedMatchCellIndex: StateFlow<Int?> = _selectedMatchCellIndex.asStateFlow()

    private val _correctRounds = MutableStateFlow(0)
    val correctRounds: StateFlow<Int> = _correctRounds.asStateFlow()

    private val _wrongTaps = MutableStateFlow(0)
    val wrongTaps: StateFlow<Int> = _wrongTaps.asStateFlow()

    private val _speedMatchRemainingMs = MutableStateFlow(60_000L)
    val speedMatchRemainingMs: StateFlow<Long> = _speedMatchRemainingMs.asStateFlow()

    private val _isWrongFlash = MutableStateFlow(false)
    val isWrongFlash: StateFlow<Boolean> = _isWrongFlash.asStateFlow()

    // Sudoku Game State
    private val _sudokuStatus = MutableStateFlow(GameStatus.IDLE)
    val sudokuStatus: StateFlow<GameStatus> = _sudokuStatus.asStateFlow()

    private val _sudokuInitialBoard = MutableStateFlow<List<String>>(emptyList())
    val sudokuInitialBoard: StateFlow<List<String>> = _sudokuInitialBoard.asStateFlow()

    private val _sudokuSolutionBoard = MutableStateFlow<List<String>>(emptyList())
    val sudokuSolutionBoard: StateFlow<List<String>> = _sudokuSolutionBoard.asStateFlow()

    private val _sudokuPlayerBoard = MutableStateFlow<List<String>>(emptyList())
    val sudokuPlayerBoard: StateFlow<List<String>> = _sudokuPlayerBoard.asStateFlow()

    private val _sudokuNotesBoard = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val sudokuNotesBoard: StateFlow<Map<Int, Set<String>>> = _sudokuNotesBoard.asStateFlow()

    private val _sudokuConfig = MutableStateFlow<SudokuConfig?>(null)
    val sudokuConfig: StateFlow<SudokuConfig?> = _sudokuConfig.asStateFlow()

    private val _selectedSudokuCellIndex = MutableStateFlow<Int?>(null)
    val selectedSudokuCellIndex: StateFlow<Int?> = _selectedSudokuCellIndex.asStateFlow()

    private val _isPencilMode = MutableStateFlow(false)
    val isPencilMode: StateFlow<Boolean> = _isPencilMode.asStateFlow()

    private val _sudokuWrongCount = MutableStateFlow(0)
    val sudokuWrongCount: StateFlow<Int> = _sudokuWrongCount.asStateFlow()

    private val _sudokuElapsedTimeMillis = MutableStateFlow(0L)
    val sudokuElapsedTimeMillis: StateFlow<Long> = _sudokuElapsedTimeMillis.asStateFlow()

    private val _sudokuMoveHistory = MutableStateFlow<List<SudokuMove>>(emptyList())

    // Cat Sudoku Game State
    private val _catSudokuStatus = MutableStateFlow(GameStatus.IDLE)
    val catSudokuStatus: StateFlow<GameStatus> = _catSudokuStatus.asStateFlow()

    private val _catSudokuSize = MutableStateFlow(4)
    val catSudokuSize: StateFlow<Int> = _catSudokuSize.asStateFlow()

    private val _catSudokuGrid = MutableStateFlow<List<CatCell>>(emptyList())
    val catSudokuGrid: StateFlow<List<CatCell>> = _catSudokuGrid.asStateFlow()

    private val _catSudokuElapsedTimeMillis = MutableStateFlow(0L)
    val catSudokuElapsedTimeMillis: StateFlow<Long> = _catSudokuElapsedTimeMillis.asStateFlow()

    private val _catSudokuWrongCount = MutableStateFlow(0)
    val catSudokuWrongCount: StateFlow<Int> = _catSudokuWrongCount.asStateFlow()

    private val _catSudokuHistory = MutableStateFlow<List<List<CatCell>>>(emptyList())

    // Turtle Soup Game State
    private val _turtleSoupPuzzles = MutableStateFlow<List<TurtleSoupPuzzle>>(emptyList())
    val turtleSoupPuzzles: StateFlow<List<TurtleSoupPuzzle>> = _turtleSoupPuzzles.asStateFlow()

    private val _turtleSoupCurrentPuzzle = MutableStateFlow<TurtleSoupPuzzle?>(null)
    val turtleSoupCurrentPuzzle: StateFlow<TurtleSoupPuzzle?> = _turtleSoupCurrentPuzzle.asStateFlow()

    private val _turtleSoupPlayState = MutableStateFlow(TurtleSoupPlayState.PUZZLE_SELECT)
    val turtleSoupPlayState: StateFlow<TurtleSoupPlayState> = _turtleSoupPlayState.asStateFlow()

    private val _turtleSoupUnlockedQuestions = MutableStateFlow<Set<Int>>(emptySet())
    val turtleSoupUnlockedQuestions: StateFlow<Set<Int>> = _turtleSoupUnlockedQuestions.asStateFlow()

    private val _turtleSoupQueryLogs = MutableStateFlow<List<com.example.data.model.InvestigationQueryLog>>(emptyList())
    val turtleSoupQueryLogs: StateFlow<List<com.example.data.model.InvestigationQueryLog>> = _turtleSoupQueryLogs.asStateFlow()

    private val _turtleSoupSelectedDimensions = MutableStateFlow<Map<String, String>>(emptyMap())
    val turtleSoupSelectedDimensions: StateFlow<Map<String, String>> = _turtleSoupSelectedDimensions.asStateFlow()

    private val _turtleSoupDiscoveredCoreCount = MutableStateFlow(0)
    val turtleSoupDiscoveredCoreCount: StateFlow<Int> = _turtleSoupDiscoveredCoreCount.asStateFlow()

    private val _turtleSoupShowAdDialog = MutableStateFlow(false)
    val turtleSoupShowAdDialog: StateFlow<Boolean> = _turtleSoupShowAdDialog.asStateFlow()

    private val _turtleSoupSelectedSlots = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val turtleSoupSelectedSlots: StateFlow<Map<Int, Int>> = _turtleSoupSelectedSlots.asStateFlow()

    private val _turtleSoupRemainingChances = MutableStateFlow(4)
    val turtleSoupRemainingChances: StateFlow<Int> = _turtleSoupRemainingChances.asStateFlow()

    private val _turtleSoupUsedChances = MutableStateFlow(0)
    val turtleSoupUsedChances: StateFlow<Int> = _turtleSoupUsedChances.asStateFlow()

    private val _turtleSoupElapsedSeconds = MutableStateFlow(0L)
    val turtleSoupElapsedSeconds: StateFlow<Long> = _turtleSoupElapsedSeconds.asStateFlow()

    private val _turtleSoupIsDeductionError = MutableStateFlow(false)
    val turtleSoupIsDeductionError: StateFlow<Boolean> = _turtleSoupIsDeductionError.asStateFlow()

    private val _turtleSoupFinalScore = MutableStateFlow(0)
    val turtleSoupFinalScore: StateFlow<Int> = _turtleSoupFinalScore.asStateFlow()

    private val _turtleSoupFinalStars = MutableStateFlow(0)
    val turtleSoupFinalStars: StateFlow<Int> = _turtleSoupFinalStars.asStateFlow()

    private val _turtleSoupUsedAdReward = MutableStateFlow(false)
    val turtleSoupUsedAdReward: StateFlow<Boolean> = _turtleSoupUsedAdReward.asStateFlow()

    private val _turtleSoupSaveData = MutableStateFlow(TurtleSoupSaveData())
    val turtleSoupSaveData: StateFlow<TurtleSoupSaveData> = _turtleSoupSaveData.asStateFlow()

    // Avatar Whack Game State
    private val _whackStatus = MutableStateFlow(GameStatus.IDLE)
    val whackStatus: StateFlow<GameStatus> = _whackStatus.asStateFlow()

    private val _whackConfig = MutableStateFlow(WhackDifficultyConfig.getConfig(GameDifficulty.BEGINNER))
    val whackConfig: StateFlow<WhackDifficultyConfig> = _whackConfig.asStateFlow()

    private val _whackHoles = MutableStateFlow<List<WhackHoleState>>(emptyList())
    val whackHoles: StateFlow<List<WhackHoleState>> = _whackHoles.asStateFlow()

    private val _whackScore = MutableStateFlow(0)
    val whackScore: StateFlow<Int> = _whackScore.asStateFlow()

    private val _whackCombo = MutableStateFlow(0)
    val whackCombo: StateFlow<Int> = _whackCombo.asStateFlow()

    private val _whackMaxCombo = MutableStateFlow(0)
    val whackMaxCombo: StateFlow<Int> = _whackMaxCombo.asStateFlow()

    private val _whackHits = MutableStateFlow(0)
    val whackHits: StateFlow<Int> = _whackHits.asStateFlow()

    private val _whackMisses = MutableStateFlow(0)
    val whackMisses: StateFlow<Int> = _whackMisses.asStateFlow()

    private val _whackBombHits = MutableStateFlow(0)
    val whackBombHits: StateFlow<Int> = _whackBombHits.asStateFlow()

    private val _whackRemainingMs = MutableStateFlow(45_000L)
    val whackRemainingMs: StateFlow<Long> = _whackRemainingMs.asStateFlow()

    private val _whackLastCompletedScore = MutableStateFlow<Int?>(null)
    val whackLastCompletedScore: StateFlow<Int?> = _whackLastCompletedScore.asStateFlow()

    // Stroop Effect Game State
    private val _stroopStatus = MutableStateFlow(GameStatus.IDLE)
    val stroopStatus: StateFlow<GameStatus> = _stroopStatus.asStateFlow()

    private val _stroopRoundInstruction = MutableStateFlow<com.example.game.stroop.StroopInstruction?>(null)
    val stroopRoundInstruction: StateFlow<com.example.game.stroop.StroopInstruction?> = _stroopRoundInstruction.asStateFlow()

    private val _stroopCurrentQuestion = MutableStateFlow<StroopQuestion?>(null)
    val stroopCurrentQuestion: StateFlow<StroopQuestion?> = _stroopCurrentQuestion.asStateFlow()

    private val _stroopScore = MutableStateFlow(0)
    val stroopScore: StateFlow<Int> = _stroopScore.asStateFlow()

    private val _stroopCombo = MutableStateFlow(0)
    val stroopCombo: StateFlow<Int> = _stroopCombo.asStateFlow()

    private val _stroopMaxCombo = MutableStateFlow(0)
    val stroopMaxCombo: StateFlow<Int> = _stroopMaxCombo.asStateFlow()

    private val _stroopCorrectCount = MutableStateFlow(0)
    val stroopCorrectCount: StateFlow<Int> = _stroopCorrectCount.asStateFlow()

    private val _stroopWrongCount = MutableStateFlow(0)
    val stroopWrongCount: StateFlow<Int> = _stroopWrongCount.asStateFlow()

    private val _stroopRemainingGameTimeMs = MutableStateFlow(45_000L)
    val stroopRemainingGameTimeMs: StateFlow<Long> = _stroopRemainingGameTimeMs.asStateFlow()

    private val _stroopQuestionRemainingMs = MutableStateFlow(3_000L)
    val stroopQuestionRemainingMs: StateFlow<Long> = _stroopQuestionRemainingMs.asStateFlow()

    private val _stroopQuestionTimeProgress = MutableStateFlow(1f)
    val stroopQuestionTimeProgress: StateFlow<Float> = _stroopQuestionTimeProgress.asStateFlow()

    private val _stroopIsWrongFlash = MutableStateFlow(false)
    val stroopIsWrongFlash: StateFlow<Boolean> = _stroopIsWrongFlash.asStateFlow()

    private val _stroopLastCompletedScore = MutableStateFlow<Int?>(null)
    val stroopLastCompletedScore: StateFlow<Int?> = _stroopLastCompletedScore.asStateFlow()

    // Dialogs & Settings
    private val _leaderboardList = MutableStateFlow<List<ScoreRecord>>(emptyList())
    val leaderboardList: StateFlow<List<ScoreRecord>> = _leaderboardList.asStateFlow()

    private val _showLeaderboardDialog = MutableStateFlow(false)
    val showLeaderboardDialog: StateFlow<Boolean> = _showLeaderboardDialog.asStateFlow()

    private val _showNameDialog = MutableStateFlow(false)
    val showNameDialog: StateFlow<Boolean> = _showNameDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _isSfxEnabled = MutableStateFlow(prefs.getBoolean("pref_sfx", true))
    val isSfxEnabled: StateFlow<Boolean> = _isSfxEnabled.asStateFlow()

    private val _isBgmEnabled = MutableStateFlow(prefs.getBoolean("pref_bgm", false))
    val isBgmEnabled: StateFlow<Boolean> = _isBgmEnabled.asStateFlow()

    private var timerJob: Job? = null
    private var focusTrainTimerJob: Job? = null
    private var speedMatchTimerJob: Job? = null
    private var sudokuTimerJob: Job? = null
    private var catSudokuTimerJob: Job? = null
    private var turtleSoupTimerJob: Job? = null
    private var whackTimerJob: Job? = null
    private var whackLoopJob: Job? = null
    private var stroopGameTimerJob: Job? = null
    private var stroopQuestionTimerJob: Job? = null
    private var leaderboardJob: Job? = null

    init {
        SoundManager.isSfxEnabled = _isSfxEnabled.value
        SoundManager.setBgmState(_isBgmEnabled.value)
        setupInitialGrid(GameDifficulty.BEGINNER)
        setupFocusTrainGame(GameDifficulty.BEGINNER)
        setupSpeedMatchInitialGrid(GameDifficulty.BEGINNER)
        setupAvatarWhackGame(GameDifficulty.BEGINNER)
        setupStroopGame(GameDifficulty.BEGINNER)
        loadTurtleSoupPuzzles()
        loadTurtleSoupSaveData()
        loadLeaderboard(_selectedCategory.value.key, _selectedGameType.value.key, GameDifficulty.BEGINNER.key)
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString("pref_language", lang.code).apply()
    }

    fun setThemeStyle(style: com.example.ui.theme.AppThemeStyle) {
        _appTheme.value = style
        prefs.edit().putString("pref_theme", style.key).apply()
    }

    fun toggleFullScreen(enabled: Boolean) {
        _isFullScreenEnabled.value = enabled
        prefs.edit().putBoolean("pref_fullscreen", enabled).apply()
    }

    fun toggleSfx(enabled: Boolean) {
        _isSfxEnabled.value = enabled
        SoundManager.isSfxEnabled = enabled
        prefs.edit().putBoolean("pref_sfx", enabled).apply()
    }

    fun toggleBgm(enabled: Boolean) {
        _isBgmEnabled.value = enabled
        SoundManager.setBgmState(enabled)
        prefs.edit().putBoolean("pref_bgm", enabled).apply()
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun setPlayerName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            _playerName.value = trimmed
            prefs.edit().putString("pref_player_name", trimmed).apply()
        }
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun selectCategory(category: GameCategory) {
        _selectedCategory.value = category
        when (category) {
            GameCategory.DEDUCTION -> {
                _selectedGameType.value = GameType.TURTLE_SOUP
            }
            GameCategory.BRAIN -> {
                _selectedGameType.value = GameType.SUDOKU
            }
            GameCategory.TEST -> {
                if (_selectedGameType.value != GameType.FOCUS_TEST && 
                    _selectedGameType.value != GameType.FOCUS_TRAIN && 
                    _selectedGameType.value != GameType.SPEED_MATCH &&
                    _selectedGameType.value != GameType.AVATAR_WHACK &&
                    _selectedGameType.value != GameType.STROOP_EFFECT) {
                    _selectedGameType.value = GameType.FOCUS_TEST
                }
            }
            else -> {
                // Future categories
            }
        }
        loadLeaderboard(category.key, _selectedGameType.value.key, _selectedDifficulty.value.key)
        _currentScreen.value = ScreenState.CATEGORY_DETAIL
    }

    fun selectGameType(gameType: GameType) {
        _selectedGameType.value = gameType
        loadLeaderboard(_selectedCategory.value.key, gameType.key, _selectedDifficulty.value.key)
    }

    fun selectDifficultyAndStart(difficulty: GameDifficulty) {
        _selectedDifficulty.value = difficulty
        loadLeaderboard(_selectedCategory.value.key, _selectedGameType.value.key, difficulty.key)
        SoundManager.onGameSwitched()

        if (_selectedGameType.value == GameType.TURTLE_SOUP) {
            setupTurtleSoupGame(difficulty)
            _currentScreen.value = ScreenState.TURTLE_SOUP_GAME
        } else if (_selectedGameType.value == GameType.SPEED_MATCH) {
            setupSpeedMatchInitialGrid(difficulty)
            _currentScreen.value = ScreenState.SPEED_MATCH_GAME
        } else if (_selectedGameType.value == GameType.FOCUS_TRAIN) {
            setupFocusTrainGame(difficulty)
            _currentScreen.value = ScreenState.FOCUS_TRAIN_GAME
        } else if (_selectedGameType.value == GameType.SUDOKU) {
            setupSudokuGame(difficulty)
            _currentScreen.value = ScreenState.SUDOKU_GAME
        } else if (_selectedGameType.value == GameType.CAT_SUDOKU) {
            setupCatSudokuGame(difficulty)
            _currentScreen.value = ScreenState.CAT_SUDOKU_GAME
        } else if (_selectedGameType.value == GameType.AVATAR_WHACK) {
            setupAvatarWhackGame(difficulty)
            _currentScreen.value = ScreenState.AVATAR_WHACK_GAME
        } else if (_selectedGameType.value == GameType.STROOP_EFFECT) {
            setupStroopGame(difficulty)
            _currentScreen.value = ScreenState.STROOP_EFFECT_GAME
        } else {
            setupInitialGrid(difficulty)
            _currentScreen.value = ScreenState.FOCUS_GAME
        }
    }

    fun setLeaderboardDifficultyFilter(difficulty: GameDifficulty) {
        loadLeaderboard(_selectedCategory.value.key, _selectedGameType.value.key, difficulty.key)
    }

    private fun loadLeaderboard(categoryKey: String, gameTypeKey: String, difficultyKey: String) {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            repository.getTopScores(categoryKey, gameTypeKey, difficultyKey, 10).collectLatest { scores ->
                _leaderboardList.value = scores
            }
        }
    }

    // --- Focus Game Logic ---
    private fun setupInitialGrid(difficulty: GameDifficulty) {
        timerJob?.cancel()
        _gameStatus.value = GameStatus.IDLE
        _elapsedTimeMillis.value = 0L
        _currentTarget.value = 1
        _clearedIndices.value = emptySet()
        _wrongTapIndex.value = null
        _lastCompletedTimeMillis.value = null
        _gridNumbers.value = (1..difficulty.totalCells).toList()
    }

    fun startGame() {
        timerJob?.cancel()
        val diff = _selectedDifficulty.value
        _gridNumbers.value = (1..diff.totalCells).shuffled()
        _clearedIndices.value = emptySet()
        _currentTarget.value = 1
        _wrongTapIndex.value = null
        _elapsedTimeMillis.value = 0L
        _lastCompletedTimeMillis.value = null
        _gameStatus.value = GameStatus.PLAYING

        val startTimeNano = System.nanoTime()
        timerJob = viewModelScope.launch {
            while (isActive && _gameStatus.value == GameStatus.PLAYING) {
                val nowNano = System.nanoTime()
                _elapsedTimeMillis.value = (nowNano - startTimeNano) / 1_000_000L
                delay(10)
            }
        }
    }

    fun resetGame() {
        setupInitialGrid(_selectedDifficulty.value)
    }

    fun onCellTapped(index: Int) {
        if (_gameStatus.value != GameStatus.PLAYING) return
        val number = _gridNumbers.value.getOrNull(index) ?: return
        if (_clearedIndices.value.contains(index)) return

        if (number == _currentTarget.value) {
            val newCleared = _clearedIndices.value + index
            _clearedIndices.value = newCleared

            val total = _selectedDifficulty.value.totalCells
            if (_currentTarget.value == total) {
                timerJob?.cancel()
                _gameStatus.value = GameStatus.COMPLETED
                val finalTime = _elapsedTimeMillis.value
                _lastCompletedTimeMillis.value = finalTime
                SoundManager.playWin()

                viewModelScope.launch {
                    val record = ScoreRecord(
                        playerName = _playerName.value,
                        categoryKey = _selectedCategory.value.key,
                        gameTypeKey = GameType.FOCUS_TEST.key,
                        difficultyKey = _selectedDifficulty.value.key,
                        timeMillis = finalTime,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertScore(record)
                }
            } else {
                SoundManager.playSuccess()
                _currentTarget.value = _currentTarget.value + 1
            }
        } else {
            SoundManager.playError()
            _wrongTapIndex.value = index
            viewModelScope.launch {
                delay(300)
                if (_wrongTapIndex.value == index) {
                    _wrongTapIndex.value = null
                }
            }
        }
    }

    // --- Focus Train (Dynamic Schulte Wheel) Logic ---
    private fun setupFocusTrainGame(difficulty: GameDifficulty) {
        focusTrainTimerJob?.cancel()
        val config = WheelDifficultyConfig.getConfig(difficulty)
        _focusTrainConfig.value = config
        _focusTrainStatus.value = GameStatus.IDLE
        _focusTrainElapsedTimeMillis.value = 0L
        _focusTrainTarget.value = 1
        _focusTrainClearedIndices.value = emptySet()
        _focusTrainWrongTapIndex.value = null
        _focusTrainWrongCount.value = 0
        _focusTrainLastCompletedTimeMillis.value = null
        _focusTrainNumbers.value = (1..config.totalNumbers).toList()
    }

    fun startFocusTrainGame() {
        focusTrainTimerJob?.cancel()
        val diff = _selectedDifficulty.value
        val config = WheelDifficultyConfig.getConfig(diff)
        _focusTrainConfig.value = config
        _focusTrainNumbers.value = (1..config.totalNumbers).shuffled()
        _focusTrainClearedIndices.value = emptySet()
        _focusTrainTarget.value = 1
        _focusTrainWrongTapIndex.value = null
        _focusTrainWrongCount.value = 0
        _focusTrainElapsedTimeMillis.value = 0L
        _focusTrainLastCompletedTimeMillis.value = null
        _focusTrainStatus.value = GameStatus.PLAYING

        val startTimeNano = System.nanoTime()
        focusTrainTimerJob = viewModelScope.launch {
            while (isActive && _focusTrainStatus.value == GameStatus.PLAYING) {
                val nowNano = System.nanoTime()
                _focusTrainElapsedTimeMillis.value = (nowNano - startTimeNano) / 1_000_000L
                delay(10)
            }
        }
    }

    fun resetFocusTrainGame() {
        setupFocusTrainGame(_selectedDifficulty.value)
    }

    fun onFocusTrainCellTapped(index: Int) {
        if (_focusTrainStatus.value != GameStatus.PLAYING) return
        val number = _focusTrainNumbers.value.getOrNull(index) ?: return
        if (_focusTrainClearedIndices.value.contains(index)) return

        if (number == _focusTrainTarget.value) {
            val newCleared = _focusTrainClearedIndices.value + index
            _focusTrainClearedIndices.value = newCleared

            val total = _focusTrainConfig.value.totalNumbers
            if (_focusTrainTarget.value == total) {
                focusTrainTimerJob?.cancel()
                _focusTrainStatus.value = GameStatus.COMPLETED
                val finalTime = _focusTrainElapsedTimeMillis.value
                _focusTrainLastCompletedTimeMillis.value = finalTime
                SoundManager.playWin()

                viewModelScope.launch {
                    val record = ScoreRecord(
                        playerName = _playerName.value,
                        categoryKey = _selectedCategory.value.key,
                        gameTypeKey = GameType.FOCUS_TRAIN.key,
                        difficultyKey = _selectedDifficulty.value.key,
                        timeMillis = finalTime,
                        wrongCount = _focusTrainWrongCount.value,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertScore(record)
                }
            } else {
                SoundManager.playSuccess()
                _focusTrainTarget.value = _focusTrainTarget.value + 1
            }
        } else {
            SoundManager.playError()
            _focusTrainWrongCount.value = _focusTrainWrongCount.value + 1
            _focusTrainWrongTapIndex.value = index
            viewModelScope.launch {
                delay(300)
                if (_focusTrainWrongTapIndex.value == index) {
                    _focusTrainWrongTapIndex.value = null
                }
            }
        }
    }

    // --- Speed Match Game Logic ---
    private fun setupSpeedMatchInitialGrid(difficulty: GameDifficulty) {
        speedMatchTimerJob?.cancel()
        _speedMatchStatus.value = GameStatus.IDLE
        _speedMatchRemainingMs.value = 60_000L
        _correctRounds.value = 0
        _wrongTaps.value = 0
        _selectedMatchCellIndex.value = null
        _isWrongFlash.value = false
        _speedMatchGrid.value = generateSpeedMatchRoundGrid(difficulty)
    }

    fun startSpeedMatchGame() {
        speedMatchTimerJob?.cancel()
        val diff = _selectedDifficulty.value
        _correctRounds.value = 0
        _wrongTaps.value = 0
        _selectedMatchCellIndex.value = null
        _isWrongFlash.value = false
        _speedMatchGrid.value = generateSpeedMatchRoundGrid(diff)
        _speedMatchRemainingMs.value = 60_000L
        _speedMatchStatus.value = GameStatus.PLAYING

        val duration = 60_000L
        val startTime = System.currentTimeMillis()

        speedMatchTimerJob = viewModelScope.launch {
            while (isActive && _speedMatchStatus.value == GameStatus.PLAYING) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = (duration - elapsed).coerceAtLeast(0L)
                _speedMatchRemainingMs.value = remaining

                if (remaining <= 0L) {
                    _speedMatchStatus.value = GameStatus.COMPLETED
                    // Save Speed Match Record
                    val record = ScoreRecord(
                        playerName = _playerName.value,
                        categoryKey = _selectedCategory.value.key,
                        gameTypeKey = GameType.SPEED_MATCH.key,
                        difficultyKey = _selectedDifficulty.value.key,
                        timeMillis = 60_000L,
                        score = _correctRounds.value,
                        wrongCount = _wrongTaps.value,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertScore(record)
                    break
                }
                delay(50)
            }
        }
    }

    fun resetSpeedMatchGame() {
        setupSpeedMatchInitialGrid(_selectedDifficulty.value)
    }

    private fun generateSpeedMatchRoundGrid(difficulty: GameDifficulty): List<Int> {
        val maxNumber = when (difficulty) {
            GameDifficulty.BEGINNER, GameDifficulty.INTERMEDIATE -> 9
            GameDifficulty.ADVANCED -> 12
            GameDifficulty.HARD -> 16
            GameDifficulty.HELL -> 20
            GameDifficulty.EPIC -> 25
        }
        val totalCells = difficulty.speedMatchCells
        val range = (1..maxNumber).toList()
        val pairValue = range.random()
        val remainingCount = (totalCells - 2).coerceAtLeast(0)
        val otherCandidates = range.filter { it != pairValue }.shuffled()
        val otherValues = List(remainingCount) { i -> otherCandidates[i % otherCandidates.size] }.shuffled()
        return (listOf(pairValue, pairValue) + otherValues).shuffled()
    }

    fun onSpeedMatchCellTapped(index: Int) {
        if (_speedMatchStatus.value != GameStatus.PLAYING) return
        val currentGrid = _speedMatchGrid.value
        if (index !in currentGrid.indices) return

        val firstSelected = _selectedMatchCellIndex.value
        if (firstSelected == null) {
            // First cell selected
            _selectedMatchCellIndex.value = index
        } else if (firstSelected == index) {
            // Deselect same cell
            _selectedMatchCellIndex.value = null
        } else {
            // Second cell selected: check matching numbers
            val num1 = currentGrid.getOrNull(firstSelected)
            val num2 = currentGrid.getOrNull(index)

            if (num1 != null && num1 == num2) {
                // Correct Match!
                SoundManager.playSuccess()
                _correctRounds.value = _correctRounds.value + 1
                _selectedMatchCellIndex.value = null
                _speedMatchGrid.value = generateSpeedMatchRoundGrid(_selectedDifficulty.value)
            } else {
                // Wrong Match!
                SoundManager.playError()
                _wrongTaps.value = _wrongTaps.value + 1
                _selectedMatchCellIndex.value = null
                // Trigger warning flash & audio/vibration
                _isWrongFlash.value = true
                viewModelScope.launch {
                    delay(250)
                    _isWrongFlash.value = false
                }
            }
        }
    }

    // --- Sudoku Game Logic ---
    fun setupSudokuGame(difficulty: GameDifficulty) {
        sudokuTimerJob?.cancel()
        _sudokuStatus.value = GameStatus.IDLE
        _sudokuElapsedTimeMillis.value = 0L
        _sudokuWrongCount.value = 0
        _selectedSudokuCellIndex.value = null
        _isPencilMode.value = false
        _sudokuMoveHistory.value = emptyList()
        _sudokuNotesBoard.value = emptyMap()

        val (initial, solution, config) = SudokuGenerator.generatePuzzle(difficulty)
        _sudokuInitialBoard.value = initial
        _sudokuSolutionBoard.value = solution
        _sudokuPlayerBoard.value = initial.toList()
        _sudokuConfig.value = config

        startSudokuGame()
    }

    fun startSudokuGame() {
        sudokuTimerJob?.cancel()
        _sudokuStatus.value = GameStatus.PLAYING

        val startTimeNano = System.nanoTime() - (_sudokuElapsedTimeMillis.value * 1_000_000L)
        sudokuTimerJob = viewModelScope.launch {
            while (isActive && _sudokuStatus.value == GameStatus.PLAYING) {
                val nowNano = System.nanoTime()
                _sudokuElapsedTimeMillis.value = (nowNano - startTimeNano) / 1_000_000L
                delay(10)
            }
        }
    }

    fun resetSudokuGame() {
        setupSudokuGame(_selectedDifficulty.value)
    }

    fun selectSudokuCell(index: Int) {
        if (_sudokuStatus.value != GameStatus.PLAYING) return
        _selectedSudokuCellIndex.value = index
    }

    fun togglePencilMode() {
        _isPencilMode.value = !_isPencilMode.value
    }

    fun inputSudokuSymbol(symbol: String) {
        if (_sudokuStatus.value != GameStatus.PLAYING) return
        val index = _selectedSudokuCellIndex.value ?: return
        val initialBoard = _sudokuInitialBoard.value
        if (index !in initialBoard.indices) return
        if (initialBoard[index].isNotEmpty()) return // Fixed clue cell

        val currentNotesMap = _sudokuNotesBoard.value
        val currentCellNotes = currentNotesMap[index] ?: emptySet()
        val currentVal = _sudokuPlayerBoard.value.getOrNull(index) ?: ""

        if (_isPencilMode.value) {
            // Pencil mode: toggle note
            val newNotes = if (currentCellNotes.contains(symbol)) {
                currentCellNotes - symbol
            } else {
                currentCellNotes + symbol
            }
            val newNotesMap = currentNotesMap.toMutableMap()
            if (newNotes.isEmpty()) {
                newNotesMap.remove(index)
            } else {
                newNotesMap[index] = newNotes
            }
            _sudokuNotesBoard.value = newNotesMap

            _sudokuMoveHistory.value = _sudokuMoveHistory.value + SudokuMove(
                index = index,
                previousValue = currentVal,
                newValue = currentVal,
                previousNotes = currentCellNotes,
                newNotes = newNotes
            )
        } else {
            // Answer mode: fill value
            if (currentVal == symbol) return

            val solutionVal = _sudokuSolutionBoard.value.getOrNull(index) ?: ""
            if (symbol.isNotEmpty() && symbol != solutionVal) {
                SoundManager.playError()
                _sudokuWrongCount.value = _sudokuWrongCount.value + 1
            } else {
                SoundManager.playClick()
            }

            val newBoard = _sudokuPlayerBoard.value.toMutableList()
            newBoard[index] = symbol
            _sudokuPlayerBoard.value = newBoard

            // Clear notes for this cell
            val newNotesMap = currentNotesMap.toMutableMap()
            newNotesMap.remove(index)
            _sudokuNotesBoard.value = newNotesMap

            _sudokuMoveHistory.value = _sudokuMoveHistory.value + SudokuMove(
                index = index,
                previousValue = currentVal,
                newValue = symbol,
                previousNotes = currentCellNotes,
                newNotes = emptySet()
            )

            checkSudokuCompletion()
        }
    }

    fun eraseSudokuCell() {
        if (_sudokuStatus.value != GameStatus.PLAYING) return
        val index = _selectedSudokuCellIndex.value ?: return
        val initialBoard = _sudokuInitialBoard.value
        if (index !in initialBoard.indices || initialBoard[index].isNotEmpty()) return

        val currentVal = _sudokuPlayerBoard.value.getOrNull(index) ?: ""
        val currentNotesMap = _sudokuNotesBoard.value
        val currentCellNotes = currentNotesMap[index] ?: emptySet()

        if (currentVal.isEmpty() && currentCellNotes.isEmpty()) return

        val newBoard = _sudokuPlayerBoard.value.toMutableList()
        newBoard[index] = ""
        _sudokuPlayerBoard.value = newBoard

        val newNotesMap = currentNotesMap.toMutableMap()
        newNotesMap.remove(index)
        _sudokuNotesBoard.value = newNotesMap

        _sudokuMoveHistory.value = _sudokuMoveHistory.value + SudokuMove(
            index = index,
            previousValue = currentVal,
            newValue = "",
            previousNotes = currentCellNotes,
            newNotes = emptySet()
        )
    }

    fun undoSudokuMove() {
        if (_sudokuStatus.value != GameStatus.PLAYING) return
        val history = _sudokuMoveHistory.value
        if (history.isEmpty()) return

        val lastMove = history.last()
        _sudokuMoveHistory.value = history.dropLast(1)

        val newBoard = _sudokuPlayerBoard.value.toMutableList()
        newBoard[lastMove.index] = lastMove.previousValue
        _sudokuPlayerBoard.value = newBoard

        val newNotesMap = _sudokuNotesBoard.value.toMutableMap()
        if (lastMove.previousNotes.isEmpty()) {
            newNotesMap.remove(lastMove.index)
        } else {
            newNotesMap[lastMove.index] = lastMove.previousNotes
        }
        _sudokuNotesBoard.value = newNotesMap
    }

    private fun checkSudokuCompletion() {
        val playerBoard = _sudokuPlayerBoard.value
        val solutionBoard = _sudokuSolutionBoard.value
        if (playerBoard.size == solutionBoard.size && playerBoard.isNotEmpty() && playerBoard == solutionBoard) {
            sudokuTimerJob?.cancel()
            _sudokuStatus.value = GameStatus.COMPLETED
            SoundManager.playWin()

            val record = ScoreRecord(
                playerName = _playerName.value,
                categoryKey = _selectedCategory.value.key,
                gameTypeKey = GameType.SUDOKU.key,
                difficultyKey = _selectedDifficulty.value.key,
                timeMillis = _sudokuElapsedTimeMillis.value,
                score = 0,
                wrongCount = _sudokuWrongCount.value,
                timestamp = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.insertScore(record)
            }
        }
    }

    // --- Cat Sudoku Game Logic ---
    fun setupCatSudokuGame(difficulty: GameDifficulty) {
        catSudokuTimerJob?.cancel()
        _catSudokuStatus.value = GameStatus.IDLE
        _catSudokuElapsedTimeMillis.value = 0L
        _catSudokuWrongCount.value = 0
        _catSudokuHistory.value = emptyList()

        val puzzle = CatSudokuGenerator.generatePuzzle(difficulty)
        _catSudokuSize.value = puzzle.size
        _catSudokuGrid.value = puzzle.grid

        startCatSudokuGame()
    }

    fun startCatSudokuGame() {
        catSudokuTimerJob?.cancel()
        _catSudokuStatus.value = GameStatus.PLAYING

        val startTimeNano = System.nanoTime() - (_catSudokuElapsedTimeMillis.value * 1_000_000L)
        catSudokuTimerJob = viewModelScope.launch {
            while (isActive && _catSudokuStatus.value == GameStatus.PLAYING) {
                val nowNano = System.nanoTime()
                _catSudokuElapsedTimeMillis.value = (nowNano - startTimeNano) / 1_000_000L
                delay(10)
            }
        }
    }

    fun resetCatSudokuGame() {
        setupCatSudokuGame(_selectedDifficulty.value)
    }

    fun clearCatSudokuBoard() {
        if (_catSudokuStatus.value != GameStatus.PLAYING) return
        val currentGrid = _catSudokuGrid.value
        _catSudokuHistory.value = _catSudokuHistory.value + listOf(currentGrid)
        _catSudokuGrid.value = currentGrid.map { it.copy(state = CatCellState.EMPTY, isConflict = false) }
    }

    fun undoCatSudokuMove() {
        if (_catSudokuStatus.value != GameStatus.PLAYING) return
        val history = _catSudokuHistory.value
        if (history.isEmpty()) return

        val previous = history.last()
        _catSudokuHistory.value = history.dropLast(1)
        _catSudokuGrid.value = previous
    }

    fun onCatSudokuCellTapped(index: Int) {
        if (_catSudokuStatus.value != GameStatus.PLAYING) return
        val currentGrid = _catSudokuGrid.value
        if (index !in currentGrid.indices) return

        val cell = currentGrid[index]
        // 循環切換：EMPTY -> CROSS -> CAT -> EMPTY
        val nextState = when (cell.state) {
            CatCellState.EMPTY -> {
                SoundManager.playClick()
                CatCellState.CROSS
            }
            CatCellState.CROSS -> {
                SoundManager.playCatMeow()
                CatCellState.CAT
            }
            CatCellState.CAT -> {
                SoundManager.playClick()
                CatCellState.EMPTY
            }
        }

        _catSudokuHistory.value = _catSudokuHistory.value + listOf(currentGrid)

        val updatedGrid = currentGrid.toMutableList()
        updatedGrid[index] = cell.copy(state = nextState)

        val previousHadConflict = cell.isConflict
        val evaluatedGrid = evaluateConflicts(updatedGrid, _catSudokuSize.value)
        _catSudokuGrid.value = evaluatedGrid

        // 若放貓造成新的衝突，累加錯誤次數
        if (nextState == CatCellState.CAT && evaluatedGrid[index].isConflict && !previousHadConflict) {
            SoundManager.playError()
            _catSudokuWrongCount.value = _catSudokuWrongCount.value + 1
        }

        checkCatSudokuCompletion(evaluatedGrid)
    }

    private fun evaluateConflicts(grid: List<CatCell>, size: Int): List<CatCell> {
        val conflictIndices = mutableSetOf<Int>()
        val catIndices = grid.indices.filter { grid[it].state == CatCellState.CAT }

        // 1. 同行衝突
        for (r in 0 until size) {
            val rowCats = catIndices.filter { grid[it].row == r }
            if (rowCats.size > 1) {
                conflictIndices.addAll(rowCats)
            }
        }

        // 2. 同列衝突
        for (c in 0 until size) {
            val colCats = catIndices.filter { grid[it].col == c }
            if (colCats.size > 1) {
                conflictIndices.addAll(colCats)
            }
        }

        // 3. 同色區塊衝突
        for (regId in 0 until size) {
            val regCats = catIndices.filter { grid[it].regionId == regId }
            if (regCats.size > 1) {
                conflictIndices.addAll(regCats)
            }
        }

        // 4. 周圍 8 格相鄰衝突
        for (i in 0 until catIndices.size) {
            for (j in i + 1 until catIndices.size) {
                val idx1 = catIndices[i]
                val idx2 = catIndices[j]
                val c1 = grid[idx1]
                val c2 = grid[idx2]
                if (kotlin.math.abs(c1.row - c2.row) <= 1 && kotlin.math.abs(c1.col - c2.col) <= 1) {
                    conflictIndices.add(idx1)
                    conflictIndices.add(idx2)
                }
            }
        }

        return grid.mapIndexed { idx, cell ->
            cell.copy(isConflict = conflictIndices.contains(idx))
        }
    }

    private fun checkCatSudokuCompletion(grid: List<CatCell>) {
        val size = _catSudokuSize.value
        val catCells = grid.filter { it.state == CatCellState.CAT }

        if (catCells.size != size) return
        if (grid.any { it.isConflict }) return

        // 驗證是否每行、每列、每個色塊各恰好 1 隻
        val rows = catCells.map { it.row }.toSet()
        val cols = catCells.map { it.col }.toSet()
        val regions = catCells.map { it.regionId }.toSet()

        if (rows.size == size && cols.size == size && regions.size == size) {
            catSudokuTimerJob?.cancel()
            _catSudokuStatus.value = GameStatus.COMPLETED
            SoundManager.playWin()

            val record = ScoreRecord(
                playerName = _playerName.value,
                categoryKey = _selectedCategory.value.key,
                gameTypeKey = GameType.CAT_SUDOKU.key,
                difficultyKey = _selectedDifficulty.value.key,
                timeMillis = _catSudokuElapsedTimeMillis.value,
                score = 0,
                wrongCount = _catSudokuWrongCount.value,
                timestamp = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.insertScore(record)
            }
        }
    }

    // --- Dialogs ---
    fun openLeaderboardDialog() {
        _showLeaderboardDialog.value = true
    }

    fun closeLeaderboardDialog() {
        _showLeaderboardDialog.value = false
    }

    fun openNameDialog() {
        _showNameDialog.value = true
    }

    fun closeNameDialog() {
        _showNameDialog.value = false
    }

    fun clearScoresForCurrentLevel() {
        viewModelScope.launch {
            repository.clearCategoryScores(
                selectedCategory.value.key,
                selectedGameType.value.key,
                selectedDifficulty.value.key
            )
        }
    }

    // --- Turtle Soup Game Logic ---
    private fun loadTurtleSoupPuzzles() {
        val app = getApplication<Application>()
        val list = TurtleSoupRepository.loadPuzzles(app)
        _turtleSoupPuzzles.value = list
    }

    private fun loadTurtleSoupSaveData() {
        val app = getApplication<Application>()
        val data = TurtleSoupRepository.loadSaveData(app)
        _turtleSoupSaveData.value = data
    }

    fun setupTurtleSoupGame(difficulty: GameDifficulty, puzzle: TurtleSoupPuzzle? = null) {
        turtleSoupTimerJob?.cancel()
        _turtleSoupElapsedSeconds.value = 0L
        _turtleSoupUnlockedQuestions.value = emptySet()
        _turtleSoupQueryLogs.value = emptyList()
        _turtleSoupSelectedDimensions.value = emptyMap()
        _turtleSoupDiscoveredCoreCount.value = 0
        _turtleSoupShowAdDialog.value = false
        _turtleSoupSelectedSlots.value = emptyMap()
        _turtleSoupIsDeductionError.value = false
        _turtleSoupFinalScore.value = 0
        _turtleSoupFinalStars.value = 0
        _turtleSoupUsedAdReward.value = false

        if (puzzle != null) {
            selectTurtleSoupPuzzle(puzzle)
        } else {
            _turtleSoupCurrentPuzzle.value = null
            _turtleSoupPlayState.value = TurtleSoupPlayState.PUZZLE_SELECT
        }
    }

    fun selectTurtleSoupPuzzle(puzzle: TurtleSoupPuzzle) {
        turtleSoupTimerJob?.cancel()
        _turtleSoupCurrentPuzzle.value = puzzle
        val initial = TurtleSoupRepository.getInitialChances(puzzle.difficulty)
        _turtleSoupRemainingChances.value = initial
        _turtleSoupUsedChances.value = 0
        _turtleSoupElapsedSeconds.value = 0L
        _turtleSoupUnlockedQuestions.value = emptySet()
        _turtleSoupQueryLogs.value = emptyList()
        _turtleSoupDiscoveredCoreCount.value = 0
        _turtleSoupShowAdDialog.value = false
        _turtleSoupSelectedSlots.value = emptyMap()
        _turtleSoupIsDeductionError.value = false
        _turtleSoupFinalScore.value = 0
        _turtleSoupFinalStars.value = 0
        _turtleSoupUsedAdReward.value = false

        // 預設選取各維度的第 1 個選項
        val dims = TurtleSoupRepository.getEffectiveDimensions(puzzle, _language.value)
        val defaultSelections = mutableMapOf<String, String>()
        dims.forEach { dim ->
            val opts = dim.options.get(_language.value)
            if (opts.isNotEmpty()) {
                defaultSelections[dim.id] = opts[0]
            }
        }
        _turtleSoupSelectedDimensions.value = defaultSelections

        _turtleSoupPlayState.value = TurtleSoupPlayState.INVESTIGATING
        startTurtleSoupTimer()
    }

    fun selectTurtleSoupDimensionOption(dimensionId: String, option: String) {
        val current = _turtleSoupSelectedDimensions.value.toMutableMap()
        current[dimensionId] = option
        _turtleSoupSelectedDimensions.value = current
        SoundManager.playClick()
    }

    fun submitTurtleSoupInquiry(activity: Activity? = null) {
        val puzzle = _turtleSoupCurrentPuzzle.value ?: return
        if (_turtleSoupPlayState.value != TurtleSoupPlayState.INVESTIGATING) return

        if (_turtleSoupRemainingChances.value <= 0) {
            _turtleSoupShowAdDialog.value = true
            return
        }

        val selections = _turtleSoupSelectedDimensions.value
        if (selections.isEmpty()) return

        // 扣除 1 次機會
        _turtleSoupRemainingChances.value = _turtleSoupRemainingChances.value - 1
        _turtleSoupUsedChances.value = _turtleSoupUsedChances.value + 1

        val log = TurtleSoupRepository.evaluateQuery(puzzle, selections, _language.value)

        // 判斷是否點亮核心線索
        if (log.isCore) {
            val isAlreadyDiscovered = _turtleSoupQueryLogs.value.any { it.isCore && it.detail == log.detail }
            if (!isAlreadyDiscovered) {
                _turtleSoupDiscoveredCoreCount.value = _turtleSoupDiscoveredCoreCount.value + 1
            }
            SoundManager.playSuccess()
        } else if (log.answer == QuestionAnswer.YES) {
            SoundManager.playClick()
        } else if (log.answer == QuestionAnswer.NO) {
            SoundManager.playError()
        } else {
            SoundManager.playClick()
        }

        _turtleSoupQueryLogs.value = _turtleSoupQueryLogs.value + log
    }

    fun closeTurtleSoupAdDialog() {
        _turtleSoupShowAdDialog.value = false
    }

    fun watchAdForTurtleSoupInquiry(activity: Activity?) {
        closeTurtleSoupAdDialog()
        AdManager.showAdNow(activity) {
            val puzzle = _turtleSoupCurrentPuzzle.value
            val rewardCount = puzzle?.adRewardChances ?: 3
            _turtleSoupRemainingChances.value = _turtleSoupRemainingChances.value + rewardCount
            _turtleSoupUsedAdReward.value = true
            SoundManager.playSuccess()
            // 獲得次數後自動送出當前詢問
            submitTurtleSoupInquiry(activity)
        }
    }

    private fun startTurtleSoupTimer() {
        turtleSoupTimerJob?.cancel()
        val startTimeNano = System.nanoTime() - (_turtleSoupElapsedSeconds.value * 1_000_000_000L)
        turtleSoupTimerJob = viewModelScope.launch {
            while (isActive && _turtleSoupPlayState.value != TurtleSoupPlayState.SUCCESS) {
                val nowNano = System.nanoTime()
                _turtleSoupElapsedSeconds.value = (nowNano - startTimeNano) / 1_000_000_000L
                delay(500)
            }
        }
    }

    fun unlockTurtleSoupQuestion(questionIndex: Int) {
        if (_turtleSoupPlayState.value != TurtleSoupPlayState.INVESTIGATING) return
        if (_turtleSoupUnlockedQuestions.value.contains(questionIndex)) return
        if (_turtleSoupRemainingChances.value <= 0) return

        SoundManager.playClick()
        _turtleSoupRemainingChances.value = _turtleSoupRemainingChances.value - 1
        _turtleSoupUsedChances.value = _turtleSoupUsedChances.value + 1
        _turtleSoupUnlockedQuestions.value = _turtleSoupUnlockedQuestions.value + questionIndex

        val currentPuzzle = _turtleSoupCurrentPuzzle.value
        if (currentPuzzle != null && currentPuzzle.questions.getOrNull(questionIndex)?.isCore == true) {
            SoundManager.playSuccess()
        }
    }

    fun selectTurtleSoupSlotOption(slotIndex: Int, optionIndex: Int) {
        val current = _turtleSoupSelectedSlots.value.toMutableMap()
        current[slotIndex] = optionIndex
        _turtleSoupSelectedSlots.value = current
        SoundManager.playClick()
    }

    fun startTurtleSoupSolving() {
        if (_turtleSoupPlayState.value != TurtleSoupPlayState.INVESTIGATING) return
        SoundManager.playClick()
        _turtleSoupPlayState.value = TurtleSoupPlayState.SOLVING
    }

    fun backToTurtleSoupInvestigate() {
        if (_turtleSoupPlayState.value != TurtleSoupPlayState.SOLVING) return
        SoundManager.playClick()
        _turtleSoupPlayState.value = TurtleSoupPlayState.INVESTIGATING
    }

    fun submitTurtleSoupDeduction() {
        val puzzle = _turtleSoupCurrentPuzzle.value ?: return
        val slots = puzzle.slotDeduction.slots
        val selected = _turtleSoupSelectedSlots.value

        // 驗證是否全部正確
        val isAllCorrect = slots.indices.all { idx ->
            selected[idx] == slots[idx].correctIndex
        }

        if (isAllCorrect) {
            // 通關成功
            turtleSoupTimerJob?.cancel()
            val (score, stars) = TurtleSoupRepository.calculateScoreAndStars(
                difficulty = puzzle.difficulty,
                usedChances = _turtleSoupUsedChances.value,
                remainingChances = _turtleSoupRemainingChances.value,
                elapsedSeconds = _turtleSoupElapsedSeconds.value,
                usedAdReward = _turtleSoupUsedAdReward.value
            )

            _turtleSoupFinalScore.value = score
            _turtleSoupFinalStars.value = stars
            _turtleSoupPlayState.value = TurtleSoupPlayState.SUCCESS
            SoundManager.playWin()

            // 儲存至本機存檔
            val app = getApplication<Application>()
            val updatedSave = TurtleSoupRepository.savePuzzleCleared(
                context = app,
                puzzleId = puzzle.id,
                difficulty = puzzle.difficulty,
                stars = stars,
                usedChances = _turtleSoupUsedChances.value,
                score = score
            )
            _turtleSoupSaveData.value = updatedSave

            // 寫入 Room 排行榜
            viewModelScope.launch {
                val record = ScoreRecord(
                    playerName = _playerName.value,
                    categoryKey = _selectedCategory.value.key,
                    gameTypeKey = GameType.TURTLE_SOUP.key,
                    difficultyKey = _selectedDifficulty.value.key,
                    timeMillis = _turtleSoupElapsedSeconds.value * 1000L,
                    score = score,
                    wrongCount = _turtleSoupUsedChances.value,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertScore(record)
            }
        } else {
            // 答錯：扣除 2 次機會，退回 INVESTIGATING 並提示錯誤
            SoundManager.playError()
            _turtleSoupRemainingChances.value = (_turtleSoupRemainingChances.value - 2).coerceAtLeast(0)
            _turtleSoupUsedChances.value = _turtleSoupUsedChances.value + 2
            _turtleSoupIsDeductionError.value = true

            viewModelScope.launch {
                delay(600)
                _turtleSoupIsDeductionError.value = false
                _turtleSoupPlayState.value = TurtleSoupPlayState.INVESTIGATING
            }
        }
    }

    fun watchAdForTurtleSoupChances() {
        val puzzle = _turtleSoupCurrentPuzzle.value
        val rewardCount = puzzle?.adRewardChances ?: 3
        _turtleSoupRemainingChances.value = _turtleSoupRemainingChances.value + rewardCount
        _turtleSoupUsedAdReward.value = true
        SoundManager.playSuccess()
    }

    fun giveUpTurtleSoupGame() {
        turtleSoupTimerJob?.cancel()
        _turtleSoupPlayState.value = TurtleSoupPlayState.PUZZLE_SELECT
    }

    fun restartTurtleSoupPuzzle() {
        turtleSoupTimerJob?.cancel()
        _turtleSoupPlayState.value = TurtleSoupPlayState.PUZZLE_SELECT
    }

    // ==========================================
    // --- Avatar Whack (打那個誰反應戰) Logic ---
    // ==========================================
    fun setupAvatarWhackGame(difficulty: GameDifficulty) {
        whackTimerJob?.cancel()
        whackLoopJob?.cancel()
        val config = WhackDifficultyConfig.getConfig(difficulty)
        _whackConfig.value = config
        _whackStatus.value = GameStatus.IDLE
        _whackScore.value = 0
        _whackCombo.value = 0
        _whackMaxCombo.value = 0
        _whackHits.value = 0
        _whackMisses.value = 0
        _whackBombHits.value = 0
        _whackRemainingMs.value = config.gameDurationMs
        _whackLastCompletedScore.value = null
        _whackHoles.value = List(config.totalHoles) { idx ->
            WhackHoleState(holeIndex = idx)
        }
    }

    fun startAvatarWhackGame() {
        whackTimerJob?.cancel()
        whackLoopJob?.cancel()
        val diff = _selectedDifficulty.value
        val config = WhackDifficultyConfig.getConfig(diff)
        _whackConfig.value = config
        _whackScore.value = 0
        _whackCombo.value = 0
        _whackMaxCombo.value = 0
        _whackHits.value = 0
        _whackMisses.value = 0
        _whackBombHits.value = 0
        _whackRemainingMs.value = config.gameDurationMs
        _whackLastCompletedScore.value = null
        _whackHoles.value = List(config.totalHoles) { idx ->
            WhackHoleState(holeIndex = idx)
        }
        _whackStatus.value = GameStatus.PLAYING

        val startTime = System.currentTimeMillis()
        val duration = config.gameDurationMs

        // 1. 全局遊戲倒數計時器
        whackTimerJob = viewModelScope.launch {
            while (isActive && _whackStatus.value == GameStatus.PLAYING) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = (duration - elapsed).coerceAtLeast(0L)
                _whackRemainingMs.value = remaining

                if (remaining <= 0L) {
                    finishAvatarWhackGame()
                    break
                }
                delay(50)
            }
        }

        // 2. 出擊與縮回排程循環
        whackLoopJob = viewModelScope.launch {
            while (isActive && _whackStatus.value == GameStatus.PLAYING) {
                spawnAvatarsRound()
                delay(config.spawnIntervalMs)
            }
        }
    }

    private fun spawnAvatarsRound() {
        val config = _whackConfig.value
        val currentHoles = _whackHoles.value
        val availableHoles = currentHoles.filter { !it.isPopping && !it.isHit }
        if (availableHoles.isEmpty()) return

        val spawnCount = config.simultaneousTargets.coerceAtMost(availableHoles.size)
        val selectedHoles = availableHoles.shuffled().take(spawnCount)
        val now = System.currentTimeMillis()

        val updatedList = currentHoles.toMutableList()

        for (hole in selectedHoles) {
            val rand = Math.random().toFloat()
            val isBomb = rand < config.bombProb
            val isBonus = !isBomb && rand < (config.bombProb + config.bonusProb)
            val isFakeOut = !isBomb && !isBonus && (Math.random().toFloat() < config.fakeOutProb)

            val type = when {
                isBomb -> AvatarType.BOMB
                isBonus -> AvatarType.BONUS
                else -> AvatarType.NORMAL
            }

            val expression = if (isFakeOut) AvatarExpression.FAKE_OUT else AvatarExpression.NORMAL
            val avatarIdx = AvatarCatalog.getRandomAvatarIndex()

            val stayMs = config.calculateStayDuration(isFakeOut)

            val newHoleState = WhackHoleState(
                holeIndex = hole.holeIndex,
                avatar = CharacterAvatar(
                    id = "avatar_${hole.holeIndex}_$now",
                    avatarIndex = avatarIdx,
                    type = type,
                    expression = expression
                ),
                isPopping = true,
                isHit = false,
                isFakeOut = isFakeOut,
                spawnTimeMs = now,
                stayDurationMs = stayMs
            )
            updatedList[hole.holeIndex] = newHoleState

            // 啟動單個洞口超時自動縮回
            viewModelScope.launch {
                delay(stayMs)
                val current = _whackHoles.value.getOrNull(hole.holeIndex)
                if (current != null && current.isPopping && !current.isHit) {
                    // 若是正常/黃金目標且玩家沒打到，判定為漏打 (Miss)，重置 Combo
                    if (current.avatar?.type == AvatarType.NORMAL || current.avatar?.type == AvatarType.BONUS) {
                        if (!current.isFakeOut) {
                            _whackCombo.value = 0
                            _whackMisses.value = _whackMisses.value + 1
                        }
                    }
                    // 縮回洞中
                    val resetHole = WhackHoleState(holeIndex = hole.holeIndex)
                    val list = _whackHoles.value.toMutableList()
                    list[hole.holeIndex] = resetHole
                    _whackHoles.value = list
                }
            }
        }

        _whackHoles.value = updatedList
    }

    fun onWhackHoleTapped(holeIndex: Int) {
        if (_whackStatus.value != GameStatus.PLAYING) return
        val currentHoles = _whackHoles.value
        val hole = currentHoles.getOrNull(holeIndex) ?: return
        if (!hole.isPopping || hole.isHit) return

        val avatar = hole.avatar ?: return
        val list = currentHoles.toMutableList()

        when (avatar.type) {
            AvatarType.BOMB -> {
                // 誤擊炸彈：扣分、中斷 Combo
                SoundManager.playError()
                _whackScore.value = (_whackScore.value - 200).coerceAtLeast(0)
                _whackCombo.value = 0
                _whackBombHits.value = _whackBombHits.value + 1
                list[holeIndex] = hole.copy(isHit = true)
                _whackHoles.value = list
            }
            AvatarType.BONUS -> {
                // 擊中幸運黃金星：高額加分 + Combo
                SoundManager.playWin()
                val currentCombo = _whackCombo.value + 1
                _whackCombo.value = currentCombo
                if (currentCombo > _whackMaxCombo.value) {
                    _whackMaxCombo.value = currentCombo
                }
                val gain = 250 + (currentCombo * 30)
                _whackScore.value = _whackScore.value + gain
                _whackHits.value = _whackHits.value + 1
                list[holeIndex] = hole.copy(isHit = true, avatar = avatar.copy(expression = AvatarExpression.HIT))
                _whackHoles.value = list
            }
            AvatarType.NORMAL -> {
                // 正常目標或假動作探頭中
                SoundManager.playSuccess()
                val currentCombo = _whackCombo.value + 1
                _whackCombo.value = currentCombo
                if (currentCombo > _whackMaxCombo.value) {
                    _whackMaxCombo.value = currentCombo
                }
                val base = if (hole.isFakeOut) 150 else 100
                val comboBonus = currentCombo * 20
                _whackScore.value = _whackScore.value + base + comboBonus
                _whackHits.value = _whackHits.value + 1
                list[holeIndex] = hole.copy(isHit = true, avatar = avatar.copy(expression = AvatarExpression.HIT))
                _whackHoles.value = list
            }
        }

        // 擊中後短暫停頓 400ms 後縮回
        viewModelScope.launch {
            delay(400)
            val updated = _whackHoles.value.toMutableList()
            if (updated.indices.contains(holeIndex) && updated[holeIndex].isHit) {
                updated[holeIndex] = WhackHoleState(holeIndex = holeIndex)
                _whackHoles.value = updated
            }
        }
    }

    private fun finishAvatarWhackGame() {
        whackTimerJob?.cancel()
        whackLoopJob?.cancel()
        _whackStatus.value = GameStatus.COMPLETED
        val finalScore = _whackScore.value
        _whackLastCompletedScore.value = finalScore
        SoundManager.playWin()

        viewModelScope.launch {
            val record = ScoreRecord(
                playerName = _playerName.value,
                categoryKey = _selectedCategory.value.key,
                gameTypeKey = GameType.AVATAR_WHACK.key,
                difficultyKey = _selectedDifficulty.value.key,
                timeMillis = _whackConfig.value.gameDurationMs,
                score = finalScore,
                wrongCount = _whackMisses.value + _whackBombHits.value,
                timestamp = System.currentTimeMillis()
            )
            repository.insertScore(record)
        }
    }

    fun resetAvatarWhackGame() {
        setupAvatarWhackGame(_selectedDifficulty.value)
    }

    // ==========================================
    // --- Stroop Effect (斯特魯普效應) Logic ---
    // ==========================================
    fun setupStroopGame(difficulty: GameDifficulty) {
        stroopGameTimerJob?.cancel()
        stroopQuestionTimerJob?.cancel()
        val inst = StroopGenerator.pickRoundInstruction(difficulty)
        _stroopRoundInstruction.value = inst
        _stroopStatus.value = GameStatus.IDLE
        _stroopScore.value = 0
        _stroopCombo.value = 0
        _stroopMaxCombo.value = 0
        _stroopCorrectCount.value = 0
        _stroopWrongCount.value = 0
        _stroopRemainingGameTimeMs.value = 45_000L
        _stroopQuestionTimeProgress.value = 1f
        _stroopIsWrongFlash.value = false
        _stroopLastCompletedScore.value = null
        _stroopCurrentQuestion.value = StroopGenerator.generateQuestion(difficulty, inst)
    }

    fun startStroopGame() {
        stroopGameTimerJob?.cancel()
        stroopQuestionTimerJob?.cancel()
        val diff = _selectedDifficulty.value
        val inst = _stroopRoundInstruction.value ?: StroopGenerator.pickRoundInstruction(diff)
        _stroopRoundInstruction.value = inst
        _stroopScore.value = 0
        _stroopCombo.value = 0
        _stroopMaxCombo.value = 0
        _stroopCorrectCount.value = 0
        _stroopWrongCount.value = 0
        _stroopRemainingGameTimeMs.value = 45_000L
        _stroopIsWrongFlash.value = false
        _stroopLastCompletedScore.value = null
        _stroopStatus.value = GameStatus.PLAYING

        nextStroopQuestion()

        val startTime = System.currentTimeMillis()
        val duration = 45_000L

        // 1. 全局 45 秒倒數
        stroopGameTimerJob = viewModelScope.launch {
            while (isActive && _stroopStatus.value == GameStatus.PLAYING) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = (duration - elapsed).coerceAtLeast(0L)
                _stroopRemainingGameTimeMs.value = remaining

                if (remaining <= 0L) {
                    finishStroopGame()
                    break
                }
                delay(50)
            }
        }
    }

    private fun nextStroopQuestion() {
        stroopQuestionTimerJob?.cancel()
        if (_stroopStatus.value != GameStatus.PLAYING) return

        val diff = _selectedDifficulty.value
        val inst = _stroopRoundInstruction.value ?: StroopGenerator.pickRoundInstruction(diff)
        val q = StroopGenerator.generateQuestion(diff, inst)
        _stroopCurrentQuestion.value = q
        _stroopQuestionRemainingMs.value = q.timeLimitMs
        _stroopQuestionTimeProgress.value = 1f

        val qStartTime = System.currentTimeMillis()
        val qDuration = q.timeLimitMs

        stroopQuestionTimerJob = viewModelScope.launch {
            while (isActive && _stroopStatus.value == GameStatus.PLAYING) {
                val elapsed = System.currentTimeMillis() - qStartTime
                val remaining = (qDuration - elapsed).coerceAtLeast(0L)
                _stroopQuestionRemainingMs.value = remaining
                _stroopQuestionTimeProgress.value = (remaining.toFloat() / qDuration.toFloat()).coerceIn(0f, 1f)

                if (remaining <= 0L) {
                    // 本題超時：視為答錯，扣分並進入下一題
                    SoundManager.playError()
                    _stroopCombo.value = 0
                    _stroopWrongCount.value = _stroopWrongCount.value + 1
                    _stroopScore.value = (_stroopScore.value - 50).coerceAtLeast(0)
                    nextStroopQuestion()
                    break
                }
                delay(30)
            }
        }
    }

    fun onStroopOptionSelected(option: StroopOption) {
        if (_stroopStatus.value != GameStatus.PLAYING) return
        val currentQ = _stroopCurrentQuestion.value ?: return

        if (option.isCorrect) {
            // 答對！
            SoundManager.playSuccess()
            val currentCombo = _stroopCombo.value + 1
            _stroopCombo.value = currentCombo
            if (currentCombo > _stroopMaxCombo.value) {
                _stroopMaxCombo.value = currentCombo
            }
            val speedBonus = (_stroopQuestionTimeProgress.value * 50).toInt()
            val comboBonus = currentCombo * 15
            _stroopScore.value = _stroopScore.value + 100 + speedBonus + comboBonus
            _stroopCorrectCount.value = _stroopCorrectCount.value + 1
            nextStroopQuestion()
        } else {
            // 答錯！
            SoundManager.playError()
            _stroopCombo.value = 0
            _stroopWrongCount.value = _stroopWrongCount.value + 1
            _stroopScore.value = (_stroopScore.value - 50).coerceAtLeast(0)
            _stroopIsWrongFlash.value = true

            viewModelScope.launch {
                delay(350)
                _stroopIsWrongFlash.value = false
                nextStroopQuestion()
            }
        }
    }

    private fun finishStroopGame() {
        stroopGameTimerJob?.cancel()
        stroopQuestionTimerJob?.cancel()
        _stroopStatus.value = GameStatus.COMPLETED
        val finalScore = _stroopScore.value
        _stroopLastCompletedScore.value = finalScore
        SoundManager.playWin()

        viewModelScope.launch {
            val record = ScoreRecord(
                playerName = _playerName.value,
                categoryKey = _selectedCategory.value.key,
                gameTypeKey = GameType.STROOP_EFFECT.key,
                difficultyKey = _selectedDifficulty.value.key,
                timeMillis = 45_000L,
                score = finalScore,
                wrongCount = _stroopWrongCount.value,
                timestamp = System.currentTimeMillis()
            )
            repository.insertScore(record)
        }
    }

    fun resetStroopGame() {
        setupStroopGame(_selectedDifficulty.value)
    }
}
