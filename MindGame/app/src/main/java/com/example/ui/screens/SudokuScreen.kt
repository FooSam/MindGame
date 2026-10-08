package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.ui.viewmodel.GameStatus
import com.example.game.sudoku.SudokuConfig
import kotlin.math.sqrt

@Composable
fun SudokuScreen(
    difficulty: GameDifficulty,
    language: AppLanguage,
    status: GameStatus,
    elapsedTimeMillis: Long,
    wrongCount: Int,
    initialBoard: List<String>,
    solutionBoard: List<String>,
    playerBoard: List<String>,
    notesBoard: Map<Int, Set<String>>,
    config: SudokuConfig,
    selectedCellIndex: Int?,
    isPencilMode: Boolean,
    freeHintsRemaining: Int = 1,
    showAdHintDialog: Boolean = false,
    onBackClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onSymbolInput: (String) -> Unit,
    onEraseClick: () -> Unit,
    onUndoClick: () -> Unit,
    onTogglePencilClick: () -> Unit,
    onHintClick: () -> Unit = {},
    onWatchAdForHint: () -> Unit = {},
    onCloseAdHintDialog: () -> Unit = {},
    onResetClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = Localization.getString("game_sudoku", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row {
                IconButton(onClick = onResetClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onLeaderboardClick) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Leaderboard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stats Header Card (Timer & Wrong Count & Difficulty Badge)
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTimeMillis(elapsedTimeMillis),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Difficulty Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.border(
                        1.dp,
                        Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFB8860B))),
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    val diffTitle = when (difficulty) {
                        GameDifficulty.BEGINNER -> Localization.getString("diff_name_beginner", language)
                        GameDifficulty.INTERMEDIATE -> Localization.getString("diff_name_intermediate", language)
                        GameDifficulty.ADVANCED -> Localization.getString("diff_name_advanced", language)
                        GameDifficulty.HARD -> Localization.getString("diff_name_hard", language)
                        GameDifficulty.HELL -> Localization.getString("diff_name_hell", language)
                        GameDifficulty.EPIC -> Localization.getString("diff_name_epic", language)
                    }
                    Text(
                        text = "$diffTitle (${config.gridSize}x${config.gridSize})",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Mistakes Counter
                Text(
                    text = "${Localization.getString("mistakes_label", language)}: $wrongCount",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (wrongCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sudoku Board View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            SudokuGridView(
                gridSize = config.gridSize,
                subRows = config.subRows,
                subCols = config.subCols,
                symbols = config.symbols,
                initialBoard = initialBoard,
                playerBoard = playerBoard,
                notesBoard = notesBoard,
                selectedCellIndex = selectedCellIndex,
                onCellClick = onCellClick
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Toolbar (Undo, Erase, Pencil Mode Toggle, Hint)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onUndoClick() }
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(3.dp, CircleShape)
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C))),
                            CircleShape
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Localization.getString("undo", language),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Erase
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onEraseClick() }
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(3.dp, CircleShape)
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C))),
                            CircleShape
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Erase",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Localization.getString("erase", language),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Pencil Mode Toggle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTogglePencilClick() }
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isPencilMode) Color(0xFFFFA000) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(if (isPencilMode) 6.dp else 3.dp, CircleShape)
                        .border(
                            1.5.dp,
                            if (isPencilMode) Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00)))
                            else Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C))),
                            CircleShape
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Pencil Mode",
                            tint = if (isPencilMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPencilMode) Localization.getString("pencil_mode_on", language) else Localization.getString("pencil_mode_off", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isPencilMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPencilMode) Color(0xFFFFA000) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Hint
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onHintClick() }
            ) {
                Box {
                    Surface(
                        shape = CircleShape,
                        color = if (freeHintsRemaining > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(3.dp, CircleShape)
                            .border(
                                1.dp,
                                if (freeHintsRemaining > 0) Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFA000)))
                                else Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C))),
                                CircleShape
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Hint",
                                tint = if (freeHintsRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (freeHintsRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                    ) {
                        Text(
                            text = if (freeHintsRemaining > 0) Localization.getString("hint_free_badge", language) else Localization.getString("hint_ad_badge", language),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Localization.getString("hint", language),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keypad Inputs
        SudokuKeypad(
            symbols = config.symbols,
            onSymbolInput = onSymbolInput
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Completion Dialog
    if (status == GameStatus.COMPLETED) {
        AlertDialog(
            onDismissRequest = { },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "🎉 ${Localization.getString("stage_completed", language)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Button(
                        onClick = onResetClick,
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
                        onClick = onLeaderboardClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = Localization.getString("leaderboard_title", language),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onBackClick,
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
            },
            confirmButton = { },
            dismissButton = { }
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

// 象牙白微倒角瓷板單元格底板繪製 (取代扁平色塊)
fun DrawScope.drawPorcelainCell(
    size: Size,
    isSelected: Boolean,
    isSameValue: Boolean,
    isSameRowOrCol: Boolean,
    isInitial: Boolean,
    isDarkTheme: Boolean
) {
    val w = size.width
    val h = size.height

    // 1. 底板基礎漸層 (Base Porcelain Gradient)
    val baseColors = when {
        isSelected -> {
            listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2), Color(0xFF80DEEA))
        }
        isSameValue -> {
            listOf(Color(0xFFFFF9C4), Color(0xFFFFECB3), Color(0xFFFFE082))
        }
        isSameRowOrCol -> {
            if (isDarkTheme) {
                listOf(Color(0xFF263238), Color(0xFF1E272C))
            } else {
                listOf(Color(0xFFF0F7F9), Color(0xFFE4EFF2))
            }
        }
        isInitial -> {
            if (isDarkTheme) {
                listOf(Color(0xFF2C2825), Color(0xFF221E1C))
            } else {
                listOf(Color(0xFFFAF7F0), Color(0xFFF2ECE1))
            }
        }
        else -> {
            if (isDarkTheme) {
                listOf(Color(0xFF1F1E24), Color(0xFF18171D))
            } else {
                listOf(Color(0xFFFFFFFF), Color(0xFFFAF8F5))
            }
        }
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = baseColors,
            startY = 0f,
            endY = h
        ),
        size = size
    )

    // 2. 瓷板倒角微高光與陰影 (1.5px Bevel Edges)
    // 左、上晶透微高光線
    drawLine(
        color = if (isSelected) Color(0xCC00E5FF) else if (isDarkTheme) Color(0x22FFFFFF) else Color(0x88FFFFFF),
        start = Offset(0f, 0.75f),
        end = Offset(w, 0.75f),
        strokeWidth = if (isSelected) 2.2f else 1.2f
    )
    drawLine(
        color = if (isSelected) Color(0xCC00E5FF) else if (isDarkTheme) Color(0x22FFFFFF) else Color(0x88FFFFFF),
        start = Offset(0.75f, 0f),
        end = Offset(0.75f, h),
        strokeWidth = if (isSelected) 2.2f else 1.2f
    )

    // 右、下環境陰影線
    drawLine(
        color = if (isSelected) Color(0x660097A7) else Color(0x22000000),
        start = Offset(0f, h - 0.75f),
        end = Offset(w, h - 0.75f),
        strokeWidth = 1.2f
    )
    drawLine(
        color = if (isSelected) Color(0x660097A7) else Color(0x22000000),
        start = Offset(w - 0.75f, 0f),
        end = Offset(w - 0.75f, h),
        strokeWidth = 1.2f
    )

    // 3. 選中聚光燈聚焦微光環 (Spotlight Ring)
    if (isSelected) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3300E5FF), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.65f
            ),
            size = size
        )
        drawRect(
            color = Color(0xFF00BCD4),
            size = size,
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
fun SudokuGridView(
    gridSize: Int,
    subRows: Int,
    subCols: Int,
    symbols: List<String>,
    initialBoard: List<String>,
    playerBoard: List<String>,
    notesBoard: Map<Int, Set<String>>,
    selectedCellIndex: Int?,
    onCellClick: (Int) -> Unit
) {
    // 精雕黑胡桃木與金屬立體底座護框
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
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
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until gridSize) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (c in 0 until gridSize) {
                        val index = r * gridSize + c
                        val isInitial = initialBoard.getOrNull(index)?.isNotEmpty() == true
                        val cellVal = playerBoard.getOrNull(index) ?: ""
                        val cellNotes = notesBoard[index] ?: emptySet()
                        val isSelected = selectedCellIndex == index

                        val isSameRowOrCol = selectedCellIndex != null && (
                                (selectedCellIndex / gridSize == r) || (selectedCellIndex % gridSize == c)
                                )
                        val isSameValue = selectedCellIndex != null && cellVal.isNotEmpty() &&
                                playerBoard.getOrNull(selectedCellIndex) == cellVal

                        SudokuCellView(
                            cellVal = cellVal,
                            cellNotes = cellNotes,
                            symbols = symbols,
                            gridSize = gridSize,
                            isInitial = isInitial,
                            isSelected = isSelected,
                            isSameValue = isSameValue,
                            isSameRowOrCol = isSameRowOrCol,
                            onClick = { onCellClick(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }

        // 疊加繪製 3x3 宮位之間的立體雙線（深色陰影線 + 拋光黃金線）
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / gridSize
            val cellH = size.height / gridSize

            // 宮位分割垂直線
            for (c in 1 until gridSize) {
                if (c % subCols == 0) {
                    val x = c * cellW
                    // 陰影線
                    drawLine(
                        color = Color(0xAA111827),
                        start = Offset(x + 1f, 0f),
                        end = Offset(x + 1f, size.height),
                        strokeWidth = 3.2f
                    )
                    // 金屬高光線
                    drawLine(
                        color = Color(0xEEF59E0B),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 2.2f
                    )
                }
            }

            // 宮位分割水平線
            for (r in 1 until gridSize) {
                if (r % subRows == 0) {
                    val y = r * cellH
                    // 陰影線
                    drawLine(
                        color = Color(0xAA111827),
                        start = Offset(0f, y + 1f),
                        end = Offset(size.width, y + 1f),
                        strokeWidth = 3.2f
                    )
                    // 金屬高光線
                    drawLine(
                        color = Color(0xEEF59E0B),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.2f
                    )
                }
            }
        }
    }
}

@Composable
fun SudokuCellView(
    cellVal: String,
    cellNotes: Set<String>,
    symbols: List<String>,
    gridSize: Int,
    isInitial: Boolean,
    isSelected: Boolean,
    isSameValue: Boolean,
    isSameRowOrCol: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 1. 繪製微倒角瓷板單元格底板
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPorcelainCell(
                size = size,
                isSelected = isSelected,
                isSameValue = isSameValue,
                isSameRowOrCol = isSameRowOrCol,
                isInitial = isInitial,
                isDarkTheme = isDark
            )
        }

        // 2. 繪製 3D 浮雕燙金（題目初始）或璀璨藍寶石（玩家填入）數字
        if (cellVal.isNotEmpty()) {
            val fontSp = when (gridSize) {
                4 -> 26.sp
                6 -> 22.sp
                9 -> 18.sp
                12 -> 14.sp
                else -> 16.sp
            }

            if (isInitial) {
                // 題目初始數字：立體浮雕燙金字（底層深金立體陰影 + 表層金黃高光）
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = cellVal,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFF5D4037) else Color(0xFF795548).copy(alpha = 0.45f)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(x = 0.6.dp, y = 0.8.dp)
                    )
                    Text(
                        text = cellVal,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFFFFD54F) else Color(0xFFB8860B)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 玩家填入數字：皇家藍寶石質感
                Text(
                    text = cellVal,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF40C4FF) else Color(0xFF0D47A1)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        } else if (cellNotes.isNotEmpty()) {
            // 筆記候選數：小巧雅致微消光石墨灰字型
            SudokuNotesCell(
                gridSize = gridSize,
                symbols = symbols,
                notes = cellNotes
            )
        }
    }
}

@Composable
fun SudokuNotesCell(
    gridSize: Int,
    symbols: List<String>,
    notes: Set<String>
) {
    val noteRows = when (gridSize) {
        4 -> 2
        6 -> 2
        12 -> 3
        else -> 3 // 9x9
    }
    val noteCols = when (gridSize) {
        4 -> 2
        6 -> 3
        12 -> 4
        else -> 3
    }

    val noteFontSize = when (gridSize) {
        4 -> 10.sp
        6 -> 8.sp
        9 -> 7.sp
        else -> 6.sp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (nr in 0 until noteRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (nc in 0 until noteCols) {
                    val symbolIdx = nr * noteCols + nc
                    val symbol = symbols.getOrNull(symbolIdx) ?: ""
                    Text(
                        text = if (notes.contains(symbol)) symbol else "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = noteFontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 晶透 3D 圓形水晶數字鍵盤
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SudokuKeypad(
    symbols: List<String>,
    onSymbolInput: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        symbols.forEach { symbol ->
            Surface(
                onClick = { onSymbolInput(symbol) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .size(52.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFFD54F), Color(0xFFB8860B), Color(0xFF8D6E63))
                        ),
                        shape = CircleShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF5D4037)
                        )
                    )
                }
            }
        }
    }
}

private fun formatTimeMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
