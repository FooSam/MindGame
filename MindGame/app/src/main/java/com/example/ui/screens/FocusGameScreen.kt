package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.GameDifficulty
import com.example.data.model.Localization
import com.example.ui.components.formatTimeMillis
import com.example.ui.viewmodel.GameStatus

@Composable
fun FocusGameScreen(
    difficulty: GameDifficulty,
    gameStatus: GameStatus,
    gridNumbers: List<Int>,
    currentTarget: Int,
    clearedIndices: Set<Int>,
    elapsedTimeMillis: Long,
    wrongTapIndex: Int?,
    lastCompletedTimeMillis: Long?,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                val diffTitleKey = when (difficulty) {
                    GameDifficulty.BEGINNER -> "diff_beginner"
                    GameDifficulty.INTERMEDIATE -> "diff_intermediate"
                    GameDifficulty.ADVANCED -> "diff_advanced"
                    GameDifficulty.HARD -> "diff_hard"
                    GameDifficulty.HELL -> "diff_hell"
                    GameDifficulty.EPIC -> "diff_epic"
                }
                Column {
                    Text(
                        text = Localization.getString("game_focus_test", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = Localization.getString(diffTitleKey, language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Top-Right Leaderboard Button
            IconButton(onClick = onLeaderboardClick) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = "Leaderboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timer & Next Target Dashboard Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High Precision Timer Box
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = Localization.getString("timer_label", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = formatTimeMillis(elapsedTimeMillis),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Target Number Indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Localization.getString("target_label", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (gameStatus == GameStatus.PLAYING) "$currentTarget / ${difficulty.totalCells}" else "-",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid Area (Calculated to fit strictly inside screen without scrolling)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val maxW = maxWidth
            val maxH = maxHeight
            val sideLength = if (maxW < maxH) maxW else maxH

            val gridDim = difficulty.gridDim
            val spacing = when (gridDim) {
                3 -> 10.dp
                4 -> 8.dp
                5 -> 6.dp
                6 -> 5.dp
                7 -> 4.dp
                8 -> 3.dp
                else -> 4.dp
            }

            val fontSize = when (gridDim) {
                3 -> 28.sp
                4 -> 24.sp
                5 -> 20.sp
                6 -> 16.sp
                7 -> 14.sp
                8 -> 12.sp
                else -> 14.sp
            }

            Box(
                modifier = Modifier
                    .size(sideLength)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (gameStatus == GameStatus.IDLE) {
                    // Ready overlay
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = Localization.getString("game_ready_hint", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onStartClick,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.getString("start_game", language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    // Active Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridDim),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(gridNumbers) { index, number ->
                            val isCleared = clearedIndices.contains(index)
                            val isWrong = wrongTapIndex == index

                            CellView(
                                number = number,
                                isCleared = isCleared,
                                isWrong = isWrong,
                                fontSize = fontSize,
                                onClick = { onCellClick(index) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Action Bar: Reset / Restart Button & Leaderboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Leaderboard Button
            OutlinedButton(
                onClick = onLeaderboardClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.getString("leaderboard_button", language),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Right: Restart Button
            if (gameStatus != GameStatus.IDLE) {
                Button(
                    onClick = onResetClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString("restart_game", language),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    // Completion Dialog
    if (gameStatus == GameStatus.COMPLETED && lastCompletedTimeMillis != null) {
        GameCompletedDialog(
            timeMillis = lastCompletedTimeMillis,
            difficulty = difficulty,
            language = language,
            onPlayAgain = onResetClick,
            onLeaderboard = onLeaderboardClick,
            onBackToMenu = onBackClick
        )
    }
}

@Composable
fun CellView(
    number: Int,
    isCleared: Boolean,
    isWrong: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isWrong) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cellScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isCleared -> Color.Transparent
                    isWrong -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
            .border(
                width = if (isCleared) 0.dp else 1.dp,
                color = if (isWrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = !isCleared, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!isCleared) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isWrong) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun GameCompletedDialog(
    timeMillis: Long,
    difficulty: GameDifficulty,
    language: AppLanguage,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val ratingKey = when {
        timeMillis < difficulty.totalCells * 600L -> "focus_rating_fast"
        timeMillis < difficulty.totalCells * 1200L -> "focus_rating_great"
        else -> "focus_rating_good"
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = Localization.getString("game_completed", language),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Localization.getString(ratingKey, language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Localization.getString("your_time", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = formatTimeMillis(timeMillis),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Localization.getString("score_saved", language),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = onPlayAgain) {
                Text(text = Localization.getString("play_again", language))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onLeaderboard) {
                    Text(text = Localization.getString("leaderboard_button", language))
                }
                TextButton(onClick = onBackToMenu) {
                    Text(text = Localization.getString("back_to_menu", language))
                }
            }
        }
    )
}
