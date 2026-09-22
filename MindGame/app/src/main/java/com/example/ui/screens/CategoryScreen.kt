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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import com.example.R
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
    val levelItems = if (selectedGameType == GameType.TURTLE_SOUP) {
        listOf(
            LevelItem(GameDifficulty.BEGINNER, "turtle_soup_diff_easy", Color(0xFF10B981)),      // 簡單 Easy
            LevelItem(GameDifficulty.INTERMEDIATE, "turtle_soup_diff_medium", Color(0xFF0EA5E9)),  // 普通 Medium
            LevelItem(GameDifficulty.HARD, "turtle_soup_diff_hard", Color(0xFFEF4444))          // 困難 Hard
        )
    } else if (selectedGameType == GameType.GLASS_PUZZLE_CUBE) {
        listOf(
            LevelItem(GameDifficulty.BEGINNER, "diff_name_beginner", Color(0xFF10B981)),      // 初級
            LevelItem(GameDifficulty.INTERMEDIATE, "diff_name_intermediate", Color(0xFF0EA5E9)),  // 中級
            LevelItem(GameDifficulty.ADVANCED, "diff_name_hard", Color(0xFFEF4444))          // 最高難度
        )
    } else {
        listOf(
            LevelItem(GameDifficulty.BEGINNER, "diff_name_beginner", Color(0xFF10B981)),      // Mint Emerald
            LevelItem(GameDifficulty.INTERMEDIATE, "diff_name_intermediate", Color(0xFF0EA5E9)),  // Sky Blue
            LevelItem(GameDifficulty.ADVANCED, "diff_name_advanced", Color(0xFF6366F1)),      // Indigo
            LevelItem(GameDifficulty.HARD, "diff_name_hard", Color(0xFFF59E0B)),          // Amber
            LevelItem(GameDifficulty.HELL, "diff_name_hell", Color(0xFFEF4444)),          // Red
            LevelItem(GameDifficulty.EPIC, "diff_name_epic", Color(0xFF8B5CF6))           // Purple
        )
    }

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
            if (category == GameCategory.DEDUCTION) {
                // Deduction Game: 海龜湯推理問答
                val isTurtleSoup = selectedGameType == GameType.TURTLE_SOUP
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (isTurtleSoup) 2.dp else 1.dp,
                            color = if (isTurtleSoup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onGameTypeSelect(GameType.TURTLE_SOUP) },
                    color = if (isTurtleSoup) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (isTurtleSoup) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.getString("game_turtle_soup", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isTurtleSoup) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTurtleSoup) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            } else if (category == GameCategory.BRAIN) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val brainGames = listOf(
                        Triple(GameType.SUDOKU, "game_sudoku", Icons.Default.GridOn),
                        Triple(GameType.CAT_SUDOKU, "game_cat_sudoku", Icons.Default.Pets),
                        Triple(GameType.GLASS_PUZZLE_CUBE, "game_glass_puzzle_cube", Icons.Default.Extension)
                    )
                    brainGames.forEach { (type, nameKey, icon) ->
                        val isSelected = selectedGameType == type
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onGameTypeSelect(type) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (type == GameType.GLASS_PUZZLE_CUBE) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_isometric_cube),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.getString(nameKey, language),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // GameCategory.TEST: 5 款測驗遊戲 (支援水平滾動平滑選取)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val testGames = listOf(
                        Triple(GameType.FOCUS_TEST, "game_focus_test", Icons.Default.Timer),
                        Triple(GameType.FOCUS_TRAIN, "game_focus_train", Icons.Default.TrackChanges),
                        Triple(GameType.SPEED_MATCH, "game_speed_match", Icons.Default.Bolt),
                        Triple(GameType.AVATAR_WHACK, "game_avatar_whack", Icons.Default.SportsEsports),
                        Triple(GameType.STROOP_EFFECT, "game_stroop_effect", Icons.Default.Psychology)
                    )

                    testGames.forEach { (type, nameKey, icon) ->
                        val isSelected = selectedGameType == type
                        Surface(
                            modifier = Modifier
                                .width(98.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onGameTypeSelect(type) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Localization.getString(nameKey, language),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
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
                        if (selectedGameType == GameType.GLASS_PUZZLE_CUBE) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_isometric_cube),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = when (selectedGameType) {
                                    GameType.SPEED_MATCH -> Icons.Default.Bolt
                                    GameType.SUDOKU -> Icons.Default.GridOn
                                    GameType.CAT_SUDOKU -> Icons.Default.Pets
                                    GameType.GLASS_PUZZLE_CUBE -> Icons.Default.Extension
                                    GameType.FOCUS_TRAIN -> Icons.Default.TrackChanges
                                    GameType.TURTLE_SOUP -> Icons.Default.Lightbulb
                                    GameType.AVATAR_WHACK -> Icons.Default.SportsEsports
                                    GameType.STROOP_EFFECT -> Icons.Default.Psychology
                                    else -> Icons.Default.Timer
                                },
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
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

        if (selectedGameType == GameType.GLASS_PUZZLE_CUBE) {
            // 六面合體專屬橫條式 3 列難度清單
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                levelItems.forEach { item ->
                    val descText = when (item.difficulty) {
                        GameDifficulty.BEGINNER -> Localization.getString("glass_cube_diff_desc_beginner", language)
                        GameDifficulty.INTERMEDIATE -> Localization.getString("glass_cube_diff_desc_intermediate", language)
                        else -> Localization.getString("glass_cube_diff_desc_hard", language)
                    }
                    val badgeTag = when (item.difficulty) {
                        GameDifficulty.BEGINNER -> "2~3格"
                        GameDifficulty.INTERMEDIATE -> "3~4格"
                        else -> "異形"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDifficultySelect(item.difficulty) }
                            .border(
                                width = 1.2.dp,
                                color = item.color.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左側立體積木拼塊 ICON
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(item.color.copy(alpha = 0.12f))
                                        .border(
                                            width = 1.dp,
                                            color = item.color.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MiniPolyominoIcon(difficulty = item.difficulty, baseColor = item.color)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = Localization.getString(item.titleKey, language),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = item.color.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = badgeTag,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = item.color,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = descText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(item.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = item.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
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
                        GameType.TURTLE_SOUP -> when (item.difficulty) {
                            GameDifficulty.BEGINNER -> "簡單 (初階案件 / 4次提問 / 2條核心)"
                            GameDifficulty.INTERMEDIATE -> "普通 (進階案件 / 3次提問 / 2~3條核心)"
                            else -> "困難 (深度案件 / 3次提問 / 3~4條核心)"
                        }
                        GameType.AVATAR_WHACK -> when (item.difficulty) {
                            GameDifficulty.BEGINNER -> "2x2(4洞) / 1.5s 暖身"
                            GameDifficulty.INTERMEDIATE -> "2x3(6洞) / 1.2s 假動作"
                            GameDifficulty.ADVANCED -> "3x3(9洞) / 0.9s 炸彈"
                            GameDifficulty.HARD -> "3x3(9洞) / 0.7s 雙目標"
                            GameDifficulty.HELL -> "3x4(12洞) / 0.5s 連擊"
                            GameDifficulty.EPIC -> "3x4(12洞) / 0.35s 狂暴"
                        }
                        GameType.STROOP_EFFECT -> when (item.difficulty) {
                            GameDifficulty.BEGINNER -> "經典字色辨識 (4基本色)"
                            GameDifficulty.INTERMEDIATE -> "雙向指令 (字義/顏色)"
                            GameDifficulty.ADVANCED -> "左右雙字比對 (是/否)"
                            GameDifficulty.HARD -> "干擾按鈕模式 (色彩衝突)"
                            GameDifficulty.HELL -> "動態計時閃爍 (1.5s 旋轉)"
                            GameDifficulty.EPIC -> "複合多重認知 (否定句 1.2s)"
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
                        GameType.TURTLE_SOUP -> when (item.difficulty) {
                            GameDifficulty.BEGINNER -> "Easy"
                            GameDifficulty.INTERMEDIATE -> "Medium"
                            else -> "Hard"
                        }
                        GameType.AVATAR_WHACK -> when (item.difficulty) {
                            GameDifficulty.BEGINNER -> "2x2"
                            GameDifficulty.INTERMEDIATE -> "2x3"
                            GameDifficulty.ADVANCED, GameDifficulty.HARD -> "3x3"
                            GameDifficulty.HELL, GameDifficulty.EPIC -> "3x4"
                        }
                        GameType.STROOP_EFFECT -> when (item.difficulty) {
                            GameDifficulty.BEGINNER -> "4色"
                            GameDifficulty.INTERMEDIATE -> "雙向"
                            GameDifficulty.ADVANCED -> "比對"
                            GameDifficulty.HARD -> "干擾"
                            GameDifficulty.HELL -> "動態"
                            GameDifficulty.EPIC -> "複合"
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
}

/**
 * 繪製難度示意的小型等角立體積木拼塊
 */
@Composable
fun MiniPolyominoIcon(difficulty: GameDifficulty, baseColor: Color) {
    Canvas(modifier = Modifier.size(38.dp)) {
        val cellSize = 9f
        val h = cellSize * 0.50f
        val depth = cellSize * 0.70f
        val cells = when (difficulty) {
            GameDifficulty.BEGINNER -> listOf(
                Pair(0, 0), Pair(1, 0), Pair(1, 1) // 3格 L型
            )
            GameDifficulty.INTERMEDIATE -> listOf(
                Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, 2) // 4格 T型
            )
            else -> listOf(
                Pair(0, 1), Pair(0, 2), Pair(1, 0), Pair(1, 1), Pair(2, 1) // 5格 異形
            )
        }
        val minR = cells.minOf { it.first }
        val maxR = cells.maxOf { it.first }
        val minC = cells.minOf { it.second }
        val maxC = cells.maxOf { it.second }

        val pieceWidth = (maxC - minC + 1) * cellSize
        val pieceHeight = (maxR - minR + 1) * h + depth
        val originX = size.width / 2f - pieceWidth / 2f
        val originY = size.height / 2f - pieceHeight / 2f

        for (c in cells.sortedWith(compareBy({ it.first }, { it.second }))) {
            val cx = originX + (c.second - minC) * cellSize + cellSize / 2f
            val cy = originY + (c.first - minR) * h + h / 2f

            val hw = cellSize * 0.46f
            val hh = h * 0.46f

            val topP0 = Offset(cx, cy - hh)
            val topP1 = Offset(cx + hw, cy)
            val topP2 = Offset(cx, cy + hh)
            val topP3 = Offset(cx - hw, cy)

            val topPath = Path().apply {
                moveTo(topP0.x, topP0.y); lineTo(topP1.x, topP1.y); lineTo(topP2.x, topP2.y); lineTo(topP3.x, topP3.y); close()
            }
            val flPath = Path().apply {
                moveTo(topP3.x, topP3.y); lineTo(topP2.x, topP2.y); lineTo(topP2.x, topP2.y + depth); lineTo(topP3.x, topP3.y + depth); close()
            }
            val frPath = Path().apply {
                moveTo(topP2.x, topP2.y); lineTo(topP1.x, topP1.y); lineTo(topP1.x, topP1.y + depth); lineTo(topP2.x, topP2.y + depth); close()
            }

            drawPath(flPath, color = baseColor.copy(alpha = 0.90f), style = Fill)
            drawPath(flPath, color = Color.Black.copy(alpha = 0.15f), style = Fill)
            drawPath(frPath, color = baseColor.copy(alpha = 0.90f), style = Fill)
            drawPath(frPath, color = Color.Black.copy(alpha = 0.35f), style = Fill)
            drawPath(topPath, color = baseColor, style = Fill)
            drawPath(topPath, color = Color.White.copy(alpha = 0.35f), style = Fill)
            drawPath(topPath, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 0.8f))
        }
    }
}
