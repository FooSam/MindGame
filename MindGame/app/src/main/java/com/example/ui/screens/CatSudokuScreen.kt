package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.game.catsudoku.CatCell
import com.example.game.catsudoku.CatCellState
import com.example.ui.components.formatTimeMillis
import com.example.ui.viewmodel.GameStatus

// 9 種柔和高辨識度的色塊調色盤
val RegionColors = listOf(
    Color(0xFFFFD1DC), // 柔粉紅
    Color(0xFFBEE1E6), // 冰藍
    Color(0xFFDFE7FD), // 薰衣草紫
    Color(0xFFCDDAFD), // 淺長春花藍
    Color(0xFFE2ECE9), // 鼠尾草綠
    Color(0xFFFFF1C5), // 暖奶油黃
    Color(0xFFFFDFD3), // 蜜桃粉
    Color(0xFFD0F4DE), // 薄荷綠
    Color(0xFFFDE2E4)  // 玫瑰粉
)

val RegionColorsDark = listOf(
    Color(0xFF5C3342),
    Color(0xFF2C4A52),
    Color(0xFF383B5E),
    Color(0xFF323B5C),
    Color(0xFF2E4D43),
    Color(0xFF5E502B),
    Color(0xFF5C3D34),
    Color(0xFF2B523B),
    Color(0xFF543444)
)

@Composable
fun CatSudokuScreen(
    difficulty: GameDifficulty,
    language: AppLanguage,
    status: GameStatus,
    elapsedTimeMillis: Long,
    wrongCount: Int,
    boardSize: Int,
    gridCells: List<CatCell>,
    onBackClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onUndoClick: () -> Unit,
    onResetBoardClick: () -> Unit,
    onNewGameClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    var showRuleDialog by remember { mutableStateOf(false) }

    val totalCatsNeeded = boardSize
    val currentCatsPlaced = gridCells.count { it.state == CatCellState.CAT }
    val hasConflicts = gridCells.any { it.isConflict }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("cat_sudoku_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = Localization.getString("game_cat_sudoku", language),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${boardSize}x${boardSize} (${totalCatsNeeded}隻貓)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showRuleDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Rules",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onLeaderboardClick,
                    modifier = Modifier.testTag("cat_sudoku_leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Top Status Dashboard (Timer, Cat Counter, Mistakes)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.getString("timer_label", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Text(
                        text = formatTimeMillis(elapsedTimeMillis),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Cats Count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = if (currentCatsPlaced == totalCatsNeeded && !hasConflicts) Color(0xFF10B981) else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.getString("cats_placed_label", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Text(
                        text = "$currentCatsPlaced / $totalCatsNeeded 🐱",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (currentCatsPlaced == totalCatsNeeded && !hasConflicts) Color(0xFF10B981) else MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                // Mistakes Count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (wrongCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.getString("mistakes_label", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Text(
                        text = "$wrongCount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (wrongCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interaction Hint
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Text(
                text = Localization.getString("cat_control_hint", language),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cat Sudoku Board Grid
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardDim = maxWidth
            CatSudokuBoard(
                boardSize = boardSize,
                gridCells = gridCells,
                onCellClick = onCellClick,
                modifier = Modifier
                    .size(boardDim)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 2.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(14.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo
            OutlinedButton(
                onClick = onUndoClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("cat_sudoku_undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.getString("undo", language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Clear Board
            OutlinedButton(
                onClick = onResetBoardClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("cat_sudoku_clear_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.getString("clear_board", language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // New Game
            Button(
                onClick = onNewGameClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("cat_sudoku_new_game_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.getString("restart_game", language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Rules Dialog
    if (showRuleDialog) {
        Dialog(
            onDismissRequest = { showRuleDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.getString("check_rules", language),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    RuleRowItem(number = "1", text = Localization.getString("cat_rule_row_col", language))
                    Spacer(modifier = Modifier.height(10.dp))
                    RuleRowItem(number = "2", text = Localization.getString("cat_rule_region", language))
                    Spacer(modifier = Modifier.height(10.dp))
                    RuleRowItem(number = "3", text = Localization.getString("cat_rule_adjacent", language))

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = Localization.getString("cat_control_hint", language),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showRuleDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = Localization.getString("confirm", language))
                    }
                }
            }
        }
    }

    // Completed Win Dialog
    if (status == GameStatus.COMPLETED) {
        CatSudokuWinDialog(
            elapsedTimeMillis = elapsedTimeMillis,
            wrongCount = wrongCount,
            language = language,
            onPlayAgain = onNewGameClick,
            onLeaderboard = onLeaderboardClick,
            onBackToMenu = onBackClick
        )
    }
}

@Composable
fun RuleRowItem(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun CatSudokuBoard(
    boardSize: Int,
    gridCells: List<CatCell>,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Grid cells
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until boardSize) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (col in 0 until boardSize) {
                        val index = row * boardSize + col
                        val cell = gridCells.getOrNull(index)
                        if (cell != null) {
                            CatCellView(
                                cell = cell,
                                onClick = { onCellClick(index) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .testTag("cat_cell_${row}_${col}")
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Region boundary canvas for clear grid lines and distinct colored region borders
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / boardSize
            val cellHeight = size.height / boardSize

            val thinLineWidth = 1.5.dp.toPx()
            val thickLineWidth = 3.5.dp.toPx()
            val thinLineColor = Color(0x55333333) // 清楚分明的格子細線（適中不刺眼）
            val thickLineColor = Color(0xFF1E293B) // 區域分割加粗線

            // 1. 繪製所有格子間的清晰基準網格線
            for (i in 1 until boardSize) {
                val x = i * cellWidth
                val y = i * cellHeight
                // 垂直網格線
                drawLine(
                    color = thinLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = thinLineWidth
                )
                // 水平網格線
                drawLine(
                    color = thinLineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = thinLineWidth
                )
            }

            // 2. 疊加繪製不同色塊區域之間的加粗邊界線
            for (r in 0 until boardSize) {
                for (c in 0 until boardSize) {
                    val idx = r * boardSize + c
                    val currentReg = gridCells.getOrNull(idx)?.regionId ?: continue

                    // 檢查右側鄰居
                    if (c < boardSize - 1) {
                        val rightIdx = r * boardSize + (c + 1)
                        val rightReg = gridCells.getOrNull(rightIdx)?.regionId
                        if (rightReg != null && rightReg != currentReg) {
                            val x = (c + 1) * cellWidth
                            drawLine(
                                color = thickLineColor,
                                start = Offset(x, r * cellHeight),
                                end = Offset(x, (r + 1) * cellHeight),
                                strokeWidth = thickLineWidth
                            )
                        }
                    }

                    // 檢查下方鄰居
                    if (r < boardSize - 1) {
                        val bottomIdx = (r + 1) * boardSize + c
                        val bottomReg = gridCells.getOrNull(bottomIdx)?.regionId
                        if (bottomReg != null && bottomReg != currentReg) {
                            val y = (r + 1) * cellHeight
                            drawLine(
                                color = thickLineColor,
                                start = Offset(c * cellWidth, y),
                                end = Offset((c + 1) * cellWidth, y),
                                strokeWidth = thickLineWidth
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CatCellView(
    cell: CatCell,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val palette = if (isDark) RegionColorsDark else RegionColors
    val baseRegionColor = palette[cell.regionId % palette.size]

    val cellBackgroundColor by animateColorAsState(
        targetValue = if (cell.isConflict) {
            Color(0xFFFFCDD2) // 衝突紅色警示底色
        } else {
            baseRegionColor
        },
        animationSpec = tween(durationMillis = 200),
        label = "cell_color"
    )

    Box(
        modifier = modifier
            .background(cellBackgroundColor)
            .clickable(onClick = onClick)
            .then(
                if (cell.isConflict) {
                    Modifier.border(2.dp, Color(0xFFE53935))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (cell.state) {
            CatCellState.CROSS -> {
                Text(
                    text = "✖",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757575)
                    ),
                    fontSize = 18.sp
                )
            }
            CatCellState.CAT -> {
                Text(
                    text = "🐱",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
            CatCellState.EMPTY -> {
                // Empty cell
            }
        }
    }
}

@Composable
fun CatSudokuWinDialog(
    elapsedTimeMillis: Long,
    wrongCount: Int,
    language: AppLanguage,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Dialog(
        onDismissRequest = onPlayAgain,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 🐱 🎉",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Localization.getString("cat_sudoku_completed", language),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Surface
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.getString("your_time", language),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = formatTimeMillis(elapsedTimeMillis),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.getString("mistakes_label", language),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "$wrongCount 次",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (wrongCount > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Localization.getString("score_saved", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = Localization.getString("play_again", language),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onLeaderboard,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = Localization.getString("leaderboard_button", language),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onBackToMenu,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Localization.getString("back_to_menu", language),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
