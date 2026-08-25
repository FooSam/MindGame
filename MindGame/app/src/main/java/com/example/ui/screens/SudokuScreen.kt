package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
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
    onBackClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onSymbolInput: (String) -> Unit,
    onEraseClick: () -> Unit,
    onUndoClick: () -> Unit,
    onTogglePencilClick: () -> Unit,
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    color = MaterialTheme.colorScheme.primaryContainer
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

        // Action Toolbar (Undo, Erase, Pencil Mode Toggle)
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
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
                    color = if (isPencilMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Pencil Mode",
                            tint = if (isPencilMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPencilMode) Localization.getString("pencil_mode_on", language) else Localization.getString("pencil_mode_off", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isPencilMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPencilMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        val size = maxWidth
        val cellSide = size / gridSize

        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until gridSize) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until gridSize) {
                        val index = r * gridSize + c
                        val isInitial = initialBoard.getOrNull(index)?.isNotEmpty() == true
                        val cellVal = playerBoard.getOrNull(index) ?: ""
                        val cellNotes = notesBoard[index] ?: emptySet()
                        val isSelected = selectedCellIndex == index

                        // Determine background highlight
                        val isSameRowOrCol = selectedCellIndex != null && (
                                (selectedCellIndex / gridSize == r) || (selectedCellIndex % gridSize == c)
                                )
                        val isSameValue = selectedCellIndex != null && cellVal.isNotEmpty() &&
                                playerBoard.getOrNull(selectedCellIndex) == cellVal

                        val cellBgColor = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isSameValue -> MaterialTheme.colorScheme.secondaryContainer
                            isSameRowOrCol -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            isInitial -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        // Border thickness for subgrid separation
                        val borderTop = if (r % subRows == 0 && r != 0) 2.dp else 0.5.dp
                        val borderLeft = if (c % subCols == 0 && c != 0) 2.dp else 0.5.dp

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(cellBgColor)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                .clickable { onCellClick(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (cellVal.isNotEmpty()) {
                                val fontSp = when (gridSize) {
                                    4 -> 24.sp
                                    6 -> 20.sp
                                    9 -> 16.sp
                                    12 -> 13.sp
                                    else -> 14.sp
                                }
                                Text(
                                    text = cellVal,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = fontSp,
                                        fontWeight = if (isInitial) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isInitial) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            } else if (cellNotes.isNotEmpty()) {
                                // Draw notes inside cell
                                SudokuNotesCell(
                                    gridSize = gridSize,
                                    symbols = symbols,
                                    notes = cellNotes
                                )
                            }
                        }
                    }
                }
            }
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SudokuKeypad(
    symbols: List<String>,
    onSymbolInput: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        symbols.forEach { symbol ->
            Surface(
                onClick = { onSymbolInput(symbol) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
