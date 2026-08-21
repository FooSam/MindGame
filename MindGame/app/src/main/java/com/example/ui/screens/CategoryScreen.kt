package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import com.example.data.model.WheelDifficultyConfig
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.GameCategory
import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.data.model.Localization

data class LevelItem(
    val difficulty: GameDifficulty,
    val titleKey: String,
    val color: Color
)

@Composable
fun CategoryScreen(
    category: GameCategory,
    selectedGameType: GameType,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onGameTypeSelect: (GameType) -> Unit,
    onDifficultySelect: (GameDifficulty) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val levelItems = listOf(
        LevelItem(GameDifficulty.BEGINNER, "diff_name_beginner", Color(0xFF10B981)),      // Mint Emerald
        LevelItem(GameDifficulty.INTERMEDIATE, "diff_name_intermediate", Color(0xFF0EA5E9)),  // Sky Blue
        LevelItem(GameDifficulty.ADVANCED, "diff_name_advanced", Color(0xFF6366F1)),      // Indigo
        LevelItem(GameDifficulty.HARD, "diff_name_hard", Color(0xFFF59E0B)),          // Amber
        LevelItem(GameDifficulty.HELL, "diff_name_hell", Color(0xFFEF4444)),          // Red
        LevelItem(GameDifficulty.EPIC, "diff_name_epic", Color(0xFF8B5CF6))           // Purple
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.width(8.dp))
                val categoryTitleKey = when (category) {
                    GameCategory.TEST -> "category_test"
                    GameCategory.BRAIN -> "category_brain"
                    GameCategory.DEDUCTION -> "category_deduction"
                    GameCategory.CASUAL -> "category_casual"
                }
                Text(
                    text = Localization.getString(categoryTitleKey, language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
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

        Spacer(modifier = Modifier.height(12.dp))

        // Game Selection Section Header
        Text(
            text = Localization.getString("select_game_item", language),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Game Selector Cards (Category specific)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (category == GameCategory.BRAIN) {
                // Game 1: 數獨遊戲
                val isSudoku = selectedGameType == GameType.SUDOKU
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (isSudoku) 2.dp else 1.dp,
                            color = if (isSudoku) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onGameTypeSelect(GameType.SUDOKU) },
                    color = if (isSudoku) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = if (isSudoku) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.getString("game_sudoku", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSudoku) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSudoku) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // Game 2: 貓咪數獨
                val isCatSudoku = selectedGameType == GameType.CAT_SUDOKU
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (isCatSudoku) 2.dp else 1.dp,
                            color = if (isCatSudoku) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onGameTypeSelect(GameType.CAT_SUDOKU) },
                    color = if (isCatSudoku) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = if (isCatSudoku) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.getString("game_cat_sudoku", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCatSudoku) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCatSudoku) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            } else {
                // Game 1: 專注力測驗
                val isFocus = selectedGameType == GameType.FOCUS_TEST
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isFocus) 2.dp else 1.dp,
                            color = if (isFocus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onGameTypeSelect(GameType.FOCUS_TEST) },
                    color = if (isFocus) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isFocus) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Localization.getString("game_focus_test", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isFocus) FontWeight.Bold else FontWeight.Normal,
                                color = if (isFocus) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                    }
                }

                // Game 2: 專注力訓練 (動態舒爾特圓盤)
                val isTrain = selectedGameType == GameType.FOCUS_TRAIN
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isTrain) 2.dp else 1.dp,
                            color = if (isTrain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onGameTypeSelect(GameType.FOCUS_TRAIN) },
                    color = if (isTrain) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = if (isTrain) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Localization.getString("game_focus_train", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isTrain) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTrain) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                    }
                }

                // Game 3: 極速配對
                val isSpeed = selectedGameType == GameType.SPEED_MATCH
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isSpeed) 2.dp else 1.dp,
                            color = if (isSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onGameTypeSelect(GameType.SPEED_MATCH) },
                    color = if (isSpeed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isSpeed) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Localization.getString("game_speed_match", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSpeed) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSpeed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Game Banner Description
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (selectedGameType) {
                                GameType.SPEED_MATCH -> Icons.Default.Bolt
                                GameType.SUDOKU -> Icons.Default.GridOn
                                GameType.CAT_SUDOKU -> Icons.Default.Pets
                                GameType.FOCUS_TRAIN -> Icons.Default.TrackChanges
                                else -> Icons.Default.Timer
                            },
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Localization.getString(selectedGameType.titleKey, language),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Localization.getString(selectedGameType.descKey, language),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Level Header
        Text(
            text = Localization.getString("select_difficulty", language),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 6 Difficulty Level Options Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(levelItems) { item ->
                val cellDesc = when (selectedGameType) {
                    GameType.SPEED_MATCH -> "${item.difficulty.speedMatchCells} 格 (${item.difficulty.speedMatchCols}x${item.difficulty.speedMatchCells / item.difficulty.speedMatchCols})"
                    GameType.FOCUS_TRAIN -> when (item.difficulty) {
                        GameDifficulty.BEGINNER -> "7個 (1~7, 1圈)"
                        GameDifficulty.INTERMEDIATE -> "13個 (1~13, 1圈)"
                        GameDifficulty.ADVANCED -> "19個 (1~19, 2圈)"
                        GameDifficulty.HARD -> "37個 (1~37, 2圈)"
                        GameDifficulty.HELL -> "43個 (1~43, 3圈)"
                        GameDifficulty.EPIC -> "69個 (1~69, 3圈)"
                    }
                    GameType.SUDOKU -> when (item.difficulty) {
                        GameDifficulty.BEGINNER -> "16格 (數字 1~4)"
                        GameDifficulty.INTERMEDIATE -> "36格 (數字 1~6)"
                        GameDifficulty.ADVANCED -> "81格 (提示36~45個)"
                        GameDifficulty.HARD -> "81格 (提示30~35個)"
                        GameDifficulty.HELL -> "81格 (提示22~29個)"
                        GameDifficulty.EPIC -> "144格 (1~9+A,B,C)"
                    }
                    GameType.CAT_SUDOKU -> when (item.difficulty) {
                        GameDifficulty.BEGINNER -> "4x4 (放4隻貓)"
                        GameDifficulty.INTERMEDIATE -> "5x5 (放5隻貓)"
                        GameDifficulty.ADVANCED -> "6x6 (放6隻貓)"
                        GameDifficulty.HARD -> "7x7 (放7隻貓)"
                        GameDifficulty.HELL -> "8x8 (放8隻貓)"
                        GameDifficulty.EPIC -> "9x9 (放9隻貓)"
                    }
                    else -> "${item.difficulty.totalCells} 格 (1~${item.difficulty.totalCells})"
                }

                val gridLabel = when (selectedGameType) {
                    GameType.SPEED_MATCH -> "${item.difficulty.speedMatchCells}格"
                    GameType.FOCUS_TRAIN -> when (item.difficulty) {
                        GameDifficulty.BEGINNER, GameDifficulty.INTERMEDIATE -> "1圈"
                        GameDifficulty.ADVANCED, GameDifficulty.HARD -> "2圈"
                        GameDifficulty.HELL, GameDifficulty.EPIC -> "3圈"
                    }
                    GameType.SUDOKU -> when (item.difficulty) {
                        GameDifficulty.BEGINNER -> "4x4"
                        GameDifficulty.INTERMEDIATE -> "6x6"
                        GameDifficulty.ADVANCED, GameDifficulty.HARD, GameDifficulty.HELL -> "9x9"
                        GameDifficulty.EPIC -> "12x12"
                    }
                    GameType.CAT_SUDOKU -> when (item.difficulty) {
                        GameDifficulty.BEGINNER -> "4x4"
                        GameDifficulty.INTERMEDIATE -> "5x5"
                        GameDifficulty.ADVANCED -> "6x6"
                        GameDifficulty.HARD -> "7x7"
                        GameDifficulty.HELL -> "8x8"
                        GameDifficulty.EPIC -> "9x9"
                    }
                    else -> "${item.difficulty.gridDim}x${item.difficulty.gridDim}"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDifficultySelect(item.difficulty) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    item.color.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = gridLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = item.color
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = Localization.getString(item.titleKey, language),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cellDesc,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = item.color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
