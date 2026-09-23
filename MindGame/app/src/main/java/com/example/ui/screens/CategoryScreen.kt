package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SoundManager
import com.example.data.model.AppLanguage
import com.example.data.model.GameCategory
import com.example.data.model.GameDifficulty
import com.example.data.model.GameType
import com.example.data.model.Localization
import com.example.ui.components.NewBadge

data class LevelItem(
    val difficulty: GameDifficulty,
    val titleKey: String,
    val color: Color
)

@Composable
fun CategoryScreen(
    category: GameCategory,
    expandedGameType: GameType?,
    language: AppLanguage,
    isGameNew: (GameType) -> Boolean = { false },
    onBackClick: () -> Unit,
    onGameTypeSelect: (GameType) -> Unit,
    onDifficultySelect: (GameType, GameDifficulty) -> Unit,
    onLeaderboardClick: () -> Unit
) {
    // 依類別設定所屬遊戲清單（最新熱門大作排在最前列）
    val gamesInCategory = when (category) {
        GameCategory.DEDUCTION -> listOf(GameType.TURTLE_SOUP)
        GameCategory.BRAIN -> listOf(
            GameType.GLASS_PUZZLE_CUBE,
            GameType.SUDOKU,
            GameType.CAT_SUDOKU
        )
        GameCategory.TEST -> listOf(
            GameType.STROOP_EFFECT,
            GameType.AVATAR_WHACK,
            GameType.SPEED_MATCH,
            GameType.FOCUS_TRAIN,
            GameType.FOCUS_TEST
        )
        GameCategory.CASUAL -> listOf(
            GameType.BLOCK_PUZZLE,
            GameType.FRUIT_MASTER
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 頂部導航列
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    SoundManager.playClick()
                    onBackClick()
                }) {
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

            IconButton(onClick = {
                SoundManager.playClick()
                onLeaderboardClick()
            }) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = "Leaderboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 單欄垂直手風琴式卡片列表 (淘汰水平向左滑動盲區，全遊戲一覽無遺)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = Localization.getString("select_game_item", language),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            items(gamesInCategory) { gameType ->
                val isSelected = expandedGameType == gameType
                val isNew = isGameNew(gameType)

                GameAccordionCard(
                    gameType = gameType,
                    isSelected = isSelected,
                    isNew = isNew,
                    language = language,
                    onCardClick = {
                        SoundManager.playClick()
                        onGameTypeSelect(gameType)
                    },
                    onDifficultySelect = { diff ->
                        SoundManager.playClick()
                        onDifficultySelect(gameType, diff)
                    }
                )
            }
        }
    }
}

/**
 * 單欄折疊手風琴卡片：
 * - 收合時：精簡橫條（高度約 76dp），顯示小圖示 + 遊戲名稱 + 簡述 + NEW 角標 + 展開指示
 * - 展開時：平滑延伸在原地下方呈現難度等級選擇按鈕與開始挑戰
 */
@Composable
fun GameAccordionCard(
    gameType: GameType,
    isSelected: Boolean,
    isNew: Boolean,
    language: AppLanguage,
    onCardClick: () -> Unit,
    onDifficultySelect: (GameDifficulty) -> Unit
) {
    val levelItems = getLevelItemsForGame(gameType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 卡片主要抬頭列 (點擊切換選取/展開)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左側圓角小圖示卡片
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameType == GameType.GLASS_PUZZLE_CUBE) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_isometric_cube),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Icon(
                            imageVector = getGameIcon(gameType),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 中間遊戲標題與簡短說明
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Localization.getString(gameType.titleKey, language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (isNew) {
                            Spacer(modifier = Modifier.width(8.dp))
                            NewBadge()
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = Localization.getString(gameType.descKey, language),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        ),
                        maxLines = if (isSelected) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 右側展開/收合箭頭指示
                Icon(
                    imageVector = if (isSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 展開區域：難度選擇面板
            if (isSelected) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = Localization.getString("select_difficulty", language),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (gameType == GameType.GLASS_PUZZLE_CUBE) {
                        // 六面合體專屬橫條式 3 列難度面板
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = 1.dp,
                                            color = item.color.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onDifficultySelect(item.difficulty) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(item.color.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                MiniPolyominoIcon(difficulty = item.difficulty, baseColor = item.color)
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = Localization.getString(item.titleKey, language),
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = item.color.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = badgeTag,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = item.color,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = descText,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(item.color.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
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
                    } else {
                        // 一般遊戲 2 欄式難度選擇網格 (自適應排版，無滑動衝突)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            levelItems.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        val gridLabel = getGridLabel(gameType, item.difficulty)
                                        val cellDesc = getCellDesc(gameType, item.difficulty)

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = item.color.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable { onDifficultySelect(item.difficulty) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(30.dp)
                                                            .background(
                                                                item.color.copy(alpha = 0.15f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = gridLabel,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = item.color,
                                                                fontSize = 11.sp
                                                            )
                                                        )
                                                    }

                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play",
                                                        tint = item.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = Localization.getString(item.titleKey, language),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(
                                                    text = cellDesc,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getGameIcon(gameType: GameType): ImageVector {
    return when (gameType) {
        GameType.SPEED_MATCH -> Icons.Default.Bolt
        GameType.SUDOKU -> Icons.Default.GridOn
        GameType.CAT_SUDOKU -> Icons.Default.Pets
        GameType.GLASS_PUZZLE_CUBE -> Icons.Default.Extension
        GameType.FOCUS_TRAIN -> Icons.Default.TrackChanges
        GameType.TURTLE_SOUP -> Icons.Default.Lightbulb
        GameType.AVATAR_WHACK -> Icons.Default.SportsEsports
        GameType.STROOP_EFFECT -> Icons.Default.Psychology
        else -> Icons.Default.Timer
    }
}

private fun getLevelItemsForGame(gameType: GameType): List<LevelItem> {
    return when (gameType) {
        GameType.TURTLE_SOUP -> listOf(
            LevelItem(GameDifficulty.BEGINNER, "turtle_soup_diff_easy", Color(0xFF10B981)),
            LevelItem(GameDifficulty.INTERMEDIATE, "turtle_soup_diff_medium", Color(0xFF0EA5E9)),
            LevelItem(GameDifficulty.HARD, "turtle_soup_diff_hard", Color(0xFFEF4444))
        )
        GameType.GLASS_PUZZLE_CUBE -> listOf(
            LevelItem(GameDifficulty.BEGINNER, "diff_name_beginner", Color(0xFF10B981)),
            LevelItem(GameDifficulty.INTERMEDIATE, "diff_name_intermediate", Color(0xFF0EA5E9)),
            LevelItem(GameDifficulty.ADVANCED, "diff_name_hard", Color(0xFFEF4444))
        )
        else -> listOf(
            LevelItem(GameDifficulty.BEGINNER, "diff_name_beginner", Color(0xFF10B981)),
            LevelItem(GameDifficulty.INTERMEDIATE, "diff_name_intermediate", Color(0xFF0EA5E9)),
            LevelItem(GameDifficulty.ADVANCED, "diff_name_advanced", Color(0xFF6366F1)),
            LevelItem(GameDifficulty.HARD, "diff_name_hard", Color(0xFFF59E0B)),
            LevelItem(GameDifficulty.HELL, "diff_name_hell", Color(0xFFEF4444)),
            LevelItem(GameDifficulty.EPIC, "diff_name_epic", Color(0xFF8B5CF6))
        )
    }
}

private fun getGridLabel(gameType: GameType, difficulty: GameDifficulty): String {
    return when (gameType) {
        GameType.SPEED_MATCH -> "${difficulty.speedMatchCells}格"
        GameType.FOCUS_TRAIN -> when (difficulty) {
            GameDifficulty.BEGINNER, GameDifficulty.INTERMEDIATE -> "1圈"
            GameDifficulty.ADVANCED, GameDifficulty.HARD -> "2圈"
            GameDifficulty.HELL, GameDifficulty.EPIC -> "3圈"
        }
        GameType.SUDOKU -> when (difficulty) {
            GameDifficulty.BEGINNER -> "4x4"
            GameDifficulty.INTERMEDIATE -> "6x6"
            GameDifficulty.ADVANCED, GameDifficulty.HARD, GameDifficulty.HELL -> "9x9"
            GameDifficulty.EPIC -> "12x12"
        }
        GameType.CAT_SUDOKU -> when (difficulty) {
            GameDifficulty.BEGINNER -> "4x4"
            GameDifficulty.INTERMEDIATE -> "5x5"
            GameDifficulty.ADVANCED -> "6x6"
            GameDifficulty.HARD -> "7x7"
            GameDifficulty.HELL -> "8x8"
            GameDifficulty.EPIC -> "9x9"
        }
        GameType.TURTLE_SOUP -> when (difficulty) {
            GameDifficulty.BEGINNER -> "Easy"
            GameDifficulty.INTERMEDIATE -> "Medium"
            else -> "Hard"
        }
        GameType.AVATAR_WHACK -> when (difficulty) {
            GameDifficulty.BEGINNER -> "2x2"
            GameDifficulty.INTERMEDIATE -> "2x3"
            GameDifficulty.ADVANCED, GameDifficulty.HARD -> "3x3"
            GameDifficulty.HELL, GameDifficulty.EPIC -> "3x4"
        }
        GameType.STROOP_EFFECT -> when (difficulty) {
            GameDifficulty.BEGINNER -> "4色"
            GameDifficulty.INTERMEDIATE -> "雙向"
            GameDifficulty.ADVANCED -> "比對"
            GameDifficulty.HARD -> "干擾"
            GameDifficulty.HELL -> "動態"
            GameDifficulty.EPIC -> "複合"
        }
        else -> "${difficulty.gridDim}x${difficulty.gridDim}"
    }
}

private fun getCellDesc(gameType: GameType, difficulty: GameDifficulty): String {
    return when (gameType) {
        GameType.SPEED_MATCH -> "${difficulty.speedMatchCells} 格 (${difficulty.speedMatchCols}x${difficulty.speedMatchCells / difficulty.speedMatchCols})"
        GameType.FOCUS_TRAIN -> when (difficulty) {
            GameDifficulty.BEGINNER -> "7個 (1~7, 1圈)"
            GameDifficulty.INTERMEDIATE -> "13個 (1~13, 1圈)"
            GameDifficulty.ADVANCED -> "19個 (1~19, 2圈)"
            GameDifficulty.HARD -> "37個 (1~37, 2圈)"
            GameDifficulty.HELL -> "43個 (1~43, 3圈)"
            GameDifficulty.EPIC -> "69個 (1~69, 3圈)"
        }
        GameType.SUDOKU -> when (difficulty) {
            GameDifficulty.BEGINNER -> "16格 (數字 1~4)"
            GameDifficulty.INTERMEDIATE -> "36格 (數字 1~6)"
            GameDifficulty.ADVANCED -> "81格 (提示36~45個)"
            GameDifficulty.HARD -> "81格 (提示30~35個)"
            GameDifficulty.HELL -> "81格 (提示22~29個)"
            GameDifficulty.EPIC -> "144格 (1~9+A,B,C)"
        }
        GameType.CAT_SUDOKU -> when (difficulty) {
            GameDifficulty.BEGINNER -> "4x4 (放4隻貓)"
            GameDifficulty.INTERMEDIATE -> "5x5 (放5隻貓)"
            GameDifficulty.ADVANCED -> "6x6 (放6隻貓)"
            GameDifficulty.HARD -> "7x7 (放7隻貓)"
            GameDifficulty.HELL -> "8x8 (放8隻貓)"
            GameDifficulty.EPIC -> "9x9 (放9隻貓)"
        }
        GameType.TURTLE_SOUP -> when (difficulty) {
            GameDifficulty.BEGINNER -> "簡單 (初階案件)"
            GameDifficulty.INTERMEDIATE -> "普通 (進階案件)"
            else -> "困難 (深度案件)"
        }
        GameType.AVATAR_WHACK -> when (difficulty) {
            GameDifficulty.BEGINNER -> "2x2(4洞) / 1.5s"
            GameDifficulty.INTERMEDIATE -> "2x3(6洞) / 1.2s"
            GameDifficulty.ADVANCED -> "3x3(9洞) / 0.9s"
            GameDifficulty.HARD -> "3x3(9洞) / 0.7s"
            GameDifficulty.HELL -> "3x4(12洞) / 0.5s"
            GameDifficulty.EPIC -> "3x4(12洞) / 0.35s"
        }
        GameType.STROOP_EFFECT -> when (difficulty) {
            GameDifficulty.BEGINNER -> "經典字色 (4基本色)"
            GameDifficulty.INTERMEDIATE -> "雙向指令 (字義/顏色)"
            GameDifficulty.ADVANCED -> "雙字比對 (是/否)"
            GameDifficulty.HARD -> "干擾按鈕 (色彩衝突)"
            GameDifficulty.HELL -> "動態閃爍 (1.5s 旋轉)"
            GameDifficulty.EPIC -> "複合邏輯 (否定句 1.2s)"
        }
        else -> "${difficulty.totalCells} 格 (1~${difficulty.totalCells})"
    }
}

/**
 * 繪製難度示意的小型等角立體積木拼塊
 */
@Composable
fun MiniPolyominoIcon(difficulty: GameDifficulty, baseColor: Color) {
    Canvas(modifier = Modifier.size(36.dp)) {
        val cellSize = 8.5f
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
