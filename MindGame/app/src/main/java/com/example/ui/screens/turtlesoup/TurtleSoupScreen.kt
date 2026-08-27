package com.example.ui.screens.turtlesoup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.Localization
import com.example.data.model.TurtleSoupPuzzle
import com.example.data.model.TurtleSoupRecord
import com.example.data.model.TurtleSoupSaveData

import com.example.data.model.GameDifficulty
import com.example.data.model.InvestigationQueryLog
import com.example.game.turtlesoup.TurtleSoupRepository

enum class TurtleSoupPlayState {
    PUZZLE_SELECT,    // 題目列表選擇
    INVESTIGATING,    // 是非題探索中
    SOLVING,          // 複合填空結案中
    SUCCESS           // 結案成功結算
}

@Composable
fun TurtleSoupScreen(
    difficulty: GameDifficulty,
    puzzles: List<TurtleSoupPuzzle>,
    currentPuzzle: TurtleSoupPuzzle?,
    playState: TurtleSoupPlayState,
    queryLogs: List<InvestigationQueryLog>,
    selectedDimensions: Map<String, String>,
    discoveredCoreCount: Int,
    showAdDialog: Boolean,
    unlockedQuestionIndices: Set<Int>,
    selectedSlotIndices: Map<Int, Int>, // slotIndex -> selectedOptionIndex
    remainingChances: Int,
    usedChances: Int,
    elapsedSeconds: Long,
    isDeductionErrorFlash: Boolean,
    finalScore: Int,
    finalStars: Int,
    usedAdReward: Boolean,
    saveData: TurtleSoupSaveData,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onSelectPuzzle: (TurtleSoupPuzzle) -> Unit,
    onSelectDimensionOption: (String, String) -> Unit,
    onSubmitInquiry: () -> Unit,
    onWatchAdForInquiry: () -> Unit,
    onCloseAdDialog: () -> Unit,
    onUnlockQuestion: (Int) -> Unit,
    onSelectSlotOption: (Int, Int) -> Unit,
    onStartSolving: () -> Unit,
    onBackToInvestigate: () -> Unit,
    onSubmitDeduction: () -> Unit,
    onWatchAdForChances: () -> Unit,
    onGiveUpGame: () -> Unit,
    onRestartPuzzle: () -> Unit,
    onLeaderboardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOutOfChancesDialog by remember { mutableStateOf(false) }
    var showTruthDialog by remember { mutableStateOf(false) }

    val diffTitleKey = when (difficulty) {
        GameDifficulty.BEGINNER -> "turtle_soup_diff_easy"
        GameDifficulty.INTERMEDIATE -> "turtle_soup_diff_medium"
        else -> "turtle_soup_diff_hard"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MysteryThemeColors.BackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            // 頂部導航列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (playState == TurtleSoupPlayState.SOLVING) {
                            onBackToInvestigate()
                        } else if (playState != TurtleSoupPlayState.PUZZLE_SELECT) {
                            onRestartPuzzle()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MysteryThemeColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (playState == TurtleSoupPlayState.PUZZLE_SELECT) {
                            "${Localization.getString("game_turtle_soup", language)} - ${Localization.getString(diffTitleKey, language)}"
                        } else {
                            Localization.getString("game_turtle_soup", language)
                        },
                        color = MysteryThemeColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onLeaderboardClick) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = MysteryThemeColors.CoreClueGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 依 PlayState 呈現不同畫面
            when (playState) {
                TurtleSoupPlayState.PUZZLE_SELECT -> {
                    val filteredPuzzles = when (difficulty) {
                        GameDifficulty.BEGINNER -> puzzles.filter { it.difficulty.equals("Easy", ignoreCase = true) }
                        GameDifficulty.INTERMEDIATE -> puzzles.filter { it.difficulty.equals("Medium", ignoreCase = true) }
                        else -> puzzles.filter { it.difficulty.equals("Hard", ignoreCase = true) }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(filteredPuzzles) { index, puzzle ->
                            val record = saveData.records.find { it.puzzleId == puzzle.id }
                            PuzzleListItemCard(
                                index = index,
                                puzzle = puzzle,
                                record = record,
                                language = language,
                                onClick = { onSelectPuzzle(puzzle) }
                            )
                        }
                    }
                }

                TurtleSoupPlayState.INVESTIGATING -> {
                    currentPuzzle?.let { puzzle ->
                        val effectiveDimensions = remember(puzzle, language) {
                            TurtleSoupRepository.getEffectiveDimensions(puzzle, language)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            item {
                                TurtleSoupStatusBar(
                                    difficulty = puzzle.difficulty,
                                    title = puzzle.title.get(language),
                                    remainingChances = remainingChances,
                                    discoveredCoreClues = discoveredCoreCount,
                                    requiredCoreClues = puzzle.requiredCoreCluesCount,
                                    elapsedSeconds = elapsedSeconds,
                                    language = language
                                )
                            }

                            item {
                                TurtleSoupSurfaceCard(
                                    categoryName = puzzle.category.get(language),
                                    surfaceText = puzzle.surface.get(language),
                                    language = language
                                )
                            }

                            item {
                                // 歷史偵訊問答清單 (Investigation Chat Log)
                                InvestigationChatLogView(
                                    queryLogs = queryLogs,
                                    language = language
                                )
                            }

                            item {
                                // 多維度人事物下拉選單/選擇器
                                InvestigationDimensionSelector(
                                    dimensions = effectiveDimensions,
                                    selectedDimensions = selectedDimensions,
                                    language = language,
                                    onOptionSelected = onSelectDimensionOption
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                // 操作按鈕：詢問 與 我知道真相了
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 💬 詢問按鈕
                                    Button(
                                        onClick = onSubmitInquiry,
                                        modifier = Modifier.weight(1.3f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (remainingChances > 0) MysteryThemeColors.CoreClueGold else MysteryThemeColors.NoRed
                                        )
                                    ) {
                                        Text(
                                            text = if (remainingChances > 0) {
                                                "${Localization.getString("turtle_soup_ask_btn", language)} ($remainingChances)"
                                            } else {
                                                "🎬 ${Localization.getString("turtle_soup_watch_ad_btn", language)}"
                                            },
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    // ✍ 我知道真相了 (直接結案)
                                    OutlinedButton(
                                        onClick = onStartSolving,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MysteryThemeColors.CoreClueGold)
                                    ) {
                                        Text(
                                            text = "✍ ${Localization.getString("turtle_soup_know_truth", language)}",
                                            color = MysteryThemeColors.CoreClueGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TurtleSoupPlayState.SOLVING -> {
                    currentPuzzle?.let { puzzle ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            item {
                                // 填空提示卡片
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDeductionErrorFlash) MysteryThemeColors.NoRed.copy(alpha = 0.25f) else Color(0xFF232536)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDeductionErrorFlash) MysteryThemeColors.NoRed else MysteryThemeColors.CoreClueGold
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "✍ ${Localization.getString("turtle_soup_solving_title", language)}",
                                            color = MysteryThemeColors.CoreClueGold,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = puzzle.slotDeduction.template.get(language),
                                            color = MysteryThemeColors.TextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp
                                        )

                                        if (isDeductionErrorFlash) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "⚠ ${Localization.getString("turtle_soup_deduction_wrong", language)}",
                                                color = MysteryThemeColors.NoRed,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 各 Slot 膠囊選擇器
                            itemsIndexed(puzzle.slotDeduction.slots) { slotIdx, slot ->
                                DeductionSlotItem(
                                    slotIndex = slotIdx,
                                    slot = slot,
                                    selectedIndex = selectedSlotIndices[slotIdx],
                                    language = language,
                                    onOptionSelected = { optIdx ->
                                        onSelectSlotOption(slotIdx, optIdx)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onBackToInvestigate,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, MysteryThemeColors.CardBorder)
                                    ) {
                                        Text(
                                            text = Localization.getString("turtle_soup_back_to_investigate", language),
                                            color = MysteryThemeColors.TextSecondary
                                        )
                                    }

                                    val isAllSlotsSelected = puzzle.slotDeduction.slots.indices.all {
                                        selectedSlotIndices[it] != null
                                    }

                                    Button(
                                        onClick = onSubmitDeduction,
                                        enabled = isAllSlotsSelected,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MysteryThemeColors.CoreClueGold,
                                            disabledContainerColor = Color(0xFF374151)
                                        )
                                    ) {
                                        Text(
                                            text = Localization.getString("turtle_soup_submit_deduction", language),
                                            color = if (isAllSlotsSelected) Color.Black else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TurtleSoupPlayState.SUCCESS -> {
                    currentPuzzle?.let { puzzle ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MysteryThemeColors.CardDark),
                                    border = BorderStroke(1.5.dp, MysteryThemeColors.CoreClueGold)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        StampAndStarsView(
                                            stars = finalStars,
                                            score = finalScore,
                                            usedChances = usedChances,
                                            usedAdReward = usedAdReward,
                                            language = language
                                        )
                                    }
                                }
                            }

                            item {
                                // 完整湯底真相揭示
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232536)),
                                    border = BorderStroke(1.dp, MysteryThemeColors.CardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = Localization.getString("turtle_soup_bottom_truth_title", language),
                                            color = MysteryThemeColors.CoreClueGold,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = puzzle.bottom.get(language),
                                            color = MysteryThemeColors.TextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onRestartPuzzle,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, MysteryThemeColors.CardBorder)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = MysteryThemeColors.TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Localization.getString("play_again", language),
                                            color = MysteryThemeColors.TextPrimary
                                        )
                                    }

                                    Button(
                                        onClick = onRestartPuzzle, // 返回選題
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MysteryThemeColors.CoreClueGold)
                                    ) {
                                        Text(
                                            text = Localization.getString("back_to_menu", language),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 彈窗
        if (showAdDialog) {
            OutOfChancesAskDialog(
                language = language,
                onWatchAdAndSubmit = onWatchAdForInquiry,
                onDismiss = onCloseAdDialog
            )
        }

        if (showOutOfChancesDialog) {
            OutOfChancesDialog(
                language = language,
                onWatchAdClick = {
                    showOutOfChancesDialog = false
                    onWatchAdForChances()
                },
                onGiveUpClick = {
                    showOutOfChancesDialog = false
                    showTruthDialog = true
                },
                onDismiss = { showOutOfChancesDialog = false }
            )
        }

        if (showTruthDialog && currentPuzzle != null) {
            BottomTruthDialog(
                title = currentPuzzle.title.get(language),
                bottomText = currentPuzzle.bottom.get(language),
                language = language,
                onDismiss = {
                    showTruthDialog = false
                    onGiveUpGame()
                }
            )
        }
    }
}

/**
 * 題庫關卡選擇卡片
 */
@Composable
fun PuzzleListItemCard(
    index: Int,
    puzzle: TurtleSoupPuzzle,
    record: TurtleSoupRecord?,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCleared = record != null
    val diffColor = when (puzzle.difficulty.uppercase()) {
        "EASY" -> MysteryThemeColors.YesGreen
        "MEDIUM" -> MysteryThemeColors.CoreClueGold
        else -> MysteryThemeColors.NoRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MysteryThemeColors.CardDark),
        border = BorderStroke(
            1.dp,
            if (isCleared) MysteryThemeColors.YesGreen.copy(alpha = 0.5f) else MysteryThemeColors.CardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 難度/序號圖示
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = diffColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, diffColor.copy(alpha = 0.6f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#${index + 1}",
                            color = diffColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = puzzle.title.get(language),
                            color = MysteryThemeColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x334B5563)
                        ) {
                            Text(
                                text = puzzle.category.get(language),
                                color = MysteryThemeColors.TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (record != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (i in 1..3) {
                                val isLit = i <= record.stars
                                Icon(
                                    imageVector = if (isLit) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (isLit) MysteryThemeColors.GoldAccent else MysteryThemeColors.TextSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${record.score} pts",
                                color = MysteryThemeColors.CoreClueGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = "${puzzle.questions.size} Questions",
                            color = MysteryThemeColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Enter",
                tint = MysteryThemeColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
