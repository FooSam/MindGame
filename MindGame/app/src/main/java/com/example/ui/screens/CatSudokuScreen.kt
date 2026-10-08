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
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.min

// 5 大經典次世代 3D 萌貓品種配色
enum class CatBreed(
    val baseColor: Color,
    val highlightColor: Color,
    val shadowColor: Color,
    val eyeColor: Color,
    val earInnerColor: Color = Color(0xFFFF8DA1),
    val stripeColor: Color? = null,
    val patchColor: Color? = null,
    val muzzleColor: Color = Color(0xFFFFF9E6)
) {
    ORANGE_TABBY(
        baseColor = Color(0xFFFFA726),
        highlightColor = Color(0xFFFFE082),
        shadowColor = Color(0xFFE65100),
        eyeColor = Color(0xFF43A047),
        stripeColor = Color(0xFFD84315)
    ),
    BLUE_GRAY(
        baseColor = Color(0xFF78909C),
        highlightColor = Color(0xFFB0BEC5),
        shadowColor = Color(0xFF37474F),
        eyeColor = Color(0xFFFFB300),
        muzzleColor = Color(0xFFECEFF1)
    ),
    PURE_WHITE(
        baseColor = Color(0xFFFAFAFA),
        highlightColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFFCFD8DC),
        eyeColor = Color(0xFF0288D1),
        muzzleColor = Color(0xFFFFFFFF)
    ),
    CALICO(
        baseColor = Color(0xFFFFF8E1),
        highlightColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFFFFE082),
        eyeColor = Color(0xFF66BB6A),
        stripeColor = Color(0xFFEF6C00),
        patchColor = Color(0xFF263238)
    ),
    SEAL_POINT(
        baseColor = Color(0xFFFFF3E0),
        highlightColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFFFFCC80),
        eyeColor = Color(0xFF1E88E5),
        stripeColor = Color(0xFF4E342E),
        muzzleColor = Color(0xFF3E2723)
    )
}

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
    freeHintsRemaining: Int = 1,
    showAdHintDialog: Boolean = false,
    onBackClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onUndoClick: () -> Unit,
    onResetBoardClick: () -> Unit,
    onHintClick: () -> Unit = {},
    onWatchAdForHint: () -> Unit = {},
    onCloseAdHintDialog: () -> Unit = {},
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
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(vertical = 10.dp, horizontal = 14.dp),
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
                            tint = if (currentCatsPlaced == totalCatsNeeded && !hasConflicts) Color(0xFF10B981) else Color(0xFFFFA000),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$currentCatsPlaced / $totalCatsNeeded",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (currentCatsPlaced == totalCatsNeeded && !hasConflicts) Color(0xFF10B981) else Color(0xFFE65100)
                            )
                        )
                    }
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
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo
            OutlinedButton(
                onClick = onUndoClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("cat_sudoku_undo_button"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = Localization.getString("undo", language),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Clear Board
            OutlinedButton(
                onClick = onResetBoardClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("cat_sudoku_clear_button"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = Localization.getString("clear_board", language),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Hint
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = onHintClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("cat_sudoku_hint_button"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (freeHintsRemaining > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (freeHintsRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = Localization.getString("hint", language),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (freeHintsRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-5).dp)
                ) {
                    Text(
                        text = if (freeHintsRemaining > 0) Localization.getString("hint_free_badge", language) else Localization.getString("hint_ad_badge", language),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }

            // New Game
            Button(
                onClick = onNewGameClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("cat_sudoku_new_game_button"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = Localization.getString("restart_game", language),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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

    if (showAdHintDialog) {
        AlertDialog(
            onDismissRequest = onCloseAdHintDialog,
            title = {
                Text(
                    text = Localization.getString("hint_ad_confirm_title", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = Localization.getString("hint_ad_confirm_desc", language))
            },
            confirmButton = {
                Button(onClick = onWatchAdForHint) {
                    Text(text = Localization.getString("watch_ad_button", language))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onCloseAdHintDialog) {
                    Text(text = Localization.getString("cancel", language))
                }
            }
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
    // 精雕黑胡桃木立體邊框底座
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x66000000),
                spotColor = Color(0x88000000)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2C1E17), Color(0xFF1E1510), Color(0xFF140D0A)),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 600f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD54F), Color(0xFF8D6E63), Color(0xFF5D4037)),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 400f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
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
                                cellIndex = index,
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

            val thinLineWidth = 1.2f
            val thickLineWidth = 3.8f
            val thinLineColor = Color(0x33000000)
            val thickLineShadow = Color(0xAA111827)
            val thickLineGold = Color(0xEEF59E0B)

            // 1. 繪製所有格子間的細微分割線
            for (i in 1 until boardSize) {
                val x = i * cellWidth
                val y = i * cellHeight
                drawLine(
                    color = thinLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = thinLineWidth
                )
                drawLine(
                    color = thinLineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = thinLineWidth
                )
            }

            // 2. 疊加繪製不同色塊區域之間的立體加粗邊界線（深色陰影線 + 金屬高光線）
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
                            // 陰影線
                            drawLine(
                                color = thickLineShadow,
                                start = Offset(x + 1f, r * cellHeight),
                                end = Offset(x + 1f, (r + 1) * cellHeight),
                                strokeWidth = thickLineWidth
                            )
                            // 金屬高光主線
                            drawLine(
                                color = thickLineGold,
                                start = Offset(x, r * cellHeight),
                                end = Offset(x, (r + 1) * cellHeight),
                                strokeWidth = thickLineWidth * 0.8f
                            )
                        }
                    }

                    // 檢查下方鄰居
                    if (r < boardSize - 1) {
                        val bottomIdx = (r + 1) * boardSize + c
                        val bottomReg = gridCells.getOrNull(bottomIdx)?.regionId
                        if (bottomReg != null && bottomReg != currentReg) {
                            val y = (r + 1) * cellHeight
                            // 陰影線
                            drawLine(
                                color = thickLineShadow,
                                start = Offset(c * cellWidth, y + 1f),
                                end = Offset((c + 1) * cellWidth, y + 1f),
                                strokeWidth = thickLineWidth
                            )
                            // 金屬高光主線
                            drawLine(
                                color = thickLineGold,
                                start = Offset(c * cellWidth, y),
                                end = Offset((c + 1) * cellWidth, y),
                                strokeWidth = thickLineWidth * 0.8f
                            )
                        }
                    }
                }
            }
        }
    }
}

// 次世代微倒角大理石/瓷磚單元格底板繪製
fun DrawScope.drawMarbleBeveledCell(
    size: Size,
    baseColor: Color,
    isConflict: Boolean
) {
    val w = size.width
    val h = size.height

    // 1. 底板柔和漸層（左上微亮受光，右下微暗環境陰影，營造立體微凸瓷板感）
    drawRect(
        brush = Brush.linearGradient(
            colors = if (isConflict) {
                listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFEF9A9A))
            } else {
                listOf(
                    baseColor.copy(alpha = 0.96f),
                    baseColor,
                    Color(
                        red = (baseColor.red * 0.90f).coerceIn(0f, 1f),
                        green = (baseColor.green * 0.90f).coerceIn(0f, 1f),
                        blue = (baseColor.blue * 0.90f).coerceIn(0f, 1f),
                        alpha = baseColor.alpha
                    )
                )
            },
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        size = size
    )

    // 2. 頂部與左側 1.5px 晶透微高光線 (Highlight Edge)
    drawLine(
        color = Color(0x66FFFFFF),
        start = Offset(0f, 0.75f),
        end = Offset(w, 0.75f),
        strokeWidth = 1.5f
    )
    drawLine(
        color = Color(0x66FFFFFF),
        start = Offset(0.75f, 0f),
        end = Offset(0.75f, h),
        strokeWidth = 1.5f
    )

    // 3. 底部與右側 1.5px 柔和微陰影線 (Shadow Edge)
    drawLine(
        color = Color(0x22000000),
        start = Offset(0f, h - 0.75f),
        end = Offset(w, h - 0.75f),
        strokeWidth = 1.5f
    )
    drawLine(
        color = Color(0x22000000),
        start = Offset(w - 0.75f, 0f),
        end = Offset(w - 0.75f, h),
        strokeWidth = 1.5f
    )

    // 4. 若衝突，疊加醒目立體警示外框
    if (isConflict) {
        drawRect(
            color = Color(0xFFE53935),
            size = size,
            style = Stroke(width = 3.5f)
        )
    }
}

// 3D 浮雕叉叉徽章 (取代早期純文字 ✖)
fun DrawScope.drawCrossBadge(size: Size) {
    val w = size.width
    val h = size.height
    val minDim = min(w, h)
    val centerX = w * 0.5f
    val centerY = h * 0.5f
    val crossSize = minDim * 0.26f
    val strokeW = minDim * 0.08f

    // 1. 微凹下沉內陰影 (Soft Drop Shadow)
    val shadowOffset = minDim * 0.02f
    drawLine(
        color = Color(0x33000000),
        start = Offset(centerX - crossSize + shadowOffset, centerY - crossSize + shadowOffset),
        end = Offset(centerX + crossSize + shadowOffset, centerY + crossSize + shadowOffset),
        strokeWidth = strokeW,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0x33000000),
        start = Offset(centerX + crossSize + shadowOffset, centerY - crossSize + shadowOffset),
        end = Offset(centerX - crossSize + shadowOffset, centerY + crossSize + shadowOffset),
        strokeWidth = strokeW,
        cap = StrokeCap.Round
    )

    // 2. 主體高級金屬消光鈦灰線條
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF90A4AE), Color(0xFF607D8B), Color(0xFF455A64)),
            start = Offset(centerX - crossSize, centerY - crossSize),
            end = Offset(centerX + crossSize, centerY + crossSize)
        ),
        start = Offset(centerX - crossSize, centerY - crossSize),
        end = Offset(centerX + crossSize, centerY + crossSize),
        strokeWidth = strokeW,
        cap = StrokeCap.Round
    )
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF90A4AE), Color(0xFF607D8B), Color(0xFF455A64)),
            start = Offset(centerX + crossSize, centerY - crossSize),
            end = Offset(centerX - crossSize, centerY + crossSize)
        ),
        start = Offset(centerX + crossSize, centerY - crossSize),
        end = Offset(centerX - crossSize, centerY + crossSize),
        strokeWidth = strokeW,
        cap = StrokeCap.Round
    )

    // 3. 上緣細緻高光光絲
    drawLine(
        color = Color(0x88FFFFFF),
        start = Offset(centerX - crossSize * 0.65f, centerY - crossSize * 0.65f),
        end = Offset(centerX + crossSize * 0.65f, centerY + crossSize * 0.65f),
        strokeWidth = strokeW * 0.28f,
        cap = StrokeCap.Round
    )
}

// 2024-2026 次世代 Stylized 3D 萌貓公仔向量光影繪圖 (取代早期 Emoji 🐱)
fun DrawScope.drawStylized3DCat(
    size: Size,
    breed: CatBreed,
    isConflict: Boolean
) {
    val w = size.width
    val h = size.height
    val minDim = min(w, h)
    if (minDim <= 0f) return

    val centerX = w * 0.5f
    val centerY = h * 0.54f

    // 1. 接地立體柔和接觸陰影 (AO Contact Shadow)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x771A1A24),
                Color(0x261A1A24),
                Color.Transparent
            ),
            center = Offset(centerX, h * 0.90f),
            radius = minDim * 0.42f
        ),
        topLeft = Offset(centerX - minDim * 0.38f, h * 0.82f),
        size = Size(minDim * 0.76f, minDim * 0.16f)
    )

    // 2. 立體圓潤耳朵 (Ears with SSS & Rim Light)
    val earOuterPath = Path()
    val earInnerLeftPath = Path()
    val earInnerRightPath = Path()

    // 左耳
    val earLeftTop = Offset(w * 0.22f, h * 0.14f)
    val earLeftOuter = Offset(w * 0.14f, h * 0.42f)
    val earLeftInner = Offset(w * 0.38f, h * 0.28f)
    earOuterPath.moveTo(earLeftOuter.x, earLeftOuter.y)
    earOuterPath.lineTo(earLeftTop.x, earLeftTop.y)
    earOuterPath.lineTo(earLeftInner.x, earLeftInner.y)
    earOuterPath.close()

    // 右耳
    val earRightTop = Offset(w * 0.78f, h * 0.14f)
    val earRightOuter = Offset(w * 0.86f, h * 0.42f)
    val earRightInner = Offset(w * 0.62f, h * 0.28f)
    earOuterPath.moveTo(earRightOuter.x, earRightOuter.y)
    earOuterPath.lineTo(earRightTop.x, earRightTop.y)
    earOuterPath.lineTo(earRightInner.x, earRightInner.y)
    earOuterPath.close()

    // 繪製耳朵外層立體光影
    drawPath(
        path = earOuterPath,
        brush = Brush.linearGradient(
            colors = listOf(breed.highlightColor, breed.baseColor, breed.shadowColor),
            start = Offset(centerX, h * 0.10f),
            end = Offset(centerX, h * 0.45f)
        )
    )

    // 繪製內耳粉嫩次表面散射 (SSS Inner Ear Gradient)
    val innerLeftTop = Offset(w * 0.23f, h * 0.18f)
    val innerLeftOuter = Offset(w * 0.18f, h * 0.38f)
    val innerLeftInner = Offset(w * 0.35f, h * 0.28f)
    earInnerLeftPath.moveTo(innerLeftOuter.x, innerLeftOuter.y)
    earInnerLeftPath.lineTo(innerLeftTop.x, innerLeftTop.y)
    earInnerLeftPath.lineTo(innerLeftInner.x, innerLeftInner.y)
    earInnerLeftPath.close()

    val innerRightTop = Offset(w * 0.77f, h * 0.18f)
    val innerRightOuter = Offset(w * 0.82f, h * 0.38f)
    val innerRightInner = Offset(w * 0.65f, h * 0.28f)
    earInnerRightPath.moveTo(innerRightOuter.x, innerRightOuter.y)
    earInnerRightPath.lineTo(innerRightTop.x, innerRightTop.y)
    earInnerRightPath.lineTo(innerRightInner.x, innerRightInner.y)
    earInnerRightPath.close()

    drawPath(
        path = earInnerLeftPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFF8DA1), Color(0xFFFFB2C1)),
            start = Offset(innerLeftOuter.x, innerLeftOuter.y),
            end = Offset(innerLeftTop.x, innerLeftTop.y)
        )
    )
    drawPath(
        path = earInnerRightPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFF8DA1), Color(0xFFFFB2C1)),
            start = Offset(innerRightOuter.x, innerRightOuter.y),
            end = Offset(innerRightTop.x, innerRightTop.y)
        )
    )

    // 3. 飽滿身軀大圓 (3D Sphere Head/Body with Specular Highlight)
    val headRadius = minDim * 0.37f
    val headLightCenter = Offset(w * 0.40f, h * 0.42f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                breed.highlightColor,
                breed.baseColor,
                breed.shadowColor
            ),
            center = headLightCenter,
            radius = headRadius * 1.35f
        ),
        radius = headRadius,
        center = Offset(centerX, centerY)
    )

    // 4. 花紋或面具 (Stripes / Patches / Points)
    if (breed.stripeColor != null) {
        val stripePaint = breed.stripeColor
        // 額頭立體微弧斑紋
        drawLine(
            color = stripePaint,
            start = Offset(centerX, h * 0.26f),
            end = Offset(centerX, h * 0.36f),
            strokeWidth = minDim * 0.045f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = stripePaint,
            start = Offset(centerX - minDim * 0.08f, h * 0.28f),
            end = Offset(centerX - minDim * 0.06f, h * 0.35f),
            strokeWidth = minDim * 0.035f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = stripePaint,
            start = Offset(centerX + minDim * 0.08f, h * 0.28f),
            end = Offset(centerX + minDim * 0.06f, h * 0.35f),
            strokeWidth = minDim * 0.035f,
            cap = StrokeCap.Round
        )
    }

    if (breed.patchColor != null) {
        // 三花左側俏皮斑紋
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(breed.patchColor, breed.patchColor.copy(alpha = 0.85f)),
                center = Offset(w * 0.72f, h * 0.35f),
                radius = minDim * 0.16f
            ),
            topLeft = Offset(w * 0.60f, h * 0.24f),
            size = Size(minDim * 0.25f, minDim * 0.22f)
        )
    }

    if (breed == CatBreed.SEAL_POINT) {
        // 布偶/暹羅眼部深邃面具
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF4E342E), Color(0x994E342E), Color.Transparent),
                center = Offset(centerX, h * 0.54f),
                radius = minDim * 0.26f
            ),
            topLeft = Offset(centerX - minDim * 0.25f, h * 0.42f),
            size = Size(minDim * 0.50f, minDim * 0.28f)
        )
    }

    // 5. 兩頰粉嫩微醺腮紅 (Cheek Blush Glow)
    val blushRadius = minDim * 0.085f
    val blushLeft = Offset(w * 0.24f, h * 0.58f)
    val blushRight = Offset(w * 0.76f, h * 0.58f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x88FF80AB), Color.Transparent),
            center = blushLeft,
            radius = blushRadius
        ),
        radius = blushRadius,
        center = blushLeft
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x88FF80AB), Color.Transparent),
            center = blushRight,
            radius = blushRadius
        ),
        radius = blushRadius,
        center = blushRight
    )

    // 6. 晶潤靈動大眼睛 (Glistening Jewel Anime Eyes)
    val eyeRadius = minDim * 0.082f
    val eyeLeftCenter = Offset(w * 0.35f, h * 0.51f)
    val eyeRightCenter = Offset(w * 0.65f, h * 0.51f)

    listOf(eyeLeftCenter, eyeRightCenter).forEach { eyeCenter ->
        // 眼眶微立體外陰影
        drawCircle(
            color = Color(0xFF212121),
            radius = eyeRadius,
            center = eyeCenter
        )
        // 雙層寶石虹膜漸層
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    breed.eyeColor,
                    Color(0xFF1B262C)
                ),
                center = Offset(eyeCenter.x - eyeRadius * 0.2f, eyeCenter.y - eyeRadius * 0.2f),
                radius = eyeRadius
            ),
            radius = eyeRadius * 0.88f,
            center = eyeCenter
        )
        // 瞳孔深黑
        drawCircle(
            color = Color(0xFF0F172A),
            radius = eyeRadius * 0.45f,
            center = eyeCenter
        )
        // 雙星芒靈動高光 (Primary Specular Star & Secondary Dot)
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.32f,
            center = Offset(eyeCenter.x - eyeRadius * 0.28f, eyeCenter.y - eyeRadius * 0.30f)
        )
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.16f,
            center = Offset(eyeCenter.x + eyeRadius * 0.25f, eyeCenter.y + eyeRadius * 0.22f)
        )
    }

    // 7. 粉嫩立體小鼻與萌萌「ω」貓嘴
    val noseCenter = Offset(centerX, h * 0.60f)
    val nosePath = Path().apply {
        moveTo(noseCenter.x - minDim * 0.035f, noseCenter.y - minDim * 0.015f)
        lineTo(noseCenter.x + minDim * 0.035f, noseCenter.y - minDim * 0.015f)
        lineTo(noseCenter.x, noseCenter.y + minDim * 0.022f)
        close()
    }
    drawPath(path = nosePath, color = Color(0xFFFF8DA1))

    // 萌貓嘴線「ω」
    val mouthPath = Path().apply {
        moveTo(centerX, noseCenter.y + minDim * 0.022f)
        lineTo(centerX, noseCenter.y + minDim * 0.045f)
        quadraticTo(
            centerX - minDim * 0.04f, noseCenter.y + minDim * 0.075f,
            centerX - minDim * 0.07f, noseCenter.y + minDim * 0.05f
        )
        moveTo(centerX, noseCenter.y + minDim * 0.045f)
        quadraticTo(
            centerX + minDim * 0.04f, noseCenter.y + minDim * 0.075f,
            centerX + minDim * 0.07f, noseCenter.y + minDim * 0.05f
        )
    }
    drawPath(
        path = mouthPath,
        color = Color(0xFF5D4037),
        style = Stroke(width = minDim * 0.022f, cap = StrokeCap.Round)
    )

    // 8. 鍍金項圈與金屬小圓鈴鐺 (Golden Bell Collar)
    val collarY = h * 0.77f
    drawOval(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFFD32F2F), Color(0xFFFF5252), Color(0xFFC62828))
        ),
        topLeft = Offset(centerX - minDim * 0.24f, collarY - minDim * 0.035f),
        size = Size(minDim * 0.48f, minDim * 0.07f)
    )

    // 黃金小鈴鐺
    val bellRadius = minDim * 0.055f
    val bellCenter = Offset(centerX, collarY + minDim * 0.025f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFFF8F00)),
            center = Offset(bellCenter.x - bellRadius * 0.3f, bellCenter.y - bellRadius * 0.3f),
            radius = bellRadius * 1.2f
        ),
        radius = bellRadius,
        center = bellCenter
    )
    drawCircle(
        color = Color(0xFF424242),
        radius = bellRadius * 0.22f,
        center = Offset(bellCenter.x, bellCenter.y + bellRadius * 0.25f)
    )

    // 9. 衝突警示特效（若有衝突）
    if (isConflict) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x66FF1744), Color(0xCCFF1744)),
                center = Offset(centerX, centerY),
                radius = headRadius * 1.25f
            ),
            radius = headRadius * 1.25f,
            center = Offset(centerX, centerY),
            style = Stroke(width = minDim * 0.05f)
        )
    }
}

@Composable
fun CatCellView(
    cell: CatCell,
    cellIndex: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val palette = if (isDark) RegionColorsDark else RegionColors
    val baseRegionColor = palette[cell.regionId % palette.size]
    val breed = CatBreed.values()[cellIndex % CatBreed.values().size]

    Box(
        modifier = modifier
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. 繪製微倒角大理石/瓷磚底板
            drawMarbleBeveledCell(
                size = size,
                baseColor = baseRegionColor,
                isConflict = cell.isConflict
            )

            // 2. 依狀態繪製次世代 3D 萌貓公仔或立體叉叉徽章
            when (cell.state) {
                CatCellState.CAT -> {
                    drawStylized3DCat(
                        size = size,
                        breed = breed,
                        isConflict = cell.isConflict
                    )
                }
                CatCellState.CROSS -> {
                    drawCrossBadge(size = size)
                }
                CatCellState.EMPTY -> {
                    // 空白格維持大理石微倒角質感
                }
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
